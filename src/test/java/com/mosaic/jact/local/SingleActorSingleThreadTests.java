package com.mosaic.jact.local;

import com.mosaic.jact.AsyncJob;
import com.mosaic.jact.AsyncScheduler;
import com.mosaic.jtunit.JUnitTools;
import com.mosaic.jtunit.Predicate;
import com.mosaic.jtunit.TestContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

import static com.mosaic.jtunit.Threads.countThreads;
import static com.mosaic.jtunit.Threads.spinUntilAllThreadsComplete;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class SingleActorSingleThreadTests {

    private TestContext testRun       = new TestContext();
    private String      threadsPrefix = testRun.nextUniqueId();

    private AsyncScheduler actors = new ThreadBasedAsyncScheduler( threadsPrefix, 1 );

    private AtomicLong completedJobCounter = new AtomicLong(0);


    @Before
    public void setUp() {
        actors.start();
    }

    @After
    public void tearDown() {
        actors.stop();

        spinUntilAllThreadsComplete( threadsPrefix );
        assertEquals( 0, countThreads(threadsPrefix) );
    }

    @Test
    public void scheduleOneJob_expectItToRun() {
        actors.schedule( new MyTestJob() );

        spinUntilCompletedJobsCountEquals( 1 );
    }




    // scheduleTwoJobs_expectThemBothToRun_orderIsNotGuaranteed
    // scheduleAJobThatStartsAnotherJob_expectThemBothToRun
    // scheduleAJob_waitToFinishThenScheduleAnother_expectThemBothToRun
    // bulkTest_start100BatchesOf100JobsWithRandomInterfaces_expectAllToRun


    private void spinUntilCompletedJobsCountEquals( final int targetCompletedJobCount ) {
        JUnitTools.spinUntilTrue( new Predicate() {
            public boolean eval() {
                return completedJobCounter.get() == targetCompletedJobCount;
            }
        });
    }

    private class MyTestJob extends AsyncJob {
        public Object call() throws Exception {
            completedJobCounter.incrementAndGet();

            return null;
        }
    }
}
