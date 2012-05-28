package com.mosaic.jact;

import com.mosaic.jact.local.ThreadBasedAsyncScheduler;

/**
 *
 */
public class Schedulers {
    private Schedulers() {}


    public static AsyncScheduler newThreadBasedScheduler( String threadNamePrefix, int threadCount ) {
        return new ThreadBasedAsyncScheduler( threadNamePrefix, threadCount );
    }

}
