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
    public void push( AsyncJob job ) {
        wrappedMailbox.push( job );
    }

    @Override
    protected EnhancedIterable<AsyncJob> doPop() {
        return wrappedMailbox.doPop();
    }
}
