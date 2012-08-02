package com.mosaic.esa;

import com.mosaic.Future;

/**
 *
 */
public interface Echoer {

    public Future<String> id();

    public Future<String> echo(String txt);

}
