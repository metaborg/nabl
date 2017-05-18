package org.metaborg.meta.nabl2.solver_new.components;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.scopegraph.CGDecl;
import org.metaborg.meta.nabl2.constraints.scopegraph.CGDirectEdge;
import org.metaborg.meta.nabl2.constraints.scopegraph.CGExportEdge;
import org.metaborg.meta.nabl2.constraints.scopegraph.CGImportEdge;
import org.metaborg.meta.nabl2.constraints.scopegraph.CGRef;
import org.metaborg.meta.nabl2.constraints.scopegraph.IScopeGraphConstraint;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.solver.TypeException;
import org.metaborg.meta.nabl2.solver_new.ASolver;
import org.metaborg.meta.nabl2.solver_new.IIncompleteScopeGraph;
import org.metaborg.meta.nabl2.solver_new.IncompleteScopeGraph;
import org.metaborg.meta.nabl2.solver_new.SolverCore;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.util.collections.HashRelation3;
import org.metaborg.meta.nabl2.util.collections.IRelation3;

import com.google.common.collect.Sets;

public class ScopeGraphSolver extends ASolver<IScopeGraphConstraint, ScopeGraphSolver.ScopeGraphResult> {

    private final java.util.Set<IScopeGraphConstraint> constraints;
    private final IEsopScopeGraph.Builder<Scope, Label, Occurrence> scopeGraph;
    private final IRelation3.Mutable<Scope, Label, ITerm> incompleteDirectEdges;
    private final IRelation3.Mutable<Scope, Label, ITerm> incompleteImportEdges;

    public ScopeGraphSolver(SolverCore core) {
        super(core);
        this.constraints = Sets.newHashSet();
        this.scopeGraph = IEsopScopeGraph.builder();
        this.incompleteDirectEdges = HashRelation3.create();
        this.incompleteImportEdges = HashRelation3.create();
    }

    // ------------------------------------------------------------------------------------------------------//

    @Override public boolean add(IScopeGraphConstraint constraint) throws InterruptedException {
        if(!solve(constraint)) {
            return constraints.add(constraint);
        } else {
            work();
            return true;
        }
    }

    @Override public boolean iterate() throws InterruptedException {
        boolean progress = false;
        progress |= doIterate(constraints, this::solve);
        progress |= doIterate(incompleteDirectEdges, this::solveDirectEdge);
        progress |= doIterate(incompleteImportEdges, this::solveImportEdge);
        return progress;
    }

    @Override public ScopeGraphResult finish() {
        return ImmutableScopeGraphResult.of(
                new IncompleteScopeGraph(scopeGraph.result(), incompleteDirectEdges, incompleteImportEdges),
                constraints);
    }

    // ------------------------------------------------------------------------------------------------------//

    private boolean solve(IScopeGraphConstraint constraint) {
        return constraint
                .match(IScopeGraphConstraint.Cases.of(this::solve, this::solve, this::solve, this::solve, this::solve));
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
        scopeGraph.addDecl(scope, decl);
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
        scopeGraph.addRef(ref, scope);
        return true;
    }

    private boolean solve(CGDirectEdge<?> c) {
        ITerm sourceScopeTerm = find(c.getSourceScope());
        if(!sourceScopeTerm.isGround()) {
            return false;
        }
        Scope sourceScope = Scope.matcher().match(sourceScopeTerm)
                .orElseThrow(() -> new TypeException("Expected a scope as first argument to " + c));
        if(!solveDirectEdge(sourceScope, c.getLabel(), c.getTargetScope())) {
            incompleteDirectEdges.put(sourceScope, c.getLabel(), c.getTargetScope());
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
        if(!solveImportEdge(scope, c.getLabel(), c.getReference())) {
            incompleteImportEdges.put(scope, c.getLabel(), c.getReference());

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
        scopeGraph.addAssoc(decl, c.getLabel(), scope);
        return true;
    }


    private boolean solveDirectEdge(Scope sourceScope, Label label, ITerm targetScopeTerm) {
        ITerm targetScopeRep = find(targetScopeTerm);
        if(!targetScopeRep.isGround()) {
            return false;
        }
        Scope targetScope = Scope.matcher().match(targetScopeRep)
                .orElseThrow(() -> new TypeException("Expected a scope but got " + targetScopeRep));
        scopeGraph.addDirectEdge(sourceScope, label, targetScope);
        return true;
    }

    private boolean solveImportEdge(Scope scope, Label label, ITerm refTerm) {
        ITerm refRep = find(refTerm);
        if(!refRep.isGround()) {
            return false;
        }
        Occurrence ref = Occurrence.matcher().match(refRep)
                .orElseThrow(() -> new TypeException("Expected an occurrence, but got " + refRep));
        scopeGraph.addImport(scope, label, ref);
        return true;
    }

    // ------------------------------------------------------------------------------------------------------//

    @Value.Immutable
    @Serial.Version(42L)
    public static abstract class ScopeGraphResult {

        @Value.Parameter public abstract IIncompleteScopeGraph<Scope, Label, Occurrence> scopeGraph();

        @Value.Parameter public abstract java.util.Set<IScopeGraphConstraint> residualConstraints();

    }

}