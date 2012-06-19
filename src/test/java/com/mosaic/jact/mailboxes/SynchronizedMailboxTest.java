package com.mosaic.jact.mailboxes;

import org.mockito.Mockito;

/**
 *
 */
public class SynchronizedMailboxTest extends MailboxInterfaceTestCases {

    public SynchronizedMailboxTest() {
        this( Mockito.mock( MailboxListener.class ) );
    }

    private SynchronizedMailboxTest( MailboxListener l ) {
        super( new SynchronizedMailbox(l), l );
    }

}
