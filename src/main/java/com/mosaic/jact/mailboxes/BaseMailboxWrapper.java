package com.mosaic.jact.mailboxes;

import com.mosaic.jact.AsyncJob;
import com.mosaic.lang.EnhancedIterable;

/**
 * Helper class for decorating mailboxes.
 */
public class BaseMailboxWrapper implements Mailbox {
    protected final Mailbox wrappedMailbox;

    public BaseMailboxWrapper( Mailbox wrappedMailbox ) {
        this.wrappedMailbox = wrappedMailbox;
    }

    public boolean maintainsOrder() {
        return wrappedMailbox.maintainsOrder();
    }

    public boolean isThreadSafe() {
        return wrappedMailbox.isThreadSafe();
    }

    public boolean isEmpty() {
        return wrappedMailbox.isEmpty();
    }

    public void push( AsyncJob job ) {
        wrappedMailbox.push( job );
    }

    public EnhancedIterable<AsyncJob> bulkPop() {
        return wrappedMailbox.bulkPop();
    }

    public AsyncJob pop() {
        return wrappedMailbox.pop();
    }

}
