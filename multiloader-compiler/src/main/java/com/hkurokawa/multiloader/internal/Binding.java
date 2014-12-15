package com.hkurokawa.multiloader.internal;

/**
 * A field or method view injection binding.
 * Created by hiroshi on 14/12/16.
 */
interface Binding {
    /** A description of the binding in human readable form (e.g., "field 'foo'"). */
    String getDescription();
}
