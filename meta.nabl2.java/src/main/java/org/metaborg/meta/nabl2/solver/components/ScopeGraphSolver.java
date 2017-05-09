package org.metaborg.meta.nabl2.solver.components;

import static org.metaborg.meta.nabl2.util.Unit.unit;

import java.util.Map;
import java.util.Set;

import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.scopegraph.CGDecl;
import org.metaborg.meta.nabl2.constraints.scopegraph.CGDirectEdge;
import org.metaborg.meta.nabl2.constraints.scopegraph.CGExportEdge;
import org.metaborg.meta.nabl2.constraints.scopegraph.CGImportEdge;
import org.metaborg.meta.nabl2.constraints.scopegraph.CGRef;
import org.metaborg.meta.nabl2.constraints.scopegraph.IScopeGraphConstraint;
import org.metaborg.meta.nabl2.constraints.scopegraph.IScopeGraphConstraint.CheckedCases;
import org.metaborg.meta.nabl2.constraints.scopegraph.ImmutableCGDecl;
import org.metaborg.meta.nabl2.constraints.scopegraph.ImmutableCGDirectEdge;
import org.metaborg.meta.nabl2.constraints.scopegraph.ImmutableCGExportEdge;
import org.metaborg.meta.nabl2.constraints.scopegraph.ImmutableCGImportEdge;
import org.metaborg.meta.nabl2.constraints.scopegraph.ImmutableCGRef;
import org.metaborg.meta.nabl2.scopegraph.IScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.OpenCounter;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.ResolutionParameters;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.solver.Solver;
import org.metaborg.meta.nabl2.solver.SolverComponent;
import org.metaborg.meta.nabl2.solver.TypeException;
import org.metaborg.meta.nabl2.solver.UnsatisfiableException;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.util.Unit;

import com.google.common.collect.Sets;

public class ScopeGraphSolver extends SolverComponent<IScopeGraphConstraint> {

    private final ResolutionParameters params;
    private final IEsopScopeGraph.Builder<Scope, Label, Occurrence> scopeGraphBuilder;
    private final OpenCounter<Scope, Label> scopeCounter;

    private final Set<IScopeGraphConstraint> unsolved;
    private final Set<CGDirectEdge<Scope>> incompleteDirectEdges;
    private final Set<CGImportEdge<Scope>> incompleteImportEdges;

    public ScopeGraphSolver(Solver solver, ResolutionParameters params, IEsopScopeGraph.Builder<Scope, Label, Occurrence> scopeGraph, OpenCounter<Scope, Label> scopeCounter) {
        super(solver);
        this.params = params;
        this.scopeGraphBuilder = scopeGraph;
        this.scopeCounter = scopeCounter;

        this.unsolved = Sets.newHashSet();
        this.incompleteDirectEdges = Sets.newHashSet();
        this.incompleteImportEdges = Sets.newHashSet();
    }

    public IScopeGraph<Scope, Label, Occurrence> getScopeGraph() {
		return scopeGraphBuilder.result();
    }

    public void addActive(Iterable<Scope> scopes) {
        for(Scope scope : scopes) {
            scopeCounter.addAll(scope, params.getLabels().symbols());
        }
    }

    // ------------------------------------------------------------------------------------------------------//

    @Override protected Unit doAdd(IScopeGraphConstraint constraint) throws UnsatisfiableException {
        if(!solve(constraint)) {
            unsolved.add(constraint);
        } else {
            work();
        }
        return unit;
    }

    @Override protected boolean doIterate() throws UnsatisfiableException, InterruptedException {
        boolean progress = false;
        progress |= doIterate(unsolved, this::solve);
        progress |= doIterate(incompleteDirectEdges, this::solveDirectEdge);
        progress |= doIterate(incompleteImportEdges, this::solveImportEdge);
        if(!scopeCounter.isComplete() && unsolved.isEmpty()) {
            progress |= true;
            scopeCounter.setComplete();
        }
        return progress;
    }

    @Override protected Set<? extends IScopeGraphConstraint> doFinish(IMessageInfo messageInfo) {
        Set<IScopeGraphConstraint> constraints = Sets.newHashSet();
        if(isPartial()) {
            addScopeGraphConstraints(constraints, messageInfo);
        }
        constraints.addAll(unsolved);
        constraints.addAll(incompleteDirectEdges);
        constraints.addAll(incompleteImportEdges);
        return constraints;
    }

    // ------------------------------------------------------------------------------------------------------//

    private boolean solve(IScopeGraphConstraint constraint) throws UnsatisfiableException {
        return constraint.matchOrThrow(CheckedCases.of(this::solve, this::solve, this::solve, this::solve, this::solve));
    }

    private boolean solve(CGDecl c) {
        ITerm scopeTerm = find(c.getScope());
        ITerm declTerm = find(c.getDeclaration());
        if(!(scopeTerm.isGround() && declTerm.isGround())) {
            return false;
        }
        Scope scope = Scope.matcher().match(scopeTerm)
                .orElseThrow(() -> new TypeException("Expected a scope as first agument to " + c));
        Occurrence decl = Occurrence.matcher().match(declTerm)
                .orElseThrow(() -> new TypeException("Expected an occurrence as second argument to " + c));
        scopeGraphBuilder.addDecl(scope, decl);
        return true;
    }

