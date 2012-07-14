package com.mosaic.jact.jobqueues;

import com.mosaic.jact.AsyncJob;
import com.mosaic.lang.conc.Monitor;

/**
 * Blocks when the wrapped job queue is empty. Requires the caller to already be synchronized against the supplied lock
 * before calling pop or bulkPop. Otherwise thread timing issues could occur.
 */
public class BlockingJobQueueWrapper extends BaseJobQueueWrapper {
    private final Monitor LOCK;

    public BlockingJobQueueWrapper( JobQueue wrappedQueue, Monitor lock ) {
        super( wrappedQueue );

        LOCK = lock;
    }


    @Override
    public AsyncJob pop() {
        AsyncJob job = super.pop();

        if ( job == null ) {
            sleep();

            job = super.pop();
        }

        return job;
    }

    @Override
    public JobQueue bulkPop() {
        JobQueue childQueue = super.bulkPop();

        if ( childQueue.isEmpty() ) {
            sleep();

            childQueue = super.bulkPop();
        }

        return childQueue;
    }

    private void sleep() {
        try {
            // NB not calling LOCK.sleep so as to detect when this wrapper does not already have the lock synchronized
            LOCK.wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
