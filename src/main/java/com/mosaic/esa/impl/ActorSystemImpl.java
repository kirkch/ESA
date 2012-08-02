package com.mosaic.esa.impl;

import com.mosaic.Future;
import com.mosaic.esa.ActorException;
import com.mosaic.esa.ActorSystem;
import com.mosaic.esa.reflection.ReflectionException;
import com.mosaic.esa.reflection.ReflectionUtils;
import com.mosaic.schedulers.MultiThreadedScheduler;
import net.sf.cglib.proxy.Enhancer;

import java.util.concurrent.ConcurrentHashMap;

import static com.mosaic.esa.reflection.ReflectionUtils.tidyClassName;

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

    private final ConcurrentHashMap actors = new ConcurrentHashMap();

    @Override
    public <T> Future<T> newActorIfAbsent( String actorName, Class<T> targetActorType ) {
        throwIfNotRunning( targetActorType );

        try {
            T actor = ReflectionUtils.newInstance(targetActorType);
            T proxiedActor = (T) Enhancer.create( targetActorType, new ActorMethodInterceptor(actor));


            Object existingActor = actors.putIfAbsent( actorName, proxiedActor );
            if ( existingActor == null ) {
                return new Future(proxiedActor);
            } else if ( !targetActorType.isAssignableFrom(existingActor.getClass()) ) {
                String errorDescription = String.format("an actor named '%s' already exists, however its type (%s) does not match the target type of %s",actorName, tidyClassName(existingActor.getClass()), tidyClassName(targetActorType) );

                return new Future( new ClassCastException(errorDescription) );
            } else {
                return new Future(existingActor);
            }
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
    public <T> Future cancelActor( T actor ) {
        return null;
    }

    private <T> void throwIfNotRunning( Class<T> actorType ) {
        if ( !scheduler.isRunning() ) {
            throw new ActorException( "unable to start instance of "+actorType.getName()+" because the actor system is not running" );
        }
    }
}
