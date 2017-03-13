package org.metaborg.meta.nabl2.scopegraph.esop;

import java.io.Serializable;
import java.util.Set;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IResolutionParameters;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.IScopeGraph;
import org.metaborg.meta.nabl2.util.collections.BagMultimap;
import org.metaborg.meta.nabl2.util.collections.HashBagMultimap;
import org.metaborg.meta.nabl2.util.collections.HashFunction;
import org.metaborg.meta.nabl2.util.collections.HashRelation3;
import org.metaborg.meta.nabl2.util.collections.HashSet;
import org.metaborg.meta.nabl2.util.collections.IFunction;
import org.metaborg.meta.nabl2.util.collections.IRelation3;
import org.metaborg.meta.nabl2.util.collections.ISet;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

public class EsopScopeGraph<S extends IScope, L extends ILabel, O extends IOccurrence>
    implements IScopeGraph<S, L, O>, Serializable {

    private static final long serialVersionUID = 42L;

    private final ISet.Mutable<S> allScopes;
    private final ISet.Mutable<O> allDecls;
    private final ISet.Mutable<O> allRefs;

    private final IFunction.Mutable<O, S> decls;
    private final IFunction.Mutable<O, S> refs;
    private final IRelation3.Mutable<S, L, S> directEdges;
    private final IRelation3.Mutable<O, L, S> assocEdges;
    private final IRelation3.Mutable<S, L, O> importEdges;

    private final Multiset<S> activeScopes;
    private final Set<S> frozenScopes;
    private final BagMultimap<S, L> activeEdges;
    private final Multimap<S, L> frozenEdges;
    private final Multimap<O, L> frozenAssocs;

    public EsopScopeGraph() {
        this.allScopes = HashSet.create();
        this.allDecls = HashSet.create();
        this.allRefs = HashSet.create();

        this.decls = HashFunction.create();
        this.refs = HashFunction.create();
        this.directEdges = HashRelation3.create();
        this.assocEdges = HashRelation3.create();
        this.importEdges = HashRelation3.create();

        this.activeScopes = HashMultiset.create();
        this.frozenScopes = Sets.newHashSet();
        this.activeEdges = HashBagMultimap.create();
        this.frozenEdges = HashMultimap.create();
        this.frozenAssocs = HashMultimap.create();
    }

    // -----------------------

    @Override public ISet<S> getAllScopes() {
        return allScopes;
    }

    @Override public ISet<O> getAllDecls() {
        return allDecls;
    }

    @Override public ISet<O> getAllRefs() {
        return allRefs;
    }


    @Override public IFunction<O, S> getDecls() {
        return decls;
    }

    @Override public IFunction<O, S> getRefs() {
        return refs;
    }

    @Override public IRelation3<S, L, S> getDirectEdges() {
        return directEdges;
    }

    @Override public IRelation3<O, L, S> getAssocEdges() {
        return assocEdges;
    }

    @Override public IRelation3<S, L, O> getImportEdges() {
        return importEdges;
    }

    // -----------------------

    public void addDecl(S scope, O decl) {
        throwIfScopeFrozen(scope);
        allScopes.add(scope);
        allDecls.add(decl);
        decls.put(decl, scope);
    }

    public void addRef(O ref, S scope) {
        throwIfScopeFrozen(scope);
        allScopes.add(scope);
        allRefs.add(ref);
        refs.put(ref, scope);
    }

    public void addDirectEdge(S sourceScope, L label, S targetScope) {
        throwIfScopeFrozen(sourceScope);
        throwIfEdgeFrozen(sourceScope, label);
        allScopes.add(sourceScope);
        directEdges.put(sourceScope, label, targetScope);
    }

    public void addAssoc(O decl, L label, S scope) {
        throwIfEdgeFrozen(decl, label);
        allScopes.add(scope);
        allDecls.add(decl);
        assocEdges.put(decl, label, scope);
    }

    public void addImport(S scope, L label, O ref) {
        throwIfScopeFrozen(scope);
        throwIfEdgeFrozen(scope, label);
        allScopes.add(scope);
        allRefs.add(ref);
        importEdges.put(scope, label, ref);
    }

    // ------------------------------------

    EsopNameResolution<S, L, O> resolve(IResolutionParameters<L> params) {
        return new EsopNameResolution<S, L, O>(this, params);
    }

    // active scopes

    public void addActiveScope(S scope) {
        if(frozenScopes.contains(scope)) {
            throw new IllegalArgumentException("Re-activating frozen " + scope);
        }
        activeScopes.add(scope);
    }

    public void removeActiveScope(S scope) {
        activeScopes.remove(scope);
    }

    public boolean isScopeActive(S scope) {
        return activeScopes.contains(scope);
    }

    public void throwIfScopeFrozen(S scope) throws IllegalArgumentException {
        if(frozenScopes.contains(scope)) {
            throw new IllegalArgumentException("Modifying frozen " + scope);
        }
    }

    public void freezeScope(S scope) {
        if(activeScopes.contains(scope)) {
            throw new IllegalArgumentException("Freezing active " + scope);
        }
        frozenScopes.add(scope);
    }

    // active edges

    public void addActiveEdge(S scope, L label) {
        if(frozenEdges.containsEntry(scope, label)) {
            throw new IllegalArgumentException("Re-activating frozen " + scope + "/" + label);
        }
        activeEdges.put(scope, label);
    }

    public void removeActiveEdge(S scope, L label) {
        activeEdges.remove(scope, label);

    }

    public boolean isEdgeActive(S scope, L label) {
        return activeEdges.get(scope).contains(label);
    }

    public void throwIfEdgeFrozen(S scope, L label) throws IllegalArgumentException {
        if(frozenEdges.containsEntry(scope, label)) {
            throw new IllegalArgumentException("Modifying frozen " + scope + "/" + label);
        }
    }

    public void freezeEdge(S scope, L label) {
        if(activeEdges.containsEntry(scope, label)) {
            throw new IllegalArgumentException("Freezing active " + scope + "/" + label);
        }
        frozenEdges.put(scope, label);
    }

    public void throwIfEdgeFrozen(O decl, L label) throws IllegalArgumentException {
        if(frozenAssocs.containsEntry(decl, label)) {
            throw new IllegalArgumentException("Modifying frozen " + decl + "/" + label);
        }
    }

    public void freezeEdge(O decl, L label) {
        frozenAssocs.put(decl, label);
    }

}