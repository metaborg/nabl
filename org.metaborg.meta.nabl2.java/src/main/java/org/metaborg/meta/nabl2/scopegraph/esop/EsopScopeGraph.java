package org.metaborg.meta.nabl2.scopegraph.esop;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.metaborg.meta.nabl2.functions.PartialFunction0;
import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IResolutionParameters;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.IScopeGraph;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class EsopScopeGraph<S extends IScope, L extends ILabel, O extends IOccurrence> implements IScopeGraph<S,L,O> {

    private final Set<S> scopes;
    private final Multimap<S,O> decls;
    private final Map<S,Multimap<L,PartialFunction0<S>>> directEdges;
    private final Map<O,Multimap<L,S>> assocs;
    private final Map<S,Multimap<L,PartialFunction0<O>>> imports;
    private final Multimap<S,O> refs;

    private final Set<O> declsIndex;
    private final Set<O> refsIndex;
    private final Map<O,S> refScopesIndex;

    public EsopScopeGraph() {
        this.scopes = Sets.newHashSet();
        this.decls = HashMultimap.create();
        this.directEdges = Maps.newHashMap();
        this.assocs = Maps.newHashMap();
        this.imports = Maps.newHashMap();
        this.refs = HashMultimap.create();

        this.declsIndex = Sets.newHashSet();
        this.refsIndex = Sets.newHashSet();
        this.refScopesIndex = Maps.newHashMap();
    }

    @Override public Iterable<S> getAllScopes() {
        return scopes;
    }

    @Override public Iterable<O> getAllDecls() {
        return declsIndex;
    }

    @Override public Iterable<O> getAllRefs() {
        return refsIndex;
    }

    @Override public Iterable<O> getDecls(S scope) {
        return decls.containsKey(scope) ? decls.get(scope) : Iterables2.empty();
    }

    public void addDecl(S scope, O decl) {
        scopes.add(scope);
        decls.put(scope, decl);
        declsIndex.add(decl);
    }

    @Override public Iterable<O> getRefs(S scope) {
        return refs.containsKey(scope) ? refs.get(scope) : Iterables2.empty();
    }

    public void addRef(O ref, S scope) {
        scopes.add(scope);
        refs.put(scope, ref);
        refScopesIndex.put(ref, scope);
        refsIndex.add(ref);
    }

    public void addDirectEdge(S sourceScope, L label, PartialFunction0<S> targetScope) {
        directEdges.computeIfAbsent(sourceScope, s -> HashMultimap.create()).put(label, targetScope);
    }

    @Override public Iterable<PartialFunction0<S>> getDirectEdges(S scope, L label) {
        return directEdges.containsKey(scope) ? directEdges.get(scope).get(label) : Iterables2.empty();
    }

    public void addAssoc(O decl, L label, S scope) {
        assocs.computeIfAbsent(decl, s -> HashMultimap.create()).put(label, scope);
    }

    @Override public Iterable<S> getAssocs(O decl, L label) {
        return assocs.containsKey(decl) ? assocs.get(decl).get(label) : Iterables2.empty();
    }

    public void addImport(S scope, L label, PartialFunction0<O> ref) {
        imports.computeIfAbsent(scope, s -> HashMultimap.create()).put(label, ref);
    }

    @Override public Iterable<PartialFunction0<O>> getImports(S scope, L label) {
        return imports.containsKey(scope) ? imports.get(scope).get(label) : Iterables2.empty();
    }

    EsopNameResolution<S,L,O> resolve(IResolutionParameters<L> params) {
        return new EsopNameResolution<S,L,O>(this, params);
    }

    Optional<S> getRefScope(O ref) {
        return refScopesIndex.containsKey(ref) ? Optional.of(refScopesIndex.get(ref)) : Optional.empty();
    }

}