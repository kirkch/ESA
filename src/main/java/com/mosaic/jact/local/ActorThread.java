package com.mosaic.jact.local;

/**
 *
 */
public class ActorThread {
    private Thread thread;

    public ActorThread( String threadName ) {
        thread = new Thread( new ActorRunnable(), threadName );
    }

    public void start() {
        thread.start();
    }

    public void stop() {
        thread.interrupt();
    }


    private static class ActorRunnable implements Runnable {

        public void run() {
            try {
                Thread.sleep( Long.MAX_VALUE );
            } catch (InterruptedException e) {

            }
        }

    }

}
