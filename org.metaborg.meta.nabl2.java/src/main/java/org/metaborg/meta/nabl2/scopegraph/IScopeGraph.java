package org.metaborg.meta.nabl2.scopegraph;

import org.metaborg.meta.nabl2.regexp.IRegExp;
import org.metaborg.meta.nabl2.transitiveclosure.TransitiveClosure;

public interface IScopeGraph {

    Iterable<Scope> getScopes();

    Iterable<Occurrence> getDecls(Scope scope);

    Iterable<Occurrence> getRefs(Scope scope);

    INameResolution resolve(IRegExp<Label> wf, TransitiveClosure<Label> order);
    
}