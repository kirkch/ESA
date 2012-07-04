package com.mosaic.jact.mailboxes;

import org.mockito.Mockito;


public class SlowStripedMailboxTests extends MailboxInterfaceTestCases {

    public SlowStripedMailboxTests() {
        this( Mockito.mock( MailboxListener.class ) );
    }

    private SlowStripedMailboxTests( MailboxListener l ) {
        super( StripedMailbox.createStripedMailbox( new Mailbox[] {new CASMailbox(), new CASMailbox(), new CASMailbox()}, l ), l );
    }

}