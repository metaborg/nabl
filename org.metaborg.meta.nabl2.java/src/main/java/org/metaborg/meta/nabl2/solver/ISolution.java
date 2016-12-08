package org.metaborg.meta.nabl2.solver;

import org.metaborg.meta.nabl2.scopegraph.INameResolution;
import org.metaborg.meta.nabl2.scopegraph.IScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.terms.ITerm;

import com.google.common.collect.Multimap;

public interface ISolution {

    Multimap<ITerm,String> getErrors();

    Multimap<ITerm,String> getWarnings();

    Multimap<ITerm,String> getNotes();

    IScopeGraph<Scope,Label,Occurrence> getScopeGraph();

    INameResolution<Scope,Label,Occurrence> getNameResolution();
    
}