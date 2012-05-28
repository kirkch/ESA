package com.mosaic.jact.local;

import com.mosaic.jact.AsyncScheduler;
import com.mosaic.jtunit.TestContext;
import org.junit.Test;

import static com.mosaic.jtunit.Threads.*;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class LocalActors_startTests {
    private TestContext testRun       = new TestContext();
    private String      threadsPrefix = testRun.nextUniqueId();

    private AsyncScheduler actors = new ThreadBasedAsyncScheduler( threadsPrefix, 1 );

    @Test
    public void doNotStartActors_expectThreadCountToRemainAtZero() {
        spinUntilThreadCountsReaches( threadsPrefix, 0 );
        assertEquals( 0, countThreads( threadsPrefix ) );
    }

    @Test
    public void startActorStopActor_expectSupportingThreadsToStop() {
        actors.start();

        spinUntilThreadCountsReaches( threadsPrefix, 1 );
        assertEquals( 1, countThreads( threadsPrefix ) );

        actors.stop();

        spinUntilAllThreadsComplete( threadsPrefix );
        assertEquals( 0, countThreads( threadsPrefix ) );
    }
}
