package com.mosaic.esa;

import com.mosaic.Future;

/**
 *
 */
@SuppressWarnings("unchecked")
public class EchoActor implements Echoer {

    public Future<String> echo( String txt ) {
        Future<String> f = new Future();

        f.completeWithResult( txt );

        return f;
    }

}
