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

    private final Monitor       LOCK    = new Monitor();
    private final List<Monitor> threadMonitors = new ArrayList<Monitor>();

    private final String schedulerName;
    private final int    numNonBlockingThreads;
    private       int    numBlockingThreads;

    private volatile boolean    isRunning       = false;
    private volatile JobQueue   stripedJobQueue = null;



    public MultiThreadedScheduler( String name ) {
        this( name, Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors()*10 );
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

            Monitor            blockingJobsMonitor      = new Monitor();
            LinkedListJobQueue underlyingBlockableQueue = new LinkedListJobQueue();


            String threadNamePrefix = schedulerName + "_";

            createAndStartNonBlockingWorkerThreads( blockingJobsMonitor, underlyingBlockableQueue, threadNamePrefix );
            createAndStartBlockableWorkerThreads( blockingJobsMonitor, underlyingBlockableQueue, threadNamePrefix );
        }
    }

    private void createAndStartNonBlockingWorkerThreads( Monitor blockingJobsMonitor, LinkedListJobQueue underlyingBlockableQueue, String threadNamePrefix ) {
        AsyncScheduler blockingScheduler = new JobQueueScheduler( new SynchronizedJobQueueWrapper(new NotifyAllJobQueueWrapper(underlyingBlockableQueue,blockingJobsMonitor),blockingJobsMonitor) );

        JobQueue[] publicJobQueues = new JobQueue[numNonBlockingThreads];
        stripedJobQueue = StripedJobQueueFactory.stripeJobQueues( publicJobQueues );

        for ( int i=0; i< numNonBlockingThreads; i++ ) {
            Monitor            stripeLock               = new Monitor();
            LinkedListJobQueue underlyingPublicJobQueue = new LinkedListJobQueue();

            publicJobQueues[i] = new SynchronizedJobQueueWrapper(new NotifyAllJobQueueWrapper(underlyingPublicJobQueue,stripeLock),stripeLock);

            String       threadName   = threadNamePrefix+(i+1);
            WorkerThread workerThread = createNonBlockingWorkerThread( threadName, stripedJobQueue, underlyingPublicJobQueue, stripeLock, blockingScheduler );

            workerThread.start();

            threadMonitors.add( stripeLock );
        }
    }

    private void createAndStartBlockableWorkerThreads( Monitor blockingJobsMonitor, JobQueue threadSafeBlockingJobQueue, String threadNamePrefix ) {
        AsyncScheduler nonBlockingScheduler = new JobQueueScheduler(stripedJobQueue);

        for ( int i=0; i<numBlockingThreads; i++ ) {
            String threadName  = threadNamePrefix+(i+1+numNonBlockingThreads);

            WorkerThread workerThread = createBlockableWorkerThread( threadName, threadSafeBlockingJobQueue, blockingJobsMonitor, nonBlockingScheduler );
            workerThread.start();
        }

        threadMonitors.add( blockingJobsMonitor );
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


    private WorkerThread createNonBlockingWorkerThread( String threadName, JobQueue strippedJobQueue, JobQueue publicJobQueue, Monitor stripeLock, AsyncScheduler blockableScheduler ) {
        JobQueue privateJobQueue = new LinkedListJobQueue();

        AsyncContext asyncContext = new AsyncContext( new JobQueueScheduler(strippedJobQueue), new JobQueueScheduler(privateJobQueue), blockableScheduler );
        JobQueue     jobQueue     = new SynchronizedJobQueueWrapper( new BlockingJobQueueWrapper( new PublicPrivateJobQueue(publicJobQueue,privateJobQueue), stripeLock ), stripeLock );

        return new WorkerThread( threadName+"-NonBlocking", asyncContext, jobQueue );
    }

    private WorkerThread createBlockableWorkerThread( String threadName, JobQueue blockingJobsQueue, Monitor blockingJobsQueueMonitor, AsyncScheduler nonBlockingScheduler ) {
        JobQueue     jobQueue     = new SynchronizedJobQueueWrapper( new BlockingJobQueueWrapper( blockingJobsQueue, blockingJobsQueueMonitor ), blockingJobsQueueMonitor);
        AsyncContext asyncContext = new AsyncContext( nonBlockingScheduler, nonBlockingScheduler, new JobQueueScheduler(blockingJobsQueue) );

        return new WorkerThread( threadName+"-Blockable", asyncContext, jobQueue );
    }

    private class WorkerThread extends Thread {
        private final AsyncContext asyncContext;
        private final JobQueue     jobQueue;

        public WorkerThread( String threadName, AsyncContext asyncContext, JobQueue jobQueue ) {
            super(threadName);

            this.asyncContext = asyncContext;
            this.jobQueue     = jobQueue;
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
}
