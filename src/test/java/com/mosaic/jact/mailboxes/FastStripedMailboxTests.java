package com.mosaic.jact.mailboxes;

import org.mockito.Mockito;

/**
 *
 */
public class FastStripedMailboxTests extends MailboxInterfaceTestCases {

    public FastStripedMailboxTests() {
        this( Mockito.mock( MailboxListener.class ) );
    }

    private FastStripedMailboxTests( MailboxListener l ) {
        super( StripedMailbox.createStripedMailbox(new Mailbox[] {new CASMailbox(),new CASMailbox()},l), l );
    }

}
