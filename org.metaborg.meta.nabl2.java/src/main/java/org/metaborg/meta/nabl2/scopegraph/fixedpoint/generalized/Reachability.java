package org.metaborg.meta.nabl2.scopegraph.fixedpoint.generalized;

import java.util.Set;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IScope;

public interface Reachability<S extends IScope, L extends ILabel> {

    Set<ScopePath<S, L>> all();

    Set<ScopePath<S, L>> from(S scope);

    Set<ScopePath<S, L>> to(S scope);

    interface Mutable<S extends IScope, L extends ILabel> extends Reachability<S, L> {

        boolean add(ScopePath<S, L> path);

    }

}