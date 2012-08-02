package com.mosaic.esa;

import com.mosaic.Error;
import com.mosaic.jtunit.TestTools;
import org.junit.Test;

import java.lang.ref.WeakReference;

import static org.junit.Assert.*;

/**
 *
 */
@SuppressWarnings({"ThrowableResultOfMethodCallIgnored", "UnusedAssignment", "unchecked"})
public class ActorSystem_singleThreadedSystemTests extends SharedTestCases {
    private String systemName = TestTools.nextName();

    @Test
    public void startSystem_expectOneThreadToBeStarted() {
        startActorSystem();

        TestTools.spinUntilThreadCountsReaches( systemName, 1 );
    }

    @Test
    public void stopSystem_expectOneThreadToGoAway() {
        ActorSystem system = startActorSystem();

        system.stop();

        TestTools.spinUntilThreadCountsReaches( systemName, 0 );
    }

    @Test
    public void givenStoppedSystem_startAnonymousActor_expectError() {
        ActorSystem system = startActorSystem();

        system.stop();

        try {
            system.newActor( EchoActor.class );
            fail( "expected ActorException" );
        } catch ( ActorException ex ) {
            assertEquals( "unable to start instance of com.mosaic.esa.EchoActor because the actor system is not running", ex.getMessage() );
        }
    }

    @Test
    public void givenStoppedSystem_startNamedActor_expectError() {
        ActorSystem system = startActorSystem();

        system.stop();

        try {
            system.newActorIfAbsent( "echo", EchoActor.class );
            fail( "expected ActorException" );
        } catch ( ActorException ex ) {
            assertEquals( "unable to start instance of com.mosaic.esa.EchoActor because the actor system is not running", ex.getMessage() );
        }
    }

    @Test
    public void startAnonymousActorWithSuppliedInstance_expectRefReturned() {
        ActorSystem system = startActorSystem();

        Echoer actor = system.newActor( Echoer.class, new EchoActor() );

        assertNotNull( actor );
    }

    @Test
    public void givenRunningActorWithSuppliedInstance_sendMessageToActor_expectSuccess() {
        ActorSystem system = startActorSystem();

        Echoer actor = system.newActor( Echoer.class, new EchoActor() );

        assertEquals( "hello", actor.echo( "hello" ).getResultBlocking( 20 ) );
    }

    @Test
    public void startAnonymousActor_expectRefReturned() {
        ActorSystem system = startActorSystem();

        Echoer actor = system.newActor( EchoActor.class );

        assertNotNull( actor );
    }

    @Test
    public void givenRunningActor_sendMessageToActor_expectSuccess() {
        ActorSystem system = startActorSystem();

        Echoer actor = system.newActor( EchoActor.class );

        assertEquals( "hello", actor.echo( "hello" ).getResultBlocking( 20 ) );
    }

    @Test
    public void givenNamedActorUsingInterface_sendMessageToActor_expectSuccess() {
        ActorSystem system = startActorSystem();

        Echoer actor = system.newActorIfAbsent( "a1", EchoActor.class ).getResult();

        assertEquals( "hello", actor.echo( "hello" ).getResult() );
    }

    @Test
    public void givenNamedActorWithNoInterface_sendMessageToActor_expectSuccess() {
        ActorSystem system = startActorSystem();

        CounterActor actor = system.newActorIfAbsent( "counter", CounterActor.class ).getResult();

        assertEquals( 1, actor.incrementThenGetCounter().getResult().intValue() );
    }

    @Test
    public void startTwoActorsWithSameName_expectSecondRequestToReturnTheFirstActor() {
        ActorSystem system = startActorSystem();

        Echoer actor1 = system.newActorIfAbsent( "a1", EchoActor.class ).getResult();
        Echoer actor2 = system.newActorIfAbsent( "a1", EchoActor.class ).getResult();

        assertEquals( actor1.id().getResult(), actor2.id().getResult() );
    }

    @Test
    public void startTwoActorsWithSameName_secondActorWithDifferentType_expectSecondRequestToReturnTypeClassCastException() {
        ActorSystem system = startActorSystem();

        system.newActorIfAbsent( "a1", EchoActor.class ).getResult();

        Error actor2Error = system.newActorIfAbsent( "a1", CounterActor.class ).getError();

        assertEquals( ClassCastException.class, actor2Error.asException().getClass() );
        assertEquals( "an actor named 'a1' already exists, however its type (com.mosaic.esa.EchoActor) does not match the target type of com.mosaic.esa.CounterActor", actor2Error.asException().getMessage() );
    }

    @Test
    public void startTwoAnonymousActors_expectRefsForBoth() {
        ActorSystem system = startActorSystem();

        Echoer actor1 = system.newActor( EchoActor.class );
        Echoer actor2 = system.newActor( EchoActor.class );

        assertFalse( actor1.id().getResult().equals( actor2.id().getResult() ) );
    }

    @Test
    public void givenRunningActor_stopActor_expectActorToBeGCd() {
        ActorSystem system = startActorSystem();

        Echoer actor1 = system.newActor( EchoActor.class );
        system.cancelActor( actor1 );

        WeakReference ref = new WeakReference(actor1);
        actor1 = null;

        TestTools.spinUntilReleased( ref );
    }

//    @Test
    public void givenStoppedActor_sendMessageToActor_expectError() {
        ActorSystem system = startActorSystem();

        Echoer actor1 = system.newActor( EchoActor.class );
        system.cancelActor( actor1 );

        Error error = actor1.echo("hello").getError();

        assertEquals( "unable to invoke echo(\"hello\") on cancelled anonymous actor of type com.mosaic.esa.EchoActor", error.getDescription() );
        assertEquals( ActorErrors.CANCELLED_ACTOR, error.getErrorType() );
    }



    //
    //
    //
    //
    // givenStoppedActor_retrieveActorByName_expectNoMatch

    // givenRunningNamedActor_stopActor_expectActorToBeGCd
    // givenStoppedNamedActor_sendMessageToActor_expectError
    // givenStoppedNamedActor_retrieveActorByName_expectNoMatch

    // givenRunningActors_sendMessagesBetweenActors_expectSuccess
    // givenRunningActors_stopOneActorAndSendMessageToStoppedActorFromRunningActor_expectError

    private ActorSystem startActorSystem() {
        ActorSystem system = ESAFactory.createLocalSystem( systemName, 1 );

        system.start();

        return system;
    }

}
