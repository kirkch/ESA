package com.mosaic.jact.mailboxes;

import com.mosaic.jact.AsyncJob;
import com.mosaic.lang.EnhancedIterable;

/**
 *
 */
public abstract class Mailbox {

    private Mailbox parent;

    /**
     * Returns true if the mailbox ensures jobs are popped in the same order that they were pushed.
     */
    public abstract boolean maintainsOrder();
    public abstract boolean isThreadSafe();

    public abstract void push( AsyncJob job );

    protected abstract EnhancedIterable<AsyncJob> doPop();


    public EnhancedIterable<AsyncJob> bulkPop() {
        EnhancedIterable<AsyncJob> jobs = doPop();

        if ( jobs.isEmpty() && isChained() ) {
            jobs = parent.bulkPop();
        }

        return jobs;
    }

    public boolean isChained() {
        return parent != null;
    }

    /**
     * When a unchained mailbox runs out of messages, then it is empty. A chained mailbox on the other hand will
     * ask its parent mailbox for mail before declaring itself empty.
     */
    public Mailbox chainTo( Mailbox parentMailbox ) {
        this.parent = parentMailbox;

        return this;
    }

    // monitor.awakeAll()

//    public EnhancedIterable<AsyncJob> blockingBulkPop() {
//        EnhancedIterable<AsyncJob> jobs = bulkPop();
//
//        while ( jobs.isEmpty() ) {
//            long cycleNumber = monitor.getCycleNumber();
//
//            jobs = bulkPop();
//            if ( jobs.isEmpty() ) {
//                monitor.sleepUntilAwoken( cycleNumber );
//
//                jobs = bulkPop();
//            }
//
//        }
//
//        return jobs;
//    }
}
