package com.mosaic.jact.local;

import com.mosaic.jact.AsyncJob;
import com.mosaic.jact.MailboxSpike;
import com.mosaic.lang.Future;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class SynchronizedMailbox implements MailboxSpike {
    private List<AsyncJob> queue = new ArrayList<AsyncJob>();

    public <T> Future<T> scheduleFromExternalThread( AsyncJob<T> job ) {
        synchronized ( queue ) {
            queue.add( job );
            queue.notify();
        }

        return job.getFuture();
    }

    public AsyncJob popNextJobBlocking() {
        synchronized ( queue ) {
            while ( true ) {
                int size = queue.size();

                if ( size == 0 ) {
                    try {
                        queue.wait();
                    } catch (InterruptedException e) {
                        return null;
                    }
                } else {
                    int i = size - 1;

                    return queue.remove( i );
                }
            }
        }
    }

}
