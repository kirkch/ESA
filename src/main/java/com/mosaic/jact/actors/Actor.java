package com.mosaic.jact.actors;

/**
 * Optional Actor interface that gives an actor access to the underlying system,
 */
public interface Actor {

    public void setActorDirectory( ActorDirectory directory );

    public void uncaughtException( Throwable e );
    public void lowMemoryWarning();

    public void actorStarted();
    public void actorStopped();

    public void enterReplayMode();
    public void exitReplayMode();

    public void enterMergeMode();
    public void exitMergeMode();

}
