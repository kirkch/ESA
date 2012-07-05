package com.mosaic.jact.mailboxes;

import com.mosaic.jact.AsyncJob;
import com.mosaic.lang.EnhancedIterable;
import com.mosaic.lang.conc.Monitor;

/**
 * Synchronizes access to an existing mailbox.
 */
public class SynchronizedMailboxWrapper extends Mailbox {
    private final Mailbox wrappedMailbox;
    private final Monitor LOCK;

    public SynchronizedMailboxWrapper( Mailbox wrappedMailbox ) {
        this( wrappedMailbox, new Monitor() );
    }

    public SynchronizedMailboxWrapper( Mailbox wrappedMailbox, Monitor lock ) {
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
    public boolean isEmpty() {
        synchronized ( LOCK ) {
            return wrappedMailbox.isEmpty();
        }
    }

    @Override
    public void push( AsyncJob job ) {
        synchronized ( LOCK ) {
            wrappedMailbox.push( job );
        }
    }

    @Override
    public AsyncJob pop() {
        synchronized ( LOCK ) {
            return wrappedMailbox.pop();
        }
    }

    @Override
    public EnhancedIterable<AsyncJob> bulkPop() {
        synchronized ( LOCK ) {
            return wrappedMailbox.bulkPop();
        }
    }

    @Override
    public boolean isChained() {
        synchronized ( LOCK ) {
            return wrappedMailbox.isChained();
        }
    }

    @Override
    public Mailbox chainTo( Mailbox parentMailbox ) {
        synchronized ( LOCK ) {
            return wrappedMailbox.chainTo( parentMailbox );
        }
    }

    @Override
    protected AsyncJob doPop() {
        synchronized ( LOCK ) {
            return wrappedMailbox.doPop();
        }
    }

    @Override
    protected EnhancedIterable<AsyncJob> doBulkPop() {
        synchronized ( LOCK ) {
            return wrappedMailbox.doBulkPop();
        }
    }
}
