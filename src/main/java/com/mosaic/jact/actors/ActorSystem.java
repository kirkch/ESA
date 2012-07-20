package com.mosaic.jact.actors;

import com.mosaic.Future;

/**
 *
 *
 */
public interface ActorSystem extends ActorDirectory {

    public Future start();
    public Future shutdown();

}
