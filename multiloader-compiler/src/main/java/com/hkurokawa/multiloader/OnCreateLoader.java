package com.hkurokawa.multiloader;

import com.hkurokawa.multiloader.internal.ListenerClass;
import com.hkurokawa.multiloader.internal.ListenerMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by hiroshi on 12/15/14.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
@ListenerClass(
        type = "com.hkurokawa.multiloader.OnCreateLoader",
        method = @ListenerMethod(
                name = "onCreateLoader",
                parameters = {"int", "android.os.Bundle"},
                returnType = "android.content.Loader"
        )
)
public @interface OnCreateLoader {
    int value();
}
