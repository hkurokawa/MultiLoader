package com.hkurokawa.multiloader.internal;

/**
 * Represents a parameter type and its position in the listener method.
 * Created by hiroshi on 14/12/16.
 */
public class Parameter {
    static final Parameter[] NONE = new Parameter[0];

    private final int listenerPosition;
    private final String type;

    Parameter(int listenerPosition, String type) {
        this.listenerPosition = listenerPosition;
        this.type = type;
    }

    int getListenerPosition() {
        return listenerPosition;
    }

    String getType() {
        return type;
    }
}
