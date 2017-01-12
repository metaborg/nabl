package org.metaborg.meta.nabl2.scopegraph.esop;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IResolutionParameters;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.IScopeGraph;
import org.metaborg.meta.nabl2.util.functions.PartialFunction0;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

public class EsopScopeGraph<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements IScopeGraph<S,L,O>, Serializable {

    private static final long serialVersionUID = 42L;

    private final Set<S> scopes;
    private final Multimap<S,O> decls;
    private final Map<S,Multimap<L,PartialFunction0<S>>> directEdges;
    private final Map<S,Multimap<L,PartialFunction0<O>>> importRefs;
    private final Multimap<S,O> refs;
    private final Map<O,Multimap<L,S>> assocScopes;

    transient private Map<O,S> declScopesIndex;
    transient private Map<O,S> refScopesIndex;

    public EsopScopeGraph() {
        this.scopes = Sets.newHashSet();
        this.decls = HashMultimap.create();
        this.directEdges = Maps.newHashMap();
        this.assocScopes = Maps.newHashMap();
        this.importRefs = Maps.newHashMap();
        this.refs = HashMultimap.create();
        initIndices();
    }

    private void initIndices() {
        this.refScopesIndex = Maps.newHashMap();
        this.declScopesIndex = Maps.newHashMap();
    }

    @Override public Iterable<S> getAllScopes() {
        return scopes;
    }

    @Override public Iterable<O> getAllDecls() {
        return declScopesIndex.keySet();
    }

    @Override public Iterable<O> getAllRefs() {
        return refScopesIndex.keySet();
    }


    @Override public Iterable<O> getDecls(S scope) {
        return decls.containsKey(scope) ? decls.get(scope) : Iterables2.empty();
    }

    public void addDecl(S scope, O decl) {
        scopes.add(scope);
        decls.put(scope, decl);
        declScopesIndex.put(decl, scope);
    }

    @Override public Iterable<O> getRefs(S scope) {
        return refs.containsKey(scope) ? refs.get(scope) : Iterables2.empty();
    }

    public void addRef(O ref, S scope) {
        scopes.add(scope);
        refs.put(scope, ref);
        refScopesIndex.put(ref, scope);
    }

    public void addDirectEdge(S sourceScope, L label, PartialFunction0<S> targetScope) {
        scopes.add(sourceScope);
        directEdges.computeIfAbsent(sourceScope, s -> HashMultimap.create()).put(label, targetScope);
    }

    @Override public Multimap<L,PartialFunction0<S>> getDirectEdges(S scope) {
        return Multimaps.unmodifiableMultimap(directEdges.computeIfAbsent(scope, s -> HashMultimap.create()));
    }

    public void addAssoc(O decl, L label, S scope) {
        scopes.add(scope);
        assocScopes.computeIfAbsent(decl, s -> HashMultimap.create()).put(label, scope);
    }

    public void addImport(S scope, L label, PartialFunction0<O> ref) {
        scopes.add(scope);
        importRefs.computeIfAbsent(scope, s -> HashMultimap.create()).put(label, ref);
    }

    @Override public Multimap<L,PartialFunction0<O>> getImportRefs(S scope) {
        return Multimaps.unmodifiableMultimap(importRefs.computeIfAbsent(scope, s -> HashMultimap.create()));
    }


    @Override public Optional<S> getDeclScope(O decl) {
        return declScopesIndex.containsKey(decl) ? Optional.of(declScopesIndex.get(decl)) : Optional.empty();
    }

    @Override public Multimap<L,S> getAssocScopes(O decl) {
        return Multimaps.unmodifiableMultimap(assocScopes.computeIfAbsent(decl, s -> HashMultimap.create()));
    }


    @Override public Optional<S> getRefScope(O ref) {
        return refScopesIndex.containsKey(ref) ? Optional.of(refScopesIndex.get(ref)) : Optional.empty();
    }

    EsopNameResolution<S,L,O> resolve(IResolutionParameters<L> params) {
        return new EsopNameResolution<S,L,O>(this, params);
    }

    // serialization

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        initIndices();
        for(Map.Entry<S,O> entry : decls.entries()) {
            declScopesIndex.put(entry.getValue(), entry.getKey());
        }
        for(Map.Entry<S,O> entry : refs.entries()) {
            refScopesIndex.put(entry.getValue(), entry.getKey());
        }
    }

}