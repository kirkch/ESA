package com.mosaic.jact.jobqueues;

import com.mosaic.jact.AsyncJob;
import com.mosaic.jact.jobqueues.LinkedListJobQueue.Element;

import java.util.concurrent.atomic.AtomicReference;

/**
 * A thread safe, lockless version of LinkedListJobQueue. Faster than the synchronized linked list but it does not
 * guarantee order. In fact it will tend to reverse it, so most recent added will usually be next to execute over old
 * ones. Meaning that under load this queue is at risk of suffering from staleness.
 */
public class CASJobQueue implements JobQueue {
    private final AtomicReference<Element> jobQueueRef = new AtomicReference<Element>( null );


    public boolean maintainsOrder() {
        return false;
    }

    public boolean isThreadSafe() {
        return true;
    }

    public boolean isEmpty() {
        Element head = jobQueueRef.get();

        return head == null;
    }

    public boolean hasContents() {
        return !isEmpty();
    }

    public void push( AsyncJob job ) {
        Element e = new Element(job);

        while ( true ) {
            Element currentHead = jobQueueRef.get();

            e.popNextElement = currentHead;

            boolean wasSuccessful = jobQueueRef.compareAndSet( currentHead, e );
            if ( wasSuccessful ) {
                return;
            }
        }
    }

    public AsyncJob pop() {
        Element head;
        boolean wasSuccessful;

        while (true) {
            head = jobQueueRef.get();
            if ( head == null ) {
                return null;
            }

            wasSuccessful = jobQueueRef.compareAndSet( head, head.popNextElement );
            if ( wasSuccessful ) {
                return head.job;
            }
        }
    }

    /**
     * For speed over fairness all mail is copied into a new LinkedListJobQueue. By reusing data structures between
     * linked list and cas job queues, the movement of jobs between queues only involves assigning the list head to the
     * new job queue. Making it extremely fast to do, at the cost of risking hotspots in work allocation. Because this
     * library is being tuned for short lived, asynchronous jobs then the risk of hotspots is significantly reduced.
     * Especially if everything is tuned to be really fast. A different algorithm should be used for slow running or
     * thread blocking jobs.
     */
    public JobQueue bulkPop() {
        Element head = jobQueueRef.getAndSet( null );

        return new LinkedListJobQueue( head, null );
    }

}
