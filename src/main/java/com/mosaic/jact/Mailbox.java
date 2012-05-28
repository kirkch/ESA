package com.mosaic.jact;

import com.mosaic.lang.Future;

/**
 *
 */
public interface Mailbox {
    public <T> Future<T> scheduleFromExternalThread( AsyncJob<T> job );

    public AsyncJob popNextJobBlocking();
}
