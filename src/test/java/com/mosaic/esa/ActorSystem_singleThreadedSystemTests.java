package com.mosaic.esa;

import com.mosaic.jtunit.TestTools;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 */
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

        assertEquals( "hello", actor.echo("hello").getResultBlocking(20) );
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

        assertEquals( "hello", actor.echo("hello").getResultBlocking(20) );
    }

    @Test
    public void givenNamedActor_sendMessageToActor_expectSuccess() {
        ActorSystem system = startActorSystem();

        Echoer actor = system.newActorIfAbsent( "a1", EchoActor.class ).getResult();

        assertEquals( "hello", actor.echo("hello").getResult() );
    }



    // startTwoActorsWithSameName_expectSecondRequestToError
    // startTwoAnonymousActors_expectRefsForBoth
    // givenRunningActor_stopActor_expectActorToBeGCd
    // givenStoppedActor_sendMessageToActor_expectError
    // givenRunningActor_sendMessageToActor_expectSuccess
    // givenRunningActors_sendMessagesBetweenActors_expectSuccess
    // givenRunningActors_stopOneActorAndSendMessageToStoppedActorFromRunningActor_expectError

    private ActorSystem startActorSystem() {
        ActorSystem system = ESAFactory.createLocalSystem( systemName, 1 );

        system.start();

        return system;
    }

}
