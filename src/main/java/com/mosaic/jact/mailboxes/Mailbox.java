package com.mosaic.jact.mailboxes;

import com.mosaic.jact.AsyncJob;
import com.mosaic.lang.EnhancedIterable;

/**
 *
 */
public interface Mailbox {

    /**
     * Returns true if the mailbox ensures jobs are popped in the same order that they were pushed.
     */
    public abstract boolean maintainsOrder();
    public abstract boolean isThreadSafe();
    public abstract boolean isEmpty();

    public abstract void push( AsyncJob job );


    /**
     *
     * @return null if no job is in the mailbox
     */
    public abstract AsyncJob pop();

    public abstract EnhancedIterable<AsyncJob> bulkPop();

}
