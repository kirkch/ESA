package com.mosaic.jact;

import com.mosaic.jact.schedulers.AsyncScheduler;
import com.mosaic.lang.Future;

/**
 * Allows scheduling of extra work. Made available to AsyncJobs as they are invoked, offering them a way to schedule more
 * work efficiently by offering them several scheduling options.
 */
public class AsyncContext {

    private final AsyncScheduler publicScheduler;
    private final AsyncScheduler privateScheduler;
    private final AsyncScheduler blockableScheduler;

    public AsyncContext( AsyncScheduler publicScheduler, AsyncScheduler privateScheduler, AsyncScheduler blockableScheduler ) {
        this.publicScheduler    = publicScheduler;
        this.privateScheduler   = privateScheduler;
        this.blockableScheduler = blockableScheduler;
    }

    /**
     * Balance the allocation of the jobs across multiple threads. If available. Has higher context switching
     * costs to scheduleLocally but allows for greater parallelism where hardware and os allow. <p/>
     *
     * Do not schedule jobs that could block the thread here.
     */
    public <T> Future<T> schedule( AsyncJob<T> job ) {
        return publicScheduler.schedule( job );
    }

    /**
     * Schedule the job on the thread that is bound to this context. Involves less allocation and context switching
     * overhead when compared to spreading the load out across multiple threads but at the cost of less parallelism.<p/>
     *
     * Do not schedule jobs that could block the thread here.
     */
    public <T> Future<T> scheduleLocally( AsyncJob<T> job ) {
// todo have an assertion mode that checks the calling thread is the owning thread
        return privateScheduler.schedule( job );
    }

    /**
     * Schedule in a separate pool of threads that are over provisioned given the number of cpu cores. The mailboxes
     * are shared between all of the threads and contention is not considered a large bottleneck as it is expected that
     * the threads will spend most of their time blocked on locks within the jobs themselves.
     */
    public <T> Future<T> scheduleBlockableJob( AsyncJob<T> job ) {
        return blockableScheduler.schedule( job );
    }

}
