package com.mosaic.jact.local;

import com.mosaic.jact.AsyncJob;
import com.mosaic.jact.Mailbox;
import com.mosaic.lang.Future;

/**
 *
 */
public class WorkerThread {
    private Thread  thread;
    private Mailbox mailbox = new SynchronizedMailbox();

    public WorkerThread( String threadName ) {
        thread = new Thread( new ActorRunnable(), threadName );
    }

    public void start() {
        thread.start();
    }

    public void stop() {
        thread.interrupt();
    }

    public <T> Future<T> schedule( AsyncJob<T> job ) {
        return mailbox.scheduleFromExternalThread( job );
    }


    private class ActorRunnable implements Runnable {
        volatile boolean isRunningFlag = true;

        public void run() {
            while ( isRunningFlag ) {
                AsyncJob job = mailbox.popNextJobBlocking();
                if ( job == null ) { break; }

                Future   future = job.getFuture();

                try {
                    Object result = job.call();

                    future.completeWithResult( result );
                } catch ( Throwable e ) {
                    future.completeWithException( e );
                }
            }
        }

    }

}
