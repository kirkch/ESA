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
        MultiThreadedScheduler scheduler         = new MultiThreadedScheduler( schedulerName, 2, 1 );
        int                    afterThreadCount  = countThreads( schedulerName );

        assertEquals( 0, beforeThreadCount );
        assertEquals( 0, afterThreadCount );
    }

    @Test
    public void startScheduler_asyncThreadsOnly_expectThreadsToSpinUp() {
        String                 schedulerName = nextName();
        MultiThreadedScheduler scheduler     = new MultiThreadedScheduler( schedulerName, 2, 0 );

        scheduler.start();
        spinUntilThreadCountsReaches( schedulerName, 2 );
    }

    @Test
    public void startScheduler_blockingThreadsOnly_expectThreadsToSpinUp() {
        String                 schedulerName = nextName();
        MultiThreadedScheduler scheduler     = new MultiThreadedScheduler( schedulerName, 0, 1 );

        scheduler.start();
        spinUntilThreadCountsReaches( schedulerName, 1 );
    }

    @Test
    public void startScheduler_expectThreadsToSpinUp() {
        String                 schedulerName = nextName();
        MultiThreadedScheduler scheduler     = new MultiThreadedScheduler( schedulerName, 2, 1 );

        scheduler.start();
        spinUntilThreadCountsReaches( schedulerName, 3 );
    }


    @Test
    public void startSchedulerTwice_expectSecondCallToNotEffectThreadCount() {
        String                 schedulerName = nextName();
        MultiThreadedScheduler scheduler     = new MultiThreadedScheduler( schedulerName, 2, 1 );

        scheduler.start();
        spinUntilThreadCountsReaches( schedulerName, 3 );

        scheduler.start();

        sleep(50);
        spinUntilThreadCountsReaches( schedulerName, 3 );
    }

    @Test
    public void stopScheduler_expectThreadsToSpinDown() {
        String                 schedulerName = nextName();
        MultiThreadedScheduler scheduler     = new MultiThreadedScheduler( schedulerName, 2, 1 );
        scheduler.start();

        scheduler.stop();
        spinUntilThreadCountsReaches( schedulerName, 0 );
    }

    @Test
    public void givenNotRunningScheduler_callIsRunning_expectFalse() {
        String                 schedulerName = nextName();
        MultiThreadedScheduler scheduler     = new MultiThreadedScheduler( schedulerName, 2, 1 );

        assertFalse( scheduler.isRunning() );
    }

    @Test
    public void givenRunningScheduler_callIsRunning_expectTrue() {
        String                 schedulerName = nextName();
        MultiThreadedScheduler scheduler     = new MultiThreadedScheduler( schedulerName, 2, 1 );

        scheduler.start();
        assertTrue( scheduler.isRunning() );
    }

    @Test
    public void givenStoppedScheduler_callIsRunning_expectFalse() {
        String                 schedulerName = nextName();
        MultiThreadedScheduler scheduler     = new MultiThreadedScheduler( schedulerName, 2, 1 );

        scheduler.start();
        scheduler.stop();
        assertFalse( scheduler.isRunning() );
    }

    @Test
    public void givenNotRunningScheduler_scheduleWork_expectISE() throws InterruptedException {
        String                 schedulerName = nextName();
        MultiThreadedScheduler scheduler     = new MultiThreadedScheduler( schedulerName, 2, 1 );

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
        MultiThreadedScheduler scheduler     = new MultiThreadedScheduler( schedulerName, 2, 1 );

        scheduler.start();

        CountDownLatch latch = new CountDownLatch(1);
        scheduler.schedule( new CountDownJob(latch) );

        boolean wasTriggered = latch.await( 500, TimeUnit.MILLISECONDS );
        assertTrue( wasTriggered );
    }

    @Test
    public void scheduleSeveralJobs_expectAllToRun() throws InterruptedException {
        String                 schedulerName = nextName();
        MultiThreadedScheduler scheduler     = new MultiThreadedScheduler( schedulerName, 2, 1 );

        scheduler.start();

        CountDownLatch latch = new CountDownLatch(4);
        for ( int i=0; i<4; i++ ) {
            scheduler.schedule( new CountDownJob(latch) );
        }

        boolean wasTriggered = latch.await( 500, TimeUnit.MILLISECONDS );
        assertTrue( wasTriggered );
    }

    @Test
    public void scheduleJobThatSchedulesAnotherLocalJobPrivately_expectBothJobsToRun() throws InterruptedException {
        String                 schedulerName = nextName();
        MultiThreadedScheduler scheduler     = new MultiThreadedScheduler( schedulerName, 2, 1 );

        scheduler.start();

        CountDownLatch latch = new CountDownLatch(2);
        scheduler.schedule( new CountDownJobsLocally(latch) );

        boolean wasTriggered = latch.await( 500, TimeUnit.MILLISECONDS );
        assertTrue( wasTriggered );
    }

    @Test
    public void scheduleJobThatSchedulesAnotherLocalJobPublically_expectBothJobsToRun() throws InterruptedException {
        String                 schedulerName = nextName();
        MultiThreadedScheduler scheduler     = new MultiThreadedScheduler( schedulerName, 2, 1 );

        scheduler.start();

        CountDownLatch latch = new CountDownLatch(2);
        scheduler.schedule( new CountDownJobsPublicly(latch) );

        boolean wasTriggered = latch.await( 500, TimeUnit.MILLISECONDS );
        assertTrue( wasTriggered );
    }

    @Test
    public void scheduleJobThatSchedulesABlockingJob_expectBothJobsToRun() throws InterruptedException {
        String                 schedulerName = nextName();
        MultiThreadedScheduler scheduler     = new MultiThreadedScheduler( schedulerName, 2, 1 );

        scheduler.start();

        CountDownLatch latch = new CountDownLatch(2);
        scheduler.schedule( new CountDownJobsBlocking(latch) );

        boolean wasTriggered = latch.await( 500, TimeUnit.MILLISECONDS );
        assertTrue( wasTriggered );
    }

    @Test
    public void scheduleJobThatSchedulesManyOtherJobsLocally_expectAllJobsToRun() throws InterruptedException {
        String                 schedulerName = nextName();
        MultiThreadedScheduler scheduler     = new MultiThreadedScheduler( schedulerName, 2, 1 );

        scheduler.start();

        int numTopLevelJobs = 20;

        CountDownLatch latch = new CountDownLatch(numTopLevelJobs*2);

        for ( int i=0; i<numTopLevelJobs; i++ ) {
            scheduler.schedule( new CountDownJobsLocally(latch) );
        }

        boolean wasTriggered = latch.await( 500, TimeUnit.MILLISECONDS );
        assertTrue( wasTriggered );
    }

    @Test
    public void scheduleJobThatSchedulesManyOtherBlockingJobs_expectAllJobsToRun() throws InterruptedException {
        String                 schedulerName = nextName();
        MultiThreadedScheduler scheduler     = new MultiThreadedScheduler( schedulerName, 2, 1 );

        scheduler.start();

        int numTopLevelJobs = 20;

        CountDownLatch latch = new CountDownLatch(numTopLevelJobs*2);

        for ( int i=0; i<numTopLevelJobs; i++ ) {
            scheduler.schedule( new CountDownJobsBlocking(latch) );
        }

        boolean wasTriggered = latch.await( 15000, TimeUnit.MILLISECONDS );

        if ( latch.getCount() > 0 ) {
            System.out.println( "latch " + latch.getCount() );
        }
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

    private static class CountDownJobsLocally extends AsyncJob {
        private CountDownLatch latch;

        public CountDownJobsLocally( CountDownLatch latch ) {
            this.latch = latch;
        }

        @Override
        public Object invoke( AsyncContext asyncContext ) throws Exception {
            latch.countDown();

            asyncContext.scheduleLocally( new CountDownJob(latch) );

            return null;
        }
    }

    private static class CountDownJobsPublicly extends AsyncJob {
        private CountDownLatch latch;

        public CountDownJobsPublicly( CountDownLatch latch ) {
            this.latch = latch;
        }

        @Override
        public Object invoke( AsyncContext asyncContext ) throws Exception {
            latch.countDown();

            asyncContext.schedule( new CountDownJob(latch) );

            return null;
        }
    }

    private static class CountDownJobsBlocking extends AsyncJob {
        private CountDownLatch latch;

        public CountDownJobsBlocking( CountDownLatch latch ) {
            this.latch = latch;
        }

        @Override
        public Object invoke( AsyncContext asyncContext ) throws Exception {
            latch.countDown();

            asyncContext.scheduleBlockableJob( new CountDownJob(latch) );

            return null;
        }
    }
}
