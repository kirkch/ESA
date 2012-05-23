package com.mosaic.jact;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 *
 */
public interface ActorRef {

    public <T> Future<T> schedule( Callable<T> work );

}
