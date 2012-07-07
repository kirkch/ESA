package com.mosaic.jact.jobqueues;

import com.mosaic.jact.AsyncJob;
import com.mosaic.lang.Validate;

/**
 *
 */
public class LinkedListJobQueue implements JobQueue {

    private Element head = null;
    private Element tail = null;


    public LinkedListJobQueue() {}

    LinkedListJobQueue( Element head, Element tail ) {
        this.head = head;
        this.tail = tail;
    }

    public boolean maintainsOrder() {
        return true;
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

        if ( tail != null ) {
            tail.next = e;
        } else {
            head = e;
        }

        tail = e;
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
        JobQueue spawn = new LinkedListJobQueue( head, tail );

        this.head = null;
        this.tail = null;

        return spawn;
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
