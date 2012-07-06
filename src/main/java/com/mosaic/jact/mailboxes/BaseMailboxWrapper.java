package com.mosaic.jact.mailboxes;

import com.mosaic.jact.AsyncJob;
import com.mosaic.lang.EnhancedIterable;

/**
 * Helper class for decorating mailboxes.
 */
public class BaseMailboxWrapper extends Mailbox {
    protected final Mailbox wrappedMailbox;

    public BaseMailboxWrapper( Mailbox wrappedMailbox ) {
        this.wrappedMailbox = wrappedMailbox;
    }

    @Override
    public boolean maintainsOrder() {
        return wrappedMailbox.maintainsOrder();
    }

    @Override
    public boolean isThreadSafe() {
        return wrappedMailbox.isThreadSafe();
    }

    @Override
    public boolean isEmpty() {
        return wrappedMailbox.isEmpty();
    }

    @Override
    public void push( AsyncJob job ) {
        wrappedMailbox.push( job );
    }

    @Override
    public EnhancedIterable<AsyncJob> bulkPop() {
        return wrappedMailbox.bulkPop();
    }

    @Override
    public AsyncJob pop() {
        return wrappedMailbox.pop();
    }
}
