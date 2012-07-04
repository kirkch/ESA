package com.mosaic.jact.schedulers;

import com.mosaic.jact.AsyncContext;
import com.mosaic.jact.AsyncJob;
import com.mosaic.jtunit.JUnitTools;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 *
 */
public class MultiThreadedSchedulerTest extends JUnitTools {

    @Test
    public void noThreadsCreatedInConstructor() {
        String                 schedulerName     = nextName();
        int                    beforeThreadCount = countThreads( schedulerName );
        MultiThreadedScheduler scheduler         = new MultiThreadedScheduler( schedulerName, 2 );
        int                    afterThreadCount  = countThreads( schedulerName );

        assertEquals( 0, beforeThreadCount );
        assertEquals( 0, afterThreadCount );
    }

    @Test
    public void startScheduler_expectThreadsToSpinUp() {
        String                 schedulerName = nextName();
        MultiThreadedScheduler scheduler     = new MultiThreadedScheduler( schedulerName, 2 );

        scheduler.start();
        spinUntilThreadCountsReaches( schedulerName, 2 );
    }


    @Test
    public void startSchedulerTwice_expectSecondCallToNotEffectThreadCount() {
        String                 schedulerName = nextName();
        MultiThreadedScheduler scheduler     = new MultiThreadedScheduler( schedulerName, 2 );

        scheduler.start();
        spinUntilThreadCountsReaches( schedulerName, 2 );

        scheduler.start();

        sleep(50);
        spinUntilThreadCountsReaches( schedulerName, 2 );
    }

    @Test
    public void stopScheduler_expectThreadsToSpinDown() {
        String                 schedulerName = nextName();
        MultiThreadedScheduler scheduler     = new MultiThreadedScheduler( schedulerName, 2 );
        scheduler.start();

        scheduler.stop();
        spinUntilThreadCountsReaches( schedulerName, 0 );
    }

    @Test
    public void givenNotRunningScheduler_callIsRunning_expectFalse() {
        String                 schedulerName = nextName();
        MultiThreadedScheduler scheduler     = new MultiThreadedScheduler( schedulerName, 2 );

        assertFalse( scheduler.isRunning() );
    }

    @Test
    public void givenRunningScheduler_callIsRunning_expectTrue() {
        String                 schedulerName = nextName();
        MultiThreadedScheduler scheduler     = new MultiThreadedScheduler( schedulerName, 2 );

        scheduler.start();
        assertTrue( scheduler.isRunning() );
    }

    @Test
    public void givenStoppedScheduler_callIsRunning_expectFalse() {
        String                 schedulerName = nextName();
        MultiThreadedScheduler scheduler     = new MultiThreadedScheduler( schedulerName, 2 );

        scheduler.start();
        scheduler.stop();
        assertFalse( scheduler.isRunning() );
    }

    @Test
    public void givenNotRunningScheduler_scheduleWork_expectISE() throws InterruptedException {
        String                 schedulerName = nextName();
        MultiThreadedScheduler scheduler     = new MultiThreadedScheduler( schedulerName, 2 );

        try {
            scheduler.schedule( new CountDownJob(new CountDownLatch(1)) );
            fail( "expected ISE" );
        } catch ( IllegalStateException e ) {
            assertEquals( String.format("%s scheduler is not running",schedulerName), e.getMessage() );
        }
    }

    @Test
    public void givenRunningScheduler_scheduleWork_expectItToRun() throws InterruptedException {
        String                 schedulerName = nextName();
        MultiThreadedScheduler scheduler     = new MultiThreadedScheduler( schedulerName, 2 );

        scheduler.start();

        CountDownLatch latch = new CountDownLatch(1);
        scheduler.schedule( new CountDownJob(latch) );

        boolean wasTriggered = latch.await( 500, TimeUnit.MILLISECONDS );
        assertTrue( wasTriggered );
    }


    private static class CountDownJob extends AsyncJob {
        private CountDownLatch latch;

        public CountDownJob( CountDownLatch latch ) {
            this.latch = latch;
        }

        @Override
        public Object invoke( AsyncContext asyncContext ) throws Exception {
            latch.countDown();

            return null;
        }
    }
}
