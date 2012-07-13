package com.mosaic.jact.jobqueues;

import com.mosaic.jact.AsyncJob;
import com.mosaic.lang.conc.Monitor;

/**
 *
 */
public class BlockingJobQueueWrapper extends BaseJobQueueWrapper {
    private final Monitor LOCK;

    public BlockingJobQueueWrapper( JobQueue wrappedQueue ) {
        this( wrappedQueue, new Monitor() );
    }

    public BlockingJobQueueWrapper( JobQueue wrappedQueue, Monitor lock ) {
        super( wrappedQueue );

        LOCK = lock;
    }


    @Override
    public AsyncJob pop() {
        AsyncJob job = super.pop();

        if ( job == null ) {
            LOCK.sleep();

            job = super.pop();
        }

        return job;
    }

    @Override
    public JobQueue bulkPop() {
        JobQueue childQueue = super.bulkPop();

        if ( childQueue.isEmpty() ) {
            LOCK.sleep();

            childQueue = super.bulkPop();
        }

        return childQueue;
    }
}
