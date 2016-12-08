package org.metaborg.meta.nabl2.scopegraph.esop;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.metaborg.meta.nabl2.regexp.IAlphabet;
import org.metaborg.meta.nabl2.regexp.IRegExp;
import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.IScopeGraph;
import org.metaborg.meta.nabl2.terms.ITermIndex;
import org.metaborg.meta.nabl2.transitiveclosure.TransitiveClosure;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class EsopScopeGraph<S extends IScope, L extends ILabel, O extends IOccurrence> implements IScopeGraph<S,L,O> {

    private final Set<S> scopes;
    private final Multimap<S,O> decls;
    private final Map<S,Multimap<L,S>> directEdges;
    private final Map<O,Multimap<L,S>> assocs;
    private final Map<S,Multimap<L,O>> imports;
    private final Multimap<S,O> refs;

    private final Map<O,S> refScopesIndex;
    private final Multimap<ITermIndex,O> astRefsIndex;

    public EsopScopeGraph() {
        this.scopes = Sets.newHashSet();
        this.decls = HashMultimap.create();
        this.directEdges = Maps.newHashMap();
        this.assocs = Maps.newHashMap();
        this.imports = Maps.newHashMap();
        this.refs = HashMultimap.create();

        this.refScopesIndex = Maps.newHashMap();
        this.astRefsIndex = HashMultimap.create();
    }

    @Override public Iterable<S> getAllScopes() {
        return scopes;
    }

    @Override public Iterable<O> getDecls(S scope) {
        return decls.containsKey(scope) ? decls.get(scope) : Iterables2.empty();
    }

    public void addDecl(S scope, O decl) {
        scopes.add(scope);
        decls.put(scope, decl);
    }

    @Override public Iterable<O> getRefs(S scope) {
        return refs.containsKey(scope) ? refs.get(scope) : Iterables2.empty();
    }

    public void addRef(O ref, S scope) {
        scopes.add(scope);
        refs.put(scope, ref);
        refScopesIndex.put(ref, scope);
        astRefsIndex.put(ref.getPosition(), ref);
    }

    public void addDirectEdge(S sourceScope, L label, S targetScope) {
        directEdges.computeIfAbsent(sourceScope, s -> HashMultimap.create()).put(label, targetScope);
    }

    @Override public Iterable<S> getDirectEdges(S scope, L label) {
        return directEdges.containsKey(scope) ? directEdges.get(scope).get(label) : Iterables2.empty();
    }

    public void addAssoc(O decl, L label, S scope) {
        assocs.computeIfAbsent(decl, s -> HashMultimap.create()).put(label, scope);
    }

    @Override public Iterable<S> getAssocs(O decl, L label) {
        return assocs.containsKey(decl) ? assocs.get(decl).get(label) : Iterables2.empty();
    }

    public void addImport(S scope, L label, O ref) {
        imports.computeIfAbsent(scope, s -> HashMultimap.create()).put(label, ref);
    }

    @Override public Iterable<O> getImports(S scope, L label) {
        return imports.containsKey(scope) ? imports.get(scope).get(label) : Iterables2.empty();
    }

    EsopNameResolution<S,L,O> resolve(IAlphabet<L> labels, IRegExp<L> wf, TransitiveClosure<L> order) {
        return new EsopNameResolution<S,L,O>(this, labels, wf, order);
    }

    Optional<S> getRefScope(O ref) {
        return refScopesIndex.containsKey(ref) ? Optional.of(refScopesIndex.get(ref)) : Optional.empty();
    }

    @Override public Iterable<O> getAstRefs(ITermIndex index) {
        return astRefsIndex.get(index);
    }

}