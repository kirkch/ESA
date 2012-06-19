package com.mosaic.jact.mailboxes;

import org.mockito.Mockito;

/**
 *
 */
public class StripedMailboxTests extends MailboxInterfaceTestCases {

    public StripedMailboxTests() {
        this( Mockito.mock( MailboxListener.class ) );
    }

    private StripedMailboxTests( MailboxListener l ) {
        super( new StripedMailbox(CASMailbox.class,3,l), l );
    }

}
