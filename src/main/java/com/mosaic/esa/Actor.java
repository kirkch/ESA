package com.mosaic.esa;

/**
 * Optional Actor interface that gives an actor access to the underlying system,
 */
public interface Actor {

    public void setActorDirectory( ActorDirectory directory );

    public void uncaughtException( Throwable e );
    public void lowMemoryWarning();

    public void preActorStart();
    public void preActorStop();

}
