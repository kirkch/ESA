package com.mosaic.jact.local;

import com.mosaic.jact.Actors;
import com.mosaic.jtunit.TestContext;
import org.junit.Test;

import static com.mosaic.jtunit.Threads.*;
import static org.junit.Assert.*;

/**
 *
 */
public class SingleActorSingleThreadTests {

    private TestContext testRun       = new TestContext();
    private String      threadsPrefix = testRun.nextUniqueId();

    @Test
    public void passNegativeThreadCountToConstructor_expectException() {
        try {
            new LocalActors( threadsPrefix, -1 );
            fail( "expected IAE" );
        } catch ( IllegalArgumentException e ) {
            assertEquals( "'threadCount' (-1) must be >= 1", e.getMessage() );
        }
    }

    @Test
    public void passZeroThreadCountToConstructor_expectException() {
        try {
            new LocalActors( threadsPrefix, 0 );
            fail( "expected IAE" );
        } catch ( IllegalArgumentException e ) {
            assertEquals( "'threadCount' (0) must be >= 1", e.getMessage() );
        }
    }

    @Test
    public void passNullThreadNamePrefixToConstructor_expectException() {
        try {
            new LocalActors( null, 1 );
            fail( "expected IAE" );
        } catch ( IllegalArgumentException e ) {
            assertEquals( "'threadNamePrefix' must not be null", e.getMessage() );
        }
    }

    @Test
    public void passBlankThreadNamePrefixToConstructor_expectException() {
        try {
            new LocalActors( "  ", 1 );
            fail( "expected IAE" );
        } catch ( IllegalArgumentException e ) {
            assertEquals( "'threadNamePrefix' must not be '  '", e.getMessage() );
        }
    }

    @Test
    public void doNotStartActors_expectThreadCountToRemainAtZero() {
        assertEquals( 0, countThreads( threadsPrefix ) );

        Actors actors = new LocalActors( threadsPrefix, 1 );

        spinUntilThreadCountsReaches( threadsPrefix, 0 );
        assertEquals( 0, countThreads( threadsPrefix ) );
    }

    @Test
    public void startActorStopActor_expectSupportingThreadsToStop() {
        assertEquals( 0, countThreads( threadsPrefix ) );

        Actors actors = new LocalActors( threadsPrefix, 1 );

        actors.start();

        spinUntilThreadCountsReaches( threadsPrefix, 1 );
        assertEquals( 1, countThreads( threadsPrefix ) );

        actors.stop();


        spinUntilAllThreadsComplete( threadsPrefix );
        assertEquals( 0, countThreads( threadsPrefix ) );
    }

    //
    // scheduleOneJob_expectItToRun_orderIsNotGuaranteed
    // scheduleTwoJobs_expectThemBothToRun_orderIsNotGuaranteed
    // scheduleAJobThatStartsAnotherJob_expectThemBothToRun
    // scheduleAJob_waitToFinishThenScheduleAnother_expectThemBothToRun
    // bulkTest_start100BatchesOf100JobsWithRandomInterfaces_expectAllToRun

}