    private boolean solve(CGRef c) {
        ITerm scopeTerm = find(c.getScope());
        ITerm refTerm = find(c.getReference());
        if(!(scopeTerm.isGround() && refTerm.isGround())) {
            return false;
        }
        Occurrence ref = Occurrence.matcher().match(refTerm)
                .orElseThrow(() -> new TypeException("Expected an occurrence as first argument to " + c));
        Scope scope = Scope.matcher().match(scopeTerm)
                .orElseThrow(() -> new TypeException("Expected a scope as second argument to " + c));
        scopeGraphBuilder.addRef(ref, scope);
        return true;
    }

    private boolean solve(CGDirectEdge<?> c) {
        ITerm sourceScopeTerm = find(c.getSourceScope());
        if(!sourceScopeTerm.isGround()) {
            return false;
        }
        Scope sourceScope = Scope.matcher().match(sourceScopeTerm)
                .orElseThrow(() -> new TypeException("Expected a scope as first argument to " + c));
        scopeCounter.add(sourceScope, c.getLabel());
        CGDirectEdge<Scope> cc =
                ImmutableCGDirectEdge.of(sourceScope, c.getLabel(), c.getTargetScope(), c.getMessageInfo());
        if(!solveDirectEdge(cc)) {
            incompleteDirectEdges.add(cc);
        }
        return true;
    }

    private boolean solve(CGImportEdge<?> c) {
        ITerm scopeTerm = find(c.getScope());
        if(!scopeTerm.isGround()) {
            return false;
        }
        Scope scope = Scope.matcher().match(scopeTerm)
                .orElseThrow(() -> new TypeException("Expected a scope as first argument to " + c));
        scopeCounter.add(scope, c.getLabel());
        CGImportEdge<Scope> cc = ImmutableCGImportEdge.of(scope, c.getLabel(), c.getReference(), c.getMessageInfo());
        if(!solveImportEdge(cc)) {
            incompleteImportEdges.add(cc);

        }
        return true;
    }

    private boolean solve(CGExportEdge c) {
        ITerm scopeTerm = find(c.getScope());
        ITerm declTerm = find(c.getDeclaration());
        if(!(scopeTerm.isGround() && declTerm.isGround())) {
            return false;
        }
        Scope scope = Scope.matcher().match(scopeTerm)
                .orElseThrow(() -> new TypeException("Expected a scope as third argument to " + c));
        Occurrence decl = Occurrence.matcher().match(declTerm)
                .orElseThrow(() -> new TypeException("Expected an occurrence as first argument to " + c));
        scopeGraphBuilder.addAssoc(decl, c.getLabel(), scope);
        return true;
    }


    private boolean solveDirectEdge(CGDirectEdge<Scope> c) {
        ITerm targetScopeTerm = find(c.getTargetScope());
        if(!targetScopeTerm.isGround()) {
            return false;
        }
        Scope targetScope = Scope.matcher().match(targetScopeTerm)
                .orElseThrow(() -> new TypeException("Expected a scope as third argument to " + c));
        scopeGraphBuilder.addDirectEdge(c.getSourceScope(), c.getLabel(), targetScope);
        scopeCounter.remove(c.getSourceScope(), c.getLabel());
        return true;
    }

    private boolean solveImportEdge(CGImportEdge<Scope> c) {
        ITerm refTerm = find(c.getReference());
        if(!refTerm.isGround()) {
            return false;
        }
        Occurrence ref = Occurrence.matcher().match(refTerm)
                .orElseThrow(() -> new TypeException("Expected an occurrence as third argument to " + c));
        scopeGraphBuilder.addImport(c.getScope(), c.getLabel(), ref);
        scopeCounter.remove(c.getScope(), c.getLabel());
        return true;
    }

    // ------------------------------------------------------------------------------------------------------//

    private void addScopeGraphConstraints(Set<IScopeGraphConstraint> constraints, IMessageInfo messageInfo) {
        for(Scope scope : scopeGraphBuilder.getAllScopes()) {
            for(Occurrence decl : scopeGraphBuilder.getDecls().inverse().get(scope)) {
                constraints.add(ImmutableCGDecl.of(scope, decl, messageInfo));
            }
            for(Occurrence ref : scopeGraphBuilder.getRefs().inverse().get(scope)) {
                constraints.add(ImmutableCGRef.of(ref, scope, messageInfo));
            }
            for(Map.Entry<Label, Scope> edge : scopeGraphBuilder.getDirectEdges().get(scope)) {
                constraints.add(ImmutableCGDirectEdge.of(scope, edge.getKey(), edge.getValue(), messageInfo));
            }
            for(Map.Entry<Label, Occurrence> edge : scopeGraphBuilder.getImportEdges().get(scope)) {
                constraints.add(ImmutableCGImportEdge.of(scope, edge.getKey(), edge.getValue(), messageInfo));
            }
            for(Map.Entry<Label, Occurrence> edge : scopeGraphBuilder.getExportEdges().inverse().get(scope)) {
                constraints.add(ImmutableCGExportEdge.of(edge.getValue(), edge.getKey(), scope, messageInfo));
            }
        }
    }

}