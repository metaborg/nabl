package org.metaborg.meta.nabl2.scopegraph.esop.reference;

import java.io.Serializable;
import java.util.Optional;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.path.IDeclPath;
import org.metaborg.meta.nabl2.scopegraph.path.IPath;

import io.usethesource.capsule.Set;

public interface IEsopEnv<S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>>
        extends Serializable {

    Optional<Set.Immutable<P>> getAll();

    interface Filter<S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>>
            extends Serializable {

        Optional<P> test(IDeclPath<S, L, O> path);

        Object matchToken(P p);

        boolean shortCircuit();

    }

}