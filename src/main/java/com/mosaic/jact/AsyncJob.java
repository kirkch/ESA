package com.mosaic.jact;

import com.mosaic.lang.Future;

import java.util.concurrent.Callable;

/**
 *
 */
public abstract class AsyncJob<T> implements Callable<T> {
    protected final Future<T> future = new Future<T>();

    public Future<T> getFuture() {
        return future;
    }
}
