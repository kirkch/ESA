package com.mosaic.jact.schedulers;

import com.mosaic.jact.AsyncJob;
import com.mosaic.jact.jobqueues.JobQueue;
import com.mosaic.lang.Future;

/**
 *
 */
public class JobQueueScheduler implements AsyncScheduler {
    private JobQueue mailbox;

    public JobQueueScheduler( JobQueue mb ) {
        this.mailbox = mb;
    }

    public <T> Future<T> schedule( AsyncJob<T> job ) {
        mailbox.push( job );

        return job.getFuture();
    }
}
