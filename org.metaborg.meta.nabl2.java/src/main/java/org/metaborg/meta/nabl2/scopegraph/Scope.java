package org.metaborg.meta.nabl2.scopegraph;

import org.immutables.value.Value;

@Value.Immutable
public abstract class Scope {

    @Value.Parameter public abstract String getResource();

    @Value.Parameter public abstract String getName();

    @Override public String toString() {
        return "Scope(" + getResource() + "-" + getName() + ")";
    }

}