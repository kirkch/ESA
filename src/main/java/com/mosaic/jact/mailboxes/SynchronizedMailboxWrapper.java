package com.mosaic.jact.mailboxes;

import com.mosaic.jact.AsyncJob;
import com.mosaic.lang.EnhancedIterable;

/**
 * Synchronizes access to an existing mailbox.
 */
public class SynchronizedMailboxWrapper extends Mailbox {
    private final Mailbox wrappedMailbox;
    private final Object  LOCK;

    public SynchronizedMailboxWrapper( Mailbox wrappedMailbox ) {
        this( wrappedMailbox, new Object() );
    }

    public SynchronizedMailboxWrapper( Mailbox wrappedMailbox, Object lock ) {
        this.wrappedMailbox = wrappedMailbox;
        this.LOCK           = lock;
    }

    @Override
    public boolean maintainsOrder() {
        synchronized ( LOCK ) {
            return wrappedMailbox.maintainsOrder();
        }
    }

    @Override
    public boolean isThreadSafe() {
        synchronized ( LOCK ) {
            return wrappedMailbox.isThreadSafe();
        }
    }

    @Override
    public void push( AsyncJob job ) {
        synchronized ( LOCK ) {
            wrappedMailbox.push( job );
        }
    }

    @Override
    protected EnhancedIterable<AsyncJob> doPop() {
        synchronized ( LOCK ) {
            return wrappedMailbox.doPop();
        }
    }
}
