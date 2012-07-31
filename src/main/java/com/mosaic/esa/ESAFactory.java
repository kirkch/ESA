package com.mosaic.esa;

import com.mosaic.esa.impl.ActorSystemImpl;
import com.mosaic.schedulers.MultiThreadedScheduler;

/**
 *
 */
public class ESAFactory {

    public static ActorSystem createLocalSystem( String systemName, int numThreads ) {
        if ( numThreads == 1 ) {
            return createSingleThreadedLocalSystem(systemName);
        } else {
            return createSingleMultiThreadedLocalSystem( systemName, numThreads );
        }
    }

    private static ActorSystem createSingleThreadedLocalSystem( String systemName ) {
        MultiThreadedScheduler scheduler = new MultiThreadedScheduler( systemName, 1, 0 );

        return new ActorSystemImpl( scheduler );
    }

    private static ActorSystem createSingleMultiThreadedLocalSystem( String systemName, int numThreads ) {
        return null;
    }

}
