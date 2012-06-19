package com.mosaic.jact;

import com.mosaic.jact.schedulers.AsyncScheduler;
import com.mosaic.lang.Future;

/**
 * Allows scheduling of extra work.
 */
public class AsyncContext {

    public AsyncContext( AsyncScheduler defaultScheduler, AsyncScheduler localScheduler, AsyncScheduler blockableScheduler ) {

    }

    /**
     * Balance the allocation of the jobs across multiple threads. If available. Has higher context switching
     * costs to scheduleLocally but allows for greater parallelism where hardware and os allow.
     */
    public <T> Future<T> schedule( AsyncJob<T> job ) {
        return null;
    }

    /**
     * Schedule the job on the thread that is bound to this context. Involves less allocation and context switching
     * overhead when compared to spreading the load out across multiple threads but at the cost of less parallelism.
     */
    public <T> Future<T> scheduleLocally( AsyncJob<T> job ) {
        return null;
    }

    public <T> Future<T> scheduleBlockableJob( AsyncJob<T> job ) {
        return null;
    }

}
