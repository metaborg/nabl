package org.metaborg.meta.nabl2.scopegraph.terms;

import org.immutables.value.Value;
import org.metaborg.meta.nabl2.scopegraph.INamespace;
import org.metaborg.meta.nabl2.terms.ITerm;

@Value.Immutable
public abstract class SpacedName {

    @Value.Parameter public abstract INamespace getNamespace();

    @Value.Parameter public abstract ITerm getName();

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        getNamespace().getName().ifPresent(ns -> sb.append(ns));
        sb.append('{');
        sb.append(getName());
        sb.append('}');
        return sb.toString();
    }

}