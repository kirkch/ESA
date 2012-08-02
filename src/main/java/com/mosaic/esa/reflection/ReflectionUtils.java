package com.mosaic.esa.reflection;

/**
 *
 */
public class ReflectionUtils {
    public static <T> T newInstance( Class<T> clazz ) {
        try {
            return (T) clazz.newInstance();
        } catch (InstantiationException e) {
            throw new ReflectionException(e);
        } catch (IllegalAccessException e) {
            throw new ReflectionException(e);
        }
    }

    public static String tidyClassName( Class clazz ) {
        String fqn = clazz.getName();

        int index = fqn.lastIndexOf( "$$EnhancerByCGLIB$$" );
        if ( index < 0 ) {
            return fqn;
        }

        return fqn.substring( 0, index );
    }
}
