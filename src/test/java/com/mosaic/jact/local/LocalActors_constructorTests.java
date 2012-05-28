package com.mosaic.jact.local;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 *
 */
public class LocalActors_constructorTests {

    @Test
    public void passNegativeThreadCountToConstructor_expectException() {
        try {
            new ThreadBasedAsyncScheduler( "actors", -1 );
            fail( "expected IAE" );
        } catch ( IllegalArgumentException e ) {
            assertEquals( "'threadCount' (-1) must be >= 1", e.getMessage() );
        }
    }

    @Test
    public void passZeroThreadCountToConstructor_expectException() {
        try {
            new ThreadBasedAsyncScheduler( "actors", 0 );
            fail( "expected IAE" );
        } catch ( IllegalArgumentException e ) {
            assertEquals( "'threadCount' (0) must be >= 1", e.getMessage() );
        }
    }

    @Test
    public void passNullThreadNamePrefixToConstructor_expectException() {
        try {
            new ThreadBasedAsyncScheduler( null, 1 );
            fail( "expected IAE" );
        } catch ( IllegalArgumentException e ) {
            assertEquals( "'threadNamePrefix' must not be null", e.getMessage() );
        }
    }

    @Test
    public void passBlankThreadNamePrefixToConstructor_expectException() {
        try {
            new ThreadBasedAsyncScheduler( "  ", 1 );
            fail( "expected IAE" );
        } catch ( IllegalArgumentException e ) {
            assertEquals( "'threadNamePrefix' must not be '  '", e.getMessage() );
        }
    }

}
