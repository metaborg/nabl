package org.metaborg.meta.nabl2.scopegraph;

import org.immutables.value.Value;

@Value.Immutable
public abstract class Label {

    @Value.Parameter public abstract String getName();

    @Override public String toString() {
        return getName();
    }

}