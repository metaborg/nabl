package org.metaborg.meta.nabl2.solver_new;

import java.io.Serializable;

import org.metaborg.meta.nabl2.scopegraph.IActiveScopes;
import org.metaborg.meta.nabl2.scopegraph.IResolutionParameters;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.util.collections.IFunction;
import org.metaborg.meta.nabl2.util.collections.IRelation3;
import org.metaborg.meta.nabl2.util.functions.Function1;

import io.usethesource.capsule.Set;

public class IncompleteScopeGraph implements IIncompleteScopeGraph<Scope, Label, Occurrence>, Serializable {
    private static final long serialVersionUID = 42L;

    private final IEsopScopeGraph<Scope, Label, Occurrence> scopeGraph;
    private final IRelation3<Scope, Label, ITerm> incompleteDirectEdges;
    private final IRelation3<Scope, Label, ITerm> incompleteImportEdges;

    public IncompleteScopeGraph(IEsopScopeGraph<Scope, Label, Occurrence> scopeGraph,
            IRelation3<Scope, Label, ITerm> incompleteDirectEdges,
            IRelation3<Scope, Label, ITerm> incompleteImportEdges) {
        this.scopeGraph = scopeGraph;
        this.incompleteDirectEdges = incompleteDirectEdges;
        this.incompleteImportEdges = incompleteImportEdges;
    }

    // --- delegate scope graph ---

    @Override public Set.Immutable<Scope> getAllScopes() {
        return scopeGraph.getAllScopes();
    }

    @Override public Set.Immutable<Occurrence> getAllDecls() {
        return scopeGraph.getAllDecls();
    }

    @Override public Set.Immutable<Occurrence> getAllRefs() {
        return scopeGraph.getAllRefs();
    }

    @Override public IFunction<Occurrence, Scope> getDecls() {
        return scopeGraph.getDecls();
    }

    @Override public IFunction<Occurrence, Scope> getRefs() {
        return scopeGraph.getRefs();
    }

    @Override public IRelation3<Scope, Label, Scope> getDirectEdges() {
        return scopeGraph.getDirectEdges();
    }

    @Override public IRelation3<Occurrence, Label, Scope> getExportEdges() {
        return scopeGraph.getExportEdges();
    }

    @Override public IRelation3<Scope, Label, Occurrence> getImportEdges() {
        return scopeGraph.getImportEdges();
    }

    @Override public IEsopNameResolution<Scope, Label, Occurrence> resolve(IResolutionParameters<Label> params,
            IActiveScopes<Scope, Label> scopeCounter, Function1<Scope, String> tracer) {
        return scopeGraph.resolve(params, scopeCounter, tracer);
    }

    // --- incomplete edges ---

    @Override public IRelation3<Scope, Label, ITerm> incompleteDirectEdges() {
        return incompleteDirectEdges;
    }

    @Override public IRelation3<Scope, Label, ITerm> incompleteImportEdges() {
        return incompleteImportEdges;
    }

}