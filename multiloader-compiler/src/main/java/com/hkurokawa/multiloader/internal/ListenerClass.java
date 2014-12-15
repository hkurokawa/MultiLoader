package com.hkurokawa.multiloader.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by hiroshi on 14/12/16.
 */
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.ANNOTATION_TYPE)
public @interface ListenerClass {
        /** Fully-qualified class name of the listener type. */
        String type();

        /** The number of generic arguments for the type. This used used for casting the view. */
        int genericArguments() default 0;

        /**
         * Method data for single-method listener callbacks.
         */
        ListenerMethod method();
}
