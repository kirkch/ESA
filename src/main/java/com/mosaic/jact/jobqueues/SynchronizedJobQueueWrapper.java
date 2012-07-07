package com.mosaic.jact.jobqueues;

import com.mosaic.jact.AsyncJob;
import com.mosaic.lang.conc.Monitor;

/**
 * Synchronizes access to an existing job queue.
 */
public class SynchronizedJobQueueWrapper implements JobQueue {
    private final JobQueue wrappedJobQueue;
    private final Monitor  LOCK;

    public SynchronizedJobQueueWrapper( JobQueue wrappedJobQueue ) {
        this( wrappedJobQueue, new Monitor() );
    }

    public SynchronizedJobQueueWrapper( JobQueue wrappedJobQueue, Monitor lock ) {
        this.wrappedJobQueue = wrappedJobQueue;
        this.LOCK            = lock;
    }

    public boolean maintainsOrder() {
        synchronized ( LOCK ) {
            return wrappedJobQueue.maintainsOrder();
        }
    }

    public boolean isThreadSafe() {
        synchronized ( LOCK ) {
            return wrappedJobQueue.isThreadSafe();
        }
    }

    public boolean isEmpty() {
        synchronized ( LOCK ) {
            return wrappedJobQueue.isEmpty();
        }
    }

    public boolean hasContents() {
        return !isEmpty();
    }

    public void push( AsyncJob job ) {
        synchronized ( LOCK ) {
            wrappedJobQueue.push( job );
        }
    }

    public AsyncJob pop() {
        synchronized ( LOCK ) {
            return wrappedJobQueue.pop();
        }
    }

    public JobQueue bulkPop() {
        synchronized ( LOCK ) {
            return wrappedJobQueue.bulkPop();
        }
    }

}
