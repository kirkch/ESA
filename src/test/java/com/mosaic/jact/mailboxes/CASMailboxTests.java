package com.mosaic.jact.mailboxes;

import org.mockito.Mockito;

/**
 *
 */
public class CASMailboxTests extends MailboxInterfaceTestCases {

    public CASMailboxTests() {
        this( Mockito.mock(MailboxListener.class) );
    }

    private CASMailboxTests( MailboxListener l ) {
        super( new CASMailbox(l), l );
    }

}
