package com.mosaic.jact.jobqueues;

import com.mosaic.jact.AsyncJob;
import com.mosaic.lang.Validate;

/**
 *
 */
public class LinkedListJobQueue implements JobQueue {

    private Element popEnd    = null;
    private Element insertEnd = null;


    public LinkedListJobQueue() {}

    LinkedListJobQueue( Element popEnd, Element insertEnd ) {
        this.popEnd = popEnd;
        this.insertEnd = insertEnd;
    }

    public boolean maintainsOrder() {
        return true;
    }

    public boolean isThreadSafe() {
        return false;
    }

    public boolean isEmpty() {
        return popEnd == null;
    }

    public boolean hasContents() {
        return !isEmpty();
    }

    public void push( AsyncJob job ) {
        Element e = new Element(job);

        if ( insertEnd == null ) {
            popEnd    = e;
            insertEnd = e;
        } else {
            insertEnd.popNextElement = e;
            insertEnd                = e;
        }
    }

    public AsyncJob pop() {
        if ( popEnd == null ) {
            return null;
        }

        AsyncJob job = popEnd.job;

        popEnd = popEnd.popNextElement;

        if ( popEnd == null ) {
            insertEnd = null;
        }

        return job;
    }

    /**
     * Returns a new mailbox with all of this mailboxes elements within it. Clearing this mailbox.
     */
    public JobQueue bulkPop() {
        JobQueue spawn = new LinkedListJobQueue( popEnd, insertEnd );

        this.popEnd = null;
        this.insertEnd = null;

        return spawn;
    }


    static class Element {
        public final AsyncJob job;

        public Element popNextElement;

        public Element( AsyncJob job ) {
            Validate.notNull( job, "job" );

            this.job = job;
        }
    }

}
