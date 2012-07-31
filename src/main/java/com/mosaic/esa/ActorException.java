package com.mosaic.esa;

/**
 *
 */
public class ActorException extends RuntimeException {
    public ActorException() {
        super();
    }

    public ActorException( String message ) {
        super( message );
    }

    public ActorException( String message, Throwable cause ) {
        super( message, cause );
    }

    public ActorException( Throwable cause ) {
        super( cause );
    }
}
