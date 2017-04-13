package org.metaborg.meta.nabl2.scopegraph.path;

import java.util.Set;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;


public interface IResolutionPaths<S extends IScope, L extends ILabel, O extends IOccurrence> {

    IResolutionPaths<S, L, O> inverse();

    Set<IResolutionPath<S, L, O>> get(O occurrence);

}