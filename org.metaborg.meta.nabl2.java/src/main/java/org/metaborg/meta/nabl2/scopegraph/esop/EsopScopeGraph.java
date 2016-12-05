package org.metaborg.meta.nabl2.scopegraph.esop;

import java.util.Map;
import java.util.Set;

import org.metaborg.meta.nabl2.regexp.IRegExp;
import org.metaborg.meta.nabl2.scopegraph.*;
import org.metaborg.meta.nabl2.transitiveclosure.TransitiveClosure;

import com.google.common.collect.*;

public class EsopScopeGraph implements IScopeGraph {

    private final Set<Scope> scopes;
    private final Multimap<Scope,Occurrence> decls;
    private final Map<Scope,Multimap<Label,Scope>> directEdges;
    private final Multimap<Scope,Occurrence> refs;

    public EsopScopeGraph() {
        this.scopes = Sets.newHashSet();
        this.decls = HashMultimap.create();
        this.directEdges = Maps.newHashMap();
        this.refs = HashMultimap.create();
    }

    @Override public Iterable<Scope> getScopes() {
        return scopes;
    }

    @Override public Iterable<Occurrence> getDecls(Scope scope) {
        return decls.get(scope);
    }

    public void addDecl(Scope scope, Occurrence decl) {
        scopes.add(scope);
        decls.put(scope, decl);
    }

    @Override public Iterable<Occurrence> getRefs(Scope scope) {
        return refs.get(scope);
    }

    public void addRef(Occurrence ref, Scope scope) {
        scopes.add(scope);
        refs.put(scope, ref);
    }

    public void addDirectEdge(Scope sourceScope, Label label, Scope targetScope) {
        directEdges.computeIfAbsent(sourceScope, s -> HashMultimap.create()).put(label, targetScope);
    }

    @Override public EsopNameResolution resolve(IRegExp<Label> wf, TransitiveClosure<Label> order) {
        return new EsopNameResolution(this, wf, order);
    }

}