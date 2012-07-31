package com.mosaic.esa;

import com.mosaic.Future;

/**
 * Supports creating, terminating and locating actors.
 *
 * Valid Actor Interface:
 *
 * <ul>
 *     <li>Methods must return void or a Future.</li>
 * </ul>
 */
public interface ActorDirectory {

    /**
     * Start an anonymous actor of the specified type. The specified class must have a no-arg constructor which will
     * be used to instantiate the underlying instance.
     */
    public <T> T newActor( Class<T> actorType );

    /**
     * Start an anonymous actor of the specified type, wrapping the specified instance.
     */
    public <T> T newActor( Class<T> actorType, T underlyingInstance );

    /**
     * Start an named actor of the specified type. The specified class must have a no-arg constructor which will
     * be used to instantiate the underlying instance. Named actors may be located via a call to lookup.<p/>
     *
     * If actor already exists then that actor will be returned. Else the newly created actor will be returned.
     */
    public <T> Future<T> newActorIfAbsent( String actorName, Class<T> actorType );

    /**
     * Start an named actor of the specified type. Named actors may be located via a call to lookup.<p/>
     *
     * If actor already exists then that actor will be returned. Else the newly created actor will be returned.
     */
    public <T> Future<T> newActorIfAbsent( String actorName, Class<T> actorType, T underlyingInstance );

    /**
     * Locate and return a reference to the specified named actor.
     */
    public <T> Future<T> lookup( String actorName, Class<T> expectedActorType );

    /**
     * Wait for the actors current workload to finish and then release its memory back to the void.
     */
    public <T> Future terminateActor( T actor );

}
