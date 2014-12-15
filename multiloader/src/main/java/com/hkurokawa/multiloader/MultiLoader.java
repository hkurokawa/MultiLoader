package com.hkurokawa.multiloader;

import android.app.Activity;

import com.hkurokawa.multiloader.internal.MultiLoaderProcessor;

/**
 * Created by hiroshi on 14/12/15.
 */
public final class MultiLoader {
    private MultiLoader() {
        throw new AssertionError("No instances.");
    }

    public static void inject(Activity target, int...ids) {
        for (int id : ids) {
            final String clsName = target.getClass().getName() + MultiLoaderProcessor.SUFFIX + id;
        }
    }
}
