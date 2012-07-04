package com.mosaic.jact.mailboxes;

import com.mosaic.jact.AsyncJob;

/**
 *
 */
public class NotifyAllMailboxWrapper extends BaseMailboxWrapper {
    private final Object LOCK;

    public NotifyAllMailboxWrapper( Mailbox wrappedMailbox, Object lock ) {
        super( wrappedMailbox );

        LOCK = lock;
    }

    @Override
    public void push( AsyncJob job ) {
        super.push( job );

        LOCK.notifyAll();
    }
}
