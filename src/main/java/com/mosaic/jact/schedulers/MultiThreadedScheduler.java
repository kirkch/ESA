package com.mosaic.jact.schedulers;

import com.mosaic.jact.AsyncContext;
import com.mosaic.jact.AsyncJob;
import com.mosaic.jact.AsyncSystem;
import com.mosaic.jact.jobqueues.BlockingJobQueueWrapper;
import com.mosaic.jact.jobqueues.JobQueue;
import com.mosaic.jact.jobqueues.LinkedListJobQueue;
import com.mosaic.jact.jobqueues.NotifyAllJobQueueWrapper;
import com.mosaic.jact.jobqueues.PublicPrivateJobQueue;
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
 * This scheduler consists of two tiers of job queues. A job queue stores work until a thread becomes available to action the work.
 * Each thread is assigned two job queues, a public job queue which is thread safe and a private job queue which is not thread safe.<p/>
 *
 * When scheduling work via this scheduler, a threads public job queue will be selected without taking on any locks via the hashcode
 * of the work being scheduled. This will spread the work across the threads and avoid any single point of contention, improving
 * scalability of the scheduler. The work will then remain in the public job queue until a thread is ready to pull the work in.<p/>
 *
 * When a thread is started it loops over any work in its local job queue. The local job queue is extremely fast, and involves
 * no locks or thread synchronisation of any kind. It doesn't even use CAS or volatile statements. When the private job queue
 * is empty, as it will be when the thread first starts then the thread will try to pull work in from its public job queue.
 * Which does involve synchronisation. If the public job queue is also empty, then the thread will try to steal work from
 * other public job queues. If that also fails then the thread will go to sleep on its public job queues monitor,
 * waking only when new work is pushed onto its public job queue. <p/>
 *
 * This approach is biased towards throughput, giving excellent latency under load. The weaknesses of this approach is
 * that latency can suffer when the private job queues become saturated or work is not spread evenly over the public
 * job queues creating hot spots.<p/>
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
    private final int    numNonBlockingThreads;
    private       int    numBlockingThreads;

    private volatile boolean    isRunning       = false;
    private volatile JobQueue   stripedJobQueue = null;



    public MultiThreadedScheduler( String name ) {
        this( name, Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors()*4 );
    }

    /**
     *
     * @param name
     * @param numNonBlockingThreads number of worker threads to use; public jobQueue scheduling will be fastest if a value 2^n is used
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
            stripedJobQueue.push( job );
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

            Monitor        blockingJobsMonitor     = new Monitor();
            JobQueue       blockingJobsQueue       = new SynchronizedJobQueueWrapper( new NotifyAllJobQueueWrapper(new LinkedListJobQueue(),blockingJobsMonitor), blockingJobsMonitor );

            String         threadNamePrefix  = schedulerName + "_";
            AsyncScheduler blockingScheduler = new JobQueueScheduler( blockingJobsQueue );


            JobQueue[] publicJobQueues = new JobQueue[numNonBlockingThreads];
            stripedJobQueue = StripedJobQueueFactory.stripeJobQueues( publicJobQueues );

            for ( int i=0; i< numNonBlockingThreads; i++ ) {
                Monitor  lock           = new Monitor();
                JobQueue publicJobQueue = new SynchronizedJobQueueWrapper(new NotifyAllJobQueueWrapper(new LinkedListJobQueue(),lock),lock);

                publicJobQueues[i] = publicJobQueue;

                String                   threadName   = threadNamePrefix+(i+1);
                NoneBlockingWorkerThread workerThread = new NoneBlockingWorkerThread( threadName, stripedJobQueue, publicJobQueue, lock, blockingScheduler );

                workerThread.start();

                threadMonitors.add( lock );
            }




            AsyncScheduler nonBlockingScheduler = new JobQueueScheduler(stripedJobQueue);

            for ( int i=0; i<numBlockingThreads; i++ ) {
                String threadName  = threadNamePrefix+(i+1+numNonBlockingThreads);

                BlockableWorkerThread workerThread = new BlockableWorkerThread( threadName, blockingJobsQueue, blockingJobsMonitor, nonBlockingScheduler );
                workerThread.start();
            }

            threadMonitors.add( blockingJobsMonitor );
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void stop() {
        synchronized ( LOCK ) {
            stripedJobQueue = null;
            isRunning      = false;

            for ( Monitor lock : threadMonitors ) {
                lock.wakeAll();
            }

            threadMonitors.clear();
        }
    }


    private class NoneBlockingWorkerThread extends Thread {
        private final JobQueue     privateJobQueue     = new LinkedListJobQueue();
        private final AsyncContext asyncContext;

        private final JobQueue jobQueue;

        public NoneBlockingWorkerThread( String threadName, JobQueue strippedJobQueue, JobQueue publicJobQueue, Monitor publicJobQueueLock, AsyncScheduler blockableScheduler ) {
            super(threadName + "-NonBlocking");

            this.asyncContext = new AsyncContext( new JobQueueScheduler(strippedJobQueue), new JobQueueScheduler(privateJobQueue), blockableScheduler );
            this.jobQueue     = new BlockingJobQueueWrapper( new PublicPrivateJobQueue(publicJobQueue,privateJobQueue), publicJobQueueLock );
        }


        @Override
        public void run() {
            while ( isRunning ) {
                invokeJobs( jobQueue.bulkPop() );
            }
        }

        private void invokeJobs( JobQueue jobQueue ) {
            AsyncJob j = jobQueue.pop();

            while ( j != null ) {
                try {
                    j.invoke( asyncContext );
                } catch (Exception e) {
                    e.printStackTrace();
                }

                j = jobQueue.pop();
            }
        }
    }


    private class BlockableWorkerThread extends Thread {
        private final JobQueue jobQueue;
        private final Monitor  jobQueueMonitor;

        private final AsyncContext asyncContext;

        public BlockableWorkerThread( String threadName, JobQueue blockingJobsQueue, Monitor blockingJobsQueueMonitor, AsyncScheduler nonBlockingScheduler ) {
            super(threadName + "-Blockable");

            this.jobQueue        = blockingJobsQueue;
            this.jobQueueMonitor = blockingJobsQueueMonitor;
            this.asyncContext    = new AsyncContext( nonBlockingScheduler, nonBlockingScheduler, new JobQueueScheduler(blockingJobsQueue) );
        }


        @Override
        public void run() {
            while ( isRunning ) {
                AsyncJob job = jobQueue.pop();

                if ( job == null ) {
                    synchronized ( jobQueueMonitor ) {
                        job = jobQueue.pop();

                        if ( job == null ) {
                            jobQueueMonitor.sleep();
                        }
                    }
                } else {
                    try {
                        job.invoke( asyncContext );
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}

