package org.metaborg.meta.nabl2.scopegraph.fixedpoint.generalized;

import java.util.List;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IScope;

public interface ScopePath<S extends IScope, L extends ILabel> extends IPath {

    S getSource();

    List<L> getLabels();

    S getTarget();

    ScopePath<S, L> append(ScopePath<S, L> next) throws PathException;

}