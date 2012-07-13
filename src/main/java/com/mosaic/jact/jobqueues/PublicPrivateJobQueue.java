package com.mosaic.jact.jobqueues;

import com.mosaic.jact.AsyncJob;

/**
 * A job queue that pulls work from two separate job queues.
 */
public class PublicPrivateJobQueue implements JobQueue {

    private final JobQueue publicQueue;
    private final JobQueue privateQueue;

    public PublicPrivateJobQueue( JobQueue publicQueue, JobQueue privateQueue ) {
        this.publicQueue  = publicQueue;
        this.privateQueue = privateQueue;
    }

    public boolean maintainsOrder() {
        return publicQueue.maintainsOrder() && privateQueue.maintainsOrder();
    }

    public boolean isThreadSafe() {
        return publicQueue.isThreadSafe();
    }

    public boolean isEmpty() {
        return privateQueue.isEmpty() && publicQueue.isEmpty();
    }

    public boolean hasContents() {
        return privateQueue.hasContents() || publicQueue.hasContents();
    }

    public void push( AsyncJob job ) {
        publicQueue.push( job );
    }

    public AsyncJob pop() {
        AsyncJob job = privateQueue.pop();
        if ( job == null ) {
            job = publicQueue.pop();
        }

        return job;
    }

    public JobQueue bulkPop() {
        JobQueue jobs = privateQueue.bulkPop();
        if ( jobs.isEmpty() ) {
            jobs = publicQueue.bulkPop();
        }

        return jobs;
    }

}
