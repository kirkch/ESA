package com.mosaic.jact.schedulers;

import com.mosaic.jact.AsyncJob;
import com.mosaic.jact.mailboxes.Mailbox;
import com.mosaic.lang.Future;

/**
 *
 */
public class MailboxScheduler implements AsyncScheduler {
    private Mailbox mailbox;

    public MailboxScheduler( Mailbox mb ) {
        this.mailbox = mb;
    }

    public <T> Future<T> schedule( AsyncJob<T> job ) {
        mailbox.push( job );

        return job.getFuture();
    }
}
