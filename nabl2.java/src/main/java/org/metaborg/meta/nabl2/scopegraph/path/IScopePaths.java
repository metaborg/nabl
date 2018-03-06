package org.metaborg.meta.nabl2.scopegraph.path;

import java.util.Set;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;


public interface IScopePaths<S extends IScope, L extends ILabel, O extends IOccurrence> {

    IScopePaths<S, L, O> inverse();

    Set<IScopePath<S, L, O>> get(S scope);

}