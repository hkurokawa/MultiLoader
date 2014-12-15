package com.hkurokawa.multiloader.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by hiroshi on 14/12/16.
 */
class ListenerBinding implements Binding {
    private final String name;
    private final List<Parameter> parameters;

    ListenerBinding(String name, List<Parameter> parameters) {
        this.name = name;
        this.parameters = Collections.unmodifiableList(new ArrayList<Parameter>(parameters));
    }

    public String getName() {
        return name;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    @Override public String getDescription() {
        return "method '" + name + "'";
    }
}
