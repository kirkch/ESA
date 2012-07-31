package com.mosaic.esa.impl;

import com.mosaic.Future;
import com.mosaic.esa.ActorException;
import com.mosaic.esa.ActorSystem;
import com.mosaic.esa.reflection.ReflectionException;
import com.mosaic.esa.reflection.ReflectionUtils;
import com.mosaic.schedulers.MultiThreadedScheduler;
import net.sf.cglib.proxy.Enhancer;

/**
 *
 */
@SuppressWarnings("unchecked")
public class ActorSystemImpl implements ActorSystem {
    private MultiThreadedScheduler scheduler;

    public ActorSystemImpl( MultiThreadedScheduler scheduler ) {
        this.scheduler = scheduler;
    }

    @Override
    public void start() {
        scheduler.start();
    }

    @Override
    public void stop() {
        scheduler.stop();
    }

    @Override
    public <T> T newActor( Class<T> actorType ) {
        throwIfNotRunning( actorType );

        T actor        = ReflectionUtils.newInstance(actorType);
        T proxiedActor = (T) Enhancer.create( actorType, new ActorMethodInterceptor(actor));

        return proxiedActor;
    }

    @Override
    public <T> T newActor( Class<T> actorType, final T underlyingInstance ) {
        throwIfNotRunning( actorType );

        T proxiedActor = (T) Enhancer.create( actorType, new ActorMethodInterceptor(underlyingInstance));

        return proxiedActor;
    }

    @Override
    public <T> Future<T> newActorIfAbsent( String actorName, Class<T> actorType ) {
        throwIfNotRunning( actorType );

        try {
            T actor = ReflectionUtils.newInstance(actorType);
            T proxiedActor = (T) Enhancer.create( actorType, new ActorMethodInterceptor(actor));

            return new Future(proxiedActor);
        } catch ( ReflectionException ex ) {
            return new Future(ex);
        }
    }

    @Override
    public <T> Future<T> newActorIfAbsent( String actorName, Class<T> actorType, T underlyingInstance ) {
        throwIfNotRunning( actorType );

        T proxiedActor = (T) Enhancer.create( actorType, new ActorMethodInterceptor(underlyingInstance));

        return new Future(proxiedActor);
    }

    @Override
    public <T> Future<T> lookup( String actorName, Class<T> expectedActorType ) {
        return null;
    }

    @Override
    public <T> Future terminateActor( T actor ) {
        return null;
    }

    private <T> void throwIfNotRunning( Class<T> actorType ) {
        if ( !scheduler.isRunning() ) {
            throw new ActorException( "unable to start instance of "+actorType.getName()+" because the actor system is not running" );
        }
    }
}
