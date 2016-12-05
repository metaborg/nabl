package org.metaborg.meta.nabl2.scopegraph;

import java.util.Optional;

import org.immutables.value.Value;
import org.metaborg.meta.nabl2.terms.ITerm;

@Value.Immutable
public abstract class SpacedName {

    @Value.Parameter public abstract Optional<String> getNamespace();

    @Value.Parameter public abstract ITerm getName();

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        getNamespace().ifPresent(ns -> sb.append(ns));
        sb.append('{');
        sb.append(getName());
        sb.append('}');
        return sb.toString();
    }

}