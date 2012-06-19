package com.mosaic.jact.mailboxes;

import org.mockito.Mockito;

/**
 *
 */
public class LinkedListMailboxTests extends MailboxInterfaceTestCases {

    public LinkedListMailboxTests() {
        this( Mockito.mock( MailboxListener.class ) );
    }

    private LinkedListMailboxTests( MailboxListener l ) {
        super( new LinkedListMailbox(l), l );
    }
}
