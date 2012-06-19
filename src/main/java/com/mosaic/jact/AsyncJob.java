package com.mosaic.jact;

import com.mosaic.lang.Future;

/**
 * Contains a unit of work.
 */
public abstract class AsyncJob<T> {
    protected final Future<T> future = new Future<T>();

    public Future<T> getFuture() {
        return future;
    }

    /**
     * Perform the task at hand.
     *
     * @param asyncContext allows scheduling of further work
     *
     * @return the value to return via the callers future
     *
     * @throws Exception any exception thrown will be passed to the future
     */
    public abstract T invoke( AsyncContext asyncContext ) throws Exception;
}
