package com.hkurokawa.multiloader;

import android.app.Activity;
import android.app.LoaderManager;

import com.hkurokawa.multiloader.internal.MultiLoaderProcessor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by hiroshi on 14/12/15.
 */
public final class MultiLoader {
    private MultiLoader() {
        throw new AssertionError("No instances.");
    }

    public static void inject(Activity target, int... ids) throws InjectionFailureException {
        for (int id : ids) {
            final String clsName = target.getClass().getName() + MultiLoaderProcessor.SUFFIX + id;
            try {
                final Class<? extends LoaderManager.LoaderCallbacks> callbackClass = (Class<? extends LoaderManager.LoaderCallbacks>) Class.forName(clsName);
                final Object cb = callbackClass.newInstance();
                final Method method = callbackClass.getMethod("setActivity", target.getClass());
                method.invoke(cb, target);
                target.getLoaderManager().initLoader(id, null, (LoaderManager.LoaderCallbacks) cb);
            } catch (Exception e) {
                throw new InjectionFailureException("Failed to initialize loader for [" + id + "]: " + e.getMessage(), e);
            }
        }
    }

    public static class InjectionFailureException extends Exception {
        public InjectionFailureException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }
}
