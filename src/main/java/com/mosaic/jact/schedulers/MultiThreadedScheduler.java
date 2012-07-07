package com.mosaic.jact.schedulers;

import com.mosaic.jact.AsyncContext;
import com.mosaic.jact.AsyncJob;
import com.mosaic.jact.AsyncSystem;
import com.mosaic.jact.jobqueues.JobQueue;
import com.mosaic.jact.jobqueues.LinkedListJobQueue;
import com.mosaic.jact.jobqueues.NotifyAllJobQueueWrapper;
import com.mosaic.jact.jobqueues.StripedJobQueueFactory;
import com.mosaic.jact.jobqueues.SynchronizedJobQueueWrapper;
import com.mosaic.lang.Future;
import com.mosaic.lang.Validate;
import com.mosaic.lang.conc.Monitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Optimised to avoid lock contention. Scales out across many many CPU cores.<p/>
 *
 * This scheduler consists of two tiers of mailboxes. A mailbox stores work until a thread becomes available to action the work.
 * Each thread is assigned two mailboxes, a public mailbox which is thread safe and a private mailbox which is not thread safe.<p/>
 *
 * When scheduling work via this scheduler, a threads public mailbox will be selected without taking on any locks via the hashcode
 * of the work being scheduled. This will spread the work across the threads and avoid any single point of contention, improving
 * scalability of the scheduler. The work will then remain in the public mailbox until a thread is ready to pull the work in.<p/>
 *
 * When a thread is started it loops over any work in its local mailbox. The local mailbox is extremely fast, and involves
 * no locks or thread synchronisation of any kind. It doesn't even use CAS or volatile statements. When the private mailbox
 * is empty, as it will be when the thread first starts then the thread will try to pull work in from its public mailbox.
 * Which does involve synchronisation. If the public mailbox is also empty, then the thread will try to steal work from
 * other public mailboxes. If that also fails then the thread will go to sleep on its public mailboxes monitor,
 * waking only when new work is pushed onto its public mailbox. <p/>
 *
 * This approach is biased towards throughput, giving excellent latency under load. The weaknesses of this approach is
 * that latency can suffer when the private mailboxes become saturated or work is not spread evenly over the public
 * mailboxes creating hot spots.<p/>
 *
 * Once a thread picks up a piece of work, that work has the option of scheduling more work. When it does this it
 * is given the choice between making the work available to any of the CPU cores (at the cost of going through
 * synchronisation blocks) or to schedule it locally to the thread that is running the current job. In this case there
 * is no synchronisation of any kind.<p/>
 */
public class MultiThreadedScheduler implements AsyncScheduler, AsyncSystem {

    private final Monitor            LOCK    = new Monitor();
    private final List<Monitor> threadMonitors = new ArrayList<Monitor>();

    private final String schedulerName;
    private final int numNonBlockingThreads;
    private int numBlockingThreads;

    private volatile boolean   isRunning       = false;
    private volatile JobQueue stripedMailbox  = null;
    private volatile JobQueue[] publicMailboxes = null;

    private final JobQueue blockingMailbox = new SynchronizedJobQueueWrapper( new LinkedListJobQueue() );

    public MultiThreadedScheduler( String name ) {
        this( name, Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors()*4 );
    }

    /**
     *
     * @param name
     * @param numNonBlockingThreads number of worker threads to use; public mailbox scheduling will be fastest if a value 2^n is used
     * @param numBlockingThreads    number of worker threads reserved for jobs that block
     */
    public MultiThreadedScheduler( String name, int numNonBlockingThreads, int numBlockingThreads ) {
        Validate.isGTEZero( numNonBlockingThreads, "numNonBlockingThreads" );
        Validate.isGTEZero( numBlockingThreads,    "numBlockingThreads"    );

        this.schedulerName         = name;
        this.numNonBlockingThreads = numNonBlockingThreads;
        this.numBlockingThreads    = numBlockingThreads;
    }

    public <T> Future<T> schedule( AsyncJob<T> job ) {
        try {
            stripedMailbox.push( job );
        } catch ( NullPointerException e ) {
            throw new IllegalStateException( String.format("%s scheduler is not running",schedulerName) );
        }

        return null;
    }

