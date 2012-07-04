package com.mosaic.jact.schedulers;

import com.mosaic.jact.AsyncJob;
import com.mosaic.jact.AsyncSystem;
import com.mosaic.jact.mailboxes.LinkedListMailbox;
import com.mosaic.jact.mailboxes.Mailbox;
import com.mosaic.jact.mailboxes.NotifyAllMailboxWrapper;
import com.mosaic.jact.mailboxes.StripedMailbox;
import com.mosaic.jact.mailboxes.SynchronizedMailboxWrapper;
import com.mosaic.lang.EnhancedIterable;
import com.mosaic.lang.Future;
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
    private final List<WorkerThread> threads = new ArrayList<WorkerThread>();

    private final String schedulerName;
    private final int    numThreads;

    private volatile boolean   isRunning       = false;
    private volatile Mailbox   stripedMailbox  = null;
    private volatile Mailbox[] publicMailboxes = null;


    public MultiThreadedScheduler( String name ) {
        this( name, Runtime.getRuntime().availableProcessors() );
    }

    /**
     *
     * @param name
     * @param numThreads number of worker threads to use; public mailbox scheduling will be fastest if a value 2^n is used
     */
    public MultiThreadedScheduler( String name, int numThreads ) {
        this.schedulerName = name;
        this.numThreads    = numThreads;
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

            publicMailboxes = new Mailbox[numThreads];
            for ( int i=0; i<numThreads; i++ ) {
                Monitor lock          = new Monitor();
                Mailbox publicMailbox = new SynchronizedMailboxWrapper(new NotifyAllMailboxWrapper(new LinkedListMailbox(),lock),lock);
                publicMailboxes[i] = publicMailbox;

                String       threadName  = threadNamePrefix+(i+1);
                WorkerThread workerThread = new WorkerThread( threadName, publicMailbox, lock );
                workerThread.start();

                threads.add( workerThread );
            }

            stripedMailbox = StripedMailbox.createStripedMailbox( publicMailboxes );
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void stop() {
        synchronized ( LOCK ) {
            stripedMailbox = null;
            isRunning      = false;

            for ( WorkerThread t : threads ) {
                t.wakeUpIfWaitingOnMailbox();
            }

            threads.clear();
        }
    }


    private class WorkerThread extends Thread {
        private final Mailbox publicMailbox;
        private final Monitor publicMailboxLock;
        private final Mailbox privateMailbox     = new LinkedListMailbox();

        public WorkerThread( String threadName, Mailbox publicMailbox, Monitor publicMailboxLock ) {
            super(threadName);

            this.publicMailbox     = publicMailbox;
            this.publicMailboxLock = publicMailboxLock;
        }


        @Override
        public void run() {
            while ( isRunning ) {
                invokeAllJobsFromPrivateMailbox();

                invokeJobsFromPublicMailboxBlocking();
            }
        }

        public void wakeUpIfWaitingOnMailbox() {
            synchronized ( publicMailboxLock ) {
                publicMailboxLock.notifyAll();
            }
        }

        private void invokeAllJobsFromPrivateMailbox() {
            boolean jobsRan = invokeJobs( publicMailbox );

            while ( jobsRan ) {
                jobsRan = invokeJobs( publicMailbox );
            }
        }

        private void invokeJobsFromPublicMailboxBlocking() {
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

        private boolean invokeJobs( Mailbox mailbox ) {
            EnhancedIterable<AsyncJob> jobs = mailbox.bulkPop();

            for ( AsyncJob j : jobs ) {
                try {
                    j.invoke( null );
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return !jobs.isEmpty();
        }
    }
}

