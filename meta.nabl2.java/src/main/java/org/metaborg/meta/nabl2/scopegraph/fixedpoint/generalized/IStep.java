package org.metaborg.meta.nabl2.scopegraph.fixedpoint.generalized;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IScope;

public interface IStep<S, L> {

    S getSource();

    L getLabel();

    S getTarget();

    static <S extends IScope, L extends ILabel> IStep<S, L> of(S source, L label, S target) throws PathException {
        return null;
    }

    static <S extends IScope, L extends ILabel> IStep<S, L> of(S source, L label, ResolutionPath<S, L, ?, ?> path,
        S target) throws PathException {
        return null;
    }

}