package org.metaborg.meta.nabl2.scopegraph.esop;

import java.util.Map;

import org.metaborg.meta.nabl2.regexp.IRegExp;
import org.metaborg.meta.nabl2.scopegraph.*;
import org.metaborg.meta.nabl2.transitiveclosure.TransitiveClosure;

import com.google.common.collect.*;

public class EsopNameResolution implements INameResolution {

    private final EsopScopeGraph scopeGraph;
    private final IRegExp<Label> wf;
    private final TransitiveClosure<Label> order;

    private final Multimap<Occurrence, Occurrence> res;
    private final Map<Scope, Multimap<SpacedName, Occurrence>> envs;

    public EsopNameResolution(EsopScopeGraph scopeGraph, IRegExp<Label> wf, TransitiveClosure<Label> order) {
        this.scopeGraph = scopeGraph;
        this.wf = wf;
        this.order = order;
        this.envs = Maps.newHashMap();
        this.res = HashMultimap.create();
        resolve();
    }

    private void resolve() {

    }

}