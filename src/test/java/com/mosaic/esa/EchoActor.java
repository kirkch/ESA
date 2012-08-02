package com.mosaic.esa;

import com.mosaic.Future;

/**
 *
 */
@SuppressWarnings("unchecked")
public class EchoActor implements Echoer {


    @Override
    public Future<String> id() {
        return new Future(Integer.toString(System.identityHashCode(this)));
    }

    public Future<String> echo( String txt ) {
        return new Future(txt);
    }

}