    public void start() {
        synchronized ( LOCK ) {
            if ( isRunning ) {
                return;
            }

            isRunning = true;
            String threadNamePrefix = schedulerName + "_";
            AsyncScheduler blockingScheduler = new JobQueueScheduler( blockingMailbox );


            publicMailboxes = new JobQueue[numNonBlockingThreads];
            for ( int i=0; i< numNonBlockingThreads; i++ ) {
                Monitor lock          = new Monitor();
                JobQueue publicMailbox = new SynchronizedJobQueueWrapper(new NotifyAllJobQueueWrapper(new LinkedListJobQueue(),lock),lock);
                publicMailboxes[i] = publicMailbox;

                String       threadName  = threadNamePrefix+(i+1);
                NoneBlockingWorkerThread workerThread = new NoneBlockingWorkerThread( threadName, publicMailbox, lock, blockingScheduler );
                workerThread.start();

                threadMonitors.add( lock );
            }

            stripedMailbox = StripedJobQueueFactory.stripeJobQueues( publicMailboxes );

            Monitor        nonBlockingMailboxLock = new Monitor();
            JobQueue nonBlockingMailbox     = new SynchronizedJobQueueWrapper( new LinkedListJobQueue(), nonBlockingMailboxLock );
            AsyncScheduler nonBlockingScheduler   = new JobQueueScheduler( nonBlockingMailbox );

            for ( int i=0; i<numBlockingThreads; i++ ) {
                String       threadName  = threadNamePrefix+(i+1+numNonBlockingThreads);

                BlockableWorkerThread workerThread = new BlockableWorkerThread( threadName, nonBlockingMailbox, nonBlockingMailboxLock, nonBlockingScheduler );
                workerThread.start();
            }

            threadMonitors.add( nonBlockingMailboxLock );
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void stop() {
        synchronized ( LOCK ) {
            stripedMailbox = null;
            isRunning      = false;

            for ( Monitor lock : threadMonitors ) {
                lock.wakeAll();
            }

            threadMonitors.clear();
        }
    }


    private class NoneBlockingWorkerThread extends Thread {
        private final JobQueue publicMailbox;
        private final Monitor      publicMailboxLock;
        private final JobQueue privateMailbox     = new LinkedListJobQueue();
        private final AsyncContext asyncContext;

        public NoneBlockingWorkerThread( String threadName, JobQueue publicMailbox, Monitor publicMailboxLock, AsyncScheduler blockableScheduler ) {
            super(threadName + "-NonBlocking");

            this.publicMailbox     = publicMailbox;
            this.publicMailboxLock = publicMailboxLock;
            this.asyncContext      = new AsyncContext( new JobQueueScheduler(publicMailbox), new JobQueueScheduler(privateMailbox), blockableScheduler );
        }


        @Override
        public void run() {
            while ( isRunning ) {
                invokeAllJobsFromPrivateMailbox();

                invokeOneBatchOfJobsFromPublicMailboxBlocking();
            }
        }

        private void invokeAllJobsFromPrivateMailbox() {
            boolean jobsRan = invokeJobs( privateMailbox );

            while ( jobsRan ) {
                jobsRan = invokeJobs( privateMailbox );
            }
        }

        private void invokeOneBatchOfJobsFromPublicMailboxBlocking() {
            boolean jobsRan = invokeJobs( publicMailbox );

            while ( !jobsRan && isRunning ) {
                synchronized ( publicMailboxLock ) {
                    if ( publicMailbox.isEmpty() ) {     // todo work steal
                        publicMailboxLock.sleep();
                    }
                }

                jobsRan = invokeJobs( publicMailbox );
            }
        }

        private boolean invokeJobs( JobQueue mailbox ) {
            AsyncJob j = mailbox.pop();
            if ( j != null) {
                try {
                    j.invoke( asyncContext );
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return true;
            } else {
                return false;
            }

//            return !jobs.isEmpty();
        }
    }


    private class BlockableWorkerThread extends Thread {
        private final JobQueue mailbox;
        private final Monitor mailboxMonitor;

        private final AsyncContext asyncContext;

        public BlockableWorkerThread( String threadName, JobQueue mailbox, Monitor mailboxMonitor, AsyncScheduler nonBlockingScheduler ) {
            super(threadName + "-Blockable");

            this.mailbox        = mailbox;
            this.mailboxMonitor = mailboxMonitor;
            this.asyncContext   = new AsyncContext( nonBlockingScheduler, nonBlockingScheduler, new JobQueueScheduler(mailbox) );
        }


        @Override
        public void run() {
            while ( isRunning ) {
//                AsyncJob job = mailbox.pop();
//
//                if ( job == null ) {
//                    synchronized ( mailboxMonitor ) { // todo is sleeping while holding this lock dangerous?
//                        job = mailbox.pop();
//
//                        if ( job == null ) {
//                            mailboxMonitor.sleep();
//                        }
//                    }
//                }
//
//
//                invokeJob(job);
            }
        }
    }
}

