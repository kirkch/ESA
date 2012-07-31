package com.mosaic.esa.impl;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 *
 */
public class ActorMethodInterceptor implements MethodInterceptor {

    private Object obj;

    public ActorMethodInterceptor( Object obj ) {
        this.obj = obj;
    }

    @Override
    public Object intercept( Object o, Method method, Object[] args, MethodProxy methodProxy ) throws Throwable {
        return method.invoke( obj, args );
    }

}
