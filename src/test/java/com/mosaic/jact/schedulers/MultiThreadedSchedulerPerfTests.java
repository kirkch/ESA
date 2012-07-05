package com.mosaic.jact.schedulers;

import com.mosaic.jact.AsyncContext;
import com.mosaic.jact.AsyncJob;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 *
 */
@Ignore
public class MultiThreadedSchedulerPerfTests {


    @Test
    public void throughputTest() throws InterruptedException {
        MultiThreadedScheduler scheduler = new MultiThreadedScheduler( "throughput", 8, 0 );

        scheduler.start();

        int numTopLevelJobs = 100;

        CountDownLatch latch = new CountDownLatch(numTopLevelJobs*2);

        for ( int i=0; i<numTopLevelJobs; i++ ) {
            scheduler.schedule( new SumJob(0) );
        }

        boolean wasTriggered = latch.await( 60, TimeUnit.SECONDS );
//        assertTrue( wasTriggered );
    }

//    @Test   -- Runs out of memory
    public void javaExecutors() throws InterruptedException {
        ExecutorService s = Executors.newFixedThreadPool(8);

        Runnable r = new SumRunnable(s, 0);

        for ( int i=0; i<100; i++ ) {
            s.submit( r );
        }

        CountDownLatch latch = new CountDownLatch(100);

        boolean wasTriggered = latch.await( 60, TimeUnit.SECONDS );
    }

    class SumRunnable implements Runnable {
        private int v = 0;
        private int level;
        private ExecutorService s;

        public SumRunnable( ExecutorService s, int level ) {
            this.s = s;
            this.level = level;
        }

        public void run() {
            if ( level < 7 ) {
                for ( int i=0; i<10; i++ ) {
                    s.submit( new SumRunnable(s,level+1) );
                }
            }
        }
    }

    private static class SumJob extends AsyncJob {
        private int v = 0;
        private int level;

        public SumJob(int level) {
            this.level = level;
        }

        @Override
        public Object invoke( AsyncContext asyncContext ) throws Exception {
            v++;

            if ( level < 7 ) {
                for ( int i=0; i<10; i++ ) {
                    asyncContext.scheduleLocally( new SumJob(level+1) ); // apx 5m per second per thread
//                    asyncContext.schedule( new SumJob(level+1) );      // apx 4m per second per thread
                }
            }

            return null;
        }
    }

}
