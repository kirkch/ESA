package com.mosaic.jact.local;

import com.mosaic.jact.AsyncScheduler;
import com.mosaic.jact.AsyncJob;
import com.mosaic.lang.Future;
import com.mosaic.lang.Validate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
public class ThreadBasedAsyncScheduler implements AsyncScheduler {
    private final String threadsPrefix;
    private final int    threadCount;

    private final AtomicLong threadIdCounter = new AtomicLong(0);

    private List<WorkerThread> threads = new ArrayList<WorkerThread>();

    public ThreadBasedAsyncScheduler( String threadNamePrefix, int threadCount ) {
        Validate.isGTZero( threadCount,      "threadCount" );
        Validate.notBlank( threadNamePrefix, "threadNamePrefix" );

        this.threadsPrefix = threadNamePrefix;
        this.threadCount   = threadCount;
    }

    public void start() {
        for ( int i=0; i<threadCount; i++ ) {
            WorkerThread t = new WorkerThread( threadsPrefix+threadIdCounter.incrementAndGet() );

            t.start();

            threads.add( t );
        }
    }

    public boolean isRunning() {
        return false;
    }

    public void stop() {
        for ( WorkerThread t : threads ) {
            t.stop();
        }

        threads.clear();
    }


    public <T> Future<T> schedule( AsyncJob<T> job ) {
        WorkerThread t = selectActorFor( job );

        return t.schedule( job );
    }

    private WorkerThread selectActorFor( AsyncJob job ) {
        return threads.get( job.hashCode() % threads.size() );
    }
}
