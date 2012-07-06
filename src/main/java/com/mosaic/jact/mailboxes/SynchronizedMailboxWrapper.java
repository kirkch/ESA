package com.mosaic.jact.mailboxes;

import com.mosaic.jact.AsyncJob;
import com.mosaic.lang.EnhancedIterable;
import com.mosaic.lang.conc.Monitor;

/**
 * Synchronizes access to an existing mailbox.
 */
public class SynchronizedMailboxWrapper implements Mailbox {
    private final Mailbox wrappedMailbox;
    private final Monitor LOCK;

    public SynchronizedMailboxWrapper( Mailbox wrappedMailbox ) {
        this( wrappedMailbox, new Monitor() );
    }

    public SynchronizedMailboxWrapper( Mailbox wrappedMailbox, Monitor lock ) {
        this.wrappedMailbox = wrappedMailbox;
        this.LOCK           = lock;
    }

    public boolean maintainsOrder() {
        synchronized ( LOCK ) {
            return wrappedMailbox.maintainsOrder();
        }
    }

    public boolean isThreadSafe() {
        synchronized ( LOCK ) {
            return wrappedMailbox.isThreadSafe();
        }
    }

    public boolean isEmpty() {
        synchronized ( LOCK ) {
            return wrappedMailbox.isEmpty();
        }
    }

    public void push( AsyncJob job ) {
        synchronized ( LOCK ) {
            wrappedMailbox.push( job );
        }
    }

    public AsyncJob pop() {
        synchronized ( LOCK ) {
            return wrappedMailbox.pop();
        }
    }

    public EnhancedIterable<AsyncJob> bulkPop() {
        synchronized ( LOCK ) {
            return wrappedMailbox.bulkPop();
        }
    }

}
