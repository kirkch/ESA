package com.mosaic.esa;

import com.mosaic.Future;

/**
 *
 */
@SuppressWarnings("unchecked")
public class CounterActor {

    private int counter;

    public Future<Integer> incrementThenGetCounter() {
        return new Future( ++counter );
    }

}
