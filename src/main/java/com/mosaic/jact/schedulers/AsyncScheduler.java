package com.mosaic.jact.schedulers;

import com.mosaic.jact.AsyncJob;
import com.mosaic.lang.Future;

/**
 * Schedule a piece of work to execute at a later date.
 */
public interface AsyncScheduler {

    public <T> Future<T> schedule( AsyncJob<T> job );

}
