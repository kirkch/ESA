package com.mosaic.jact.jobqueues;

import com.mosaic.jact.AsyncJob;
import com.mosaic.lang.conc.Monitor;

/**
 *
 */
public class NotifyAllJobQueueWrapper extends BaseJobQueueWrapper {

    private final Monitor LOCK;

    public NotifyAllJobQueueWrapper( JobQueue wrappedMailbox, Monitor lock ) {
        super( wrappedMailbox );

        LOCK = lock;
    }

    public void push( AsyncJob job ) {
        super.push( job );

        LOCK.wakeAll();
    }

}
