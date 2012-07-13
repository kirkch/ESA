package com.mosaic.jact.jobqueues;

import com.mosaic.jact.AsyncJob;

/**
 * A thread coordination mechanism.
 */
public interface JobQueue {

    /**
     * Returns true if the mailbox ensures jobs are popped in the same order that they were pushed.
     */
    public boolean maintainsOrder();
    public boolean isThreadSafe();

    public boolean isEmpty();
    public boolean hasContents();

    public void push( AsyncJob job );


    /**
     *
     * @return null if no job is in the mailbox
     */
    public AsyncJob pop();

    /**
     * Returns a mailbox with a subset or all of this mailboxes mail in it. Useful when access to this mailbox involves
     * high overheads such as concurrency locks. Any mail returned by this method will be removed from the original
     * mailbox. Any thread safe guarantees of this mailbox will not be maintained by the mailbox returned by this method.
     * The assumption is that the caller of this method will consume the mail directly from the new mailbox without having
     * to share access to the new mailbox.
     *
     * @return never returns null
     */
    public JobQueue bulkPop();

}
