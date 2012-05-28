package com.mosaic.jact;

import com.mosaic.lang.Future;

/**
 *
 */
public interface AsyncScheduler {

    public void start();
    public boolean isRunning();
    public void stop();

    public <T> Future<T> schedule( AsyncJob<T> job );
}
