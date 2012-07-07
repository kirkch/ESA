package com.mosaic.jact.jobqueues;

import com.mosaic.jact.AsyncJob;
import com.mosaic.lang.Validate;

/**
 *
 */
public class LinkedListJobQueue implements JobQueue {

    private Element head = null;


    public LinkedListJobQueue() {}

    LinkedListJobQueue( Element head ) {
        this.head = head;
    }

    public boolean maintainsOrder() {
        return false;
    }

    public boolean isThreadSafe() {
        return false;
    }

    public boolean isEmpty() {
        return head == null;
    }

    public boolean hasContents() {
        return !isEmpty();
    }

    public void push( AsyncJob job ) {
        Element e = new Element(job);

        e.next = head;

        head = e;
    }

    public AsyncJob pop() {
        if ( head == null ) {
            return null;
        }

        AsyncJob job = head.job;

        head = head.next;

        return job;
    }

    /**
     * Returns a new mailbox with all of this mailboxes elements within it. Clearing this mailbox.
     */
    public JobQueue bulkPop() {
        JobQueue batch = new LinkedListJobQueue( head );

        this.head = null;

        return batch;
    }


    static class Element {
        public final AsyncJob job;

        public Element next;

        public Element( AsyncJob job ) {
            Validate.notNull( job, "job" );

            this.job = job;
        }
    }

}
