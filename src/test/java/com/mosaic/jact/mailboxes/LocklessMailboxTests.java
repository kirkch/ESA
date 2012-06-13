package com.mosaic.jact.mailboxes;

import org.mockito.Mockito;

/**
 *
 */
public class LocklessMailboxTests extends MailboxInterfaceTestCases {

    public LocklessMailboxTests() {
        this( Mockito.mock(MailboxListener.class) );
    }

    private LocklessMailboxTests( MailboxListener l ) {
        super( new LocklessMailbox(l), l, true );
    }

}
