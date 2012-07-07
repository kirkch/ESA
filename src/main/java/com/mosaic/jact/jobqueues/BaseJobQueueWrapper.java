package com.mosaic.jact.jobqueues;

import com.mosaic.jact.AsyncJob;

/**
 * Helper class for decorating job queues.
 */
public class BaseJobQueueWrapper implements JobQueue {
    protected final JobQueue wrappedQueue;

    public BaseJobQueueWrapper( JobQueue wrappedQueue ) {
        this.wrappedQueue = wrappedQueue;
    }

    public boolean maintainsOrder() {
        return wrappedQueue.maintainsOrder();
    }

    public boolean isThreadSafe() {
        return wrappedQueue.isThreadSafe();
    }

    public boolean isEmpty() {
        return wrappedQueue.isEmpty();
    }

    public boolean hasContents() {
        return wrappedQueue.hasContents();
    }

    public void push( AsyncJob job ) {
        wrappedQueue.push( job );
    }

    public JobQueue bulkPop() {
        return wrappedQueue.bulkPop();
    }

    public AsyncJob pop() {
        return wrappedQueue.pop();
    }

}
