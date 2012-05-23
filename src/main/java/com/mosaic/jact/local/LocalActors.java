package com.mosaic.jact.local;

import com.mosaic.jact.Actors;
import com.mosaic.lang.Validate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
public class LocalActors implements Actors {
    private final String threadsPrefix;
    private final int    threadCount;

    private final AtomicLong threadIdCounter = new AtomicLong(0);

    private List<ActorThread> threads = new ArrayList<ActorThread>();

    public LocalActors( String threadNamePrefix, int threadCount ) {
        Validate.isGTZero( threadCount,      "threadCount" );
        Validate.notBlank( threadNamePrefix, "threadNamePrefix" );

        this.threadsPrefix = threadNamePrefix;
        this.threadCount   = threadCount;
    }

    public void start() {
        for ( int i=0; i<threadCount; i++ ) {
            ActorThread t = new ActorThread( threadsPrefix+threadIdCounter.incrementAndGet() );

            t.start();

            threads.add( t );
        }
    }

    public boolean isRunning() {
        return false;
    }

    public void stop() {
        for ( ActorThread t : threads ) {
            t.stop();
        }

        threads.clear();
    }
}
