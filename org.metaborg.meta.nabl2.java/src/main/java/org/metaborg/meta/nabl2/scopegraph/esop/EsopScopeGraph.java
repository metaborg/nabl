package org.metaborg.meta.nabl2.scopegraph.esop;

import java.io.Serializable;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IResolutionParameters;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.IScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.OpenCounter;
import org.metaborg.meta.nabl2.util.collections.HashFunction;
import org.metaborg.meta.nabl2.util.collections.HashRelation3;
import org.metaborg.meta.nabl2.util.collections.HashSet;
import org.metaborg.meta.nabl2.util.collections.IFunction;
import org.metaborg.meta.nabl2.util.collections.IRelation3;
import org.metaborg.meta.nabl2.util.collections.ISet;

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

    public EsopScopeGraph() {
        this.allScopes = HashSet.create();
        this.allDecls = HashSet.create();
        this.allRefs = HashSet.create();

        this.decls = HashFunction.create();
        this.refs = HashFunction.create();
        this.directEdges = HashRelation3.create();
        this.assocEdges = HashRelation3.create();
        this.importEdges = HashRelation3.create();
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

    @Override public IRelation3<O, L, S> getExportEdges() {
        return assocEdges;
    }

    @Override public IRelation3<S, L, O> getImportEdges() {
        return importEdges;
    }

    // -----------------------

    public void addDecl(S scope, O decl) {
        // FIXME: check scope/D is not closed
        allScopes.add(scope);
        allDecls.add(decl);
        decls.put(decl, scope);
    }

    public void addRef(O ref, S scope) {
        // FIXME: check scope/R is not closed
        allScopes.add(scope);
        allRefs.add(ref);
        refs.put(ref, scope);
    }

    public void addDirectEdge(S sourceScope, L label, S targetScope) {
        // FIXME: check scope/l is not closed
        allScopes.add(sourceScope);
        directEdges.put(sourceScope, label, targetScope);
    }

    public void addAssoc(O decl, L label, S scope) {
        // FIXME: check decl/l is not closed
        allScopes.add(scope);
        allDecls.add(decl);
        assocEdges.put(decl, label, scope);
    }

    public void addImport(S scope, L label, O ref) {
        // FIXME: check scope/l is not closed
        allScopes.add(scope);
        allRefs.add(ref);
        importEdges.put(scope, label, ref);
    }

    // ------------------------------------

    EsopNameResolution<S, L, O> resolve(IResolutionParameters<L> params, OpenCounter<S, L> scopeCounter) {
        return new EsopNameResolution<>(this, params, scopeCounter);
    }

}