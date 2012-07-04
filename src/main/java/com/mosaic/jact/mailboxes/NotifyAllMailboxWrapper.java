package com.mosaic.jact.mailboxes;

import com.mosaic.jact.AsyncJob;
import com.mosaic.lang.conc.Monitor;

/**
 *
 */
public class NotifyAllMailboxWrapper extends BaseMailboxWrapper {
    private final Monitor LOCK;

    public NotifyAllMailboxWrapper( Mailbox wrappedMailbox, Monitor lock ) {
        super( wrappedMailbox );

        LOCK = lock;
    }

    @Override
    public void push( AsyncJob job ) {
        super.push( job );

        LOCK.wakeAll();
    }
}
