package org.metaborg.meta.nabl2.solver.components;

import java.util.Optional;

import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
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
import org.metaborg.meta.nabl2.solver.ASolver;
import org.metaborg.meta.nabl2.solver.SolverCore;
import org.metaborg.meta.nabl2.solver.TypeException;
import org.metaborg.meta.nabl2.terms.ITerm;

public class ScopeGraphComponent
        extends ASolver<IScopeGraphConstraint, IEsopScopeGraph.Immutable<Scope, Label, Occurrence, ITerm>> {

    private final IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> scopeGraph;

    public ScopeGraphComponent(SolverCore core, IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> initial) {
        super(core);
        this.scopeGraph = initial;
    }

    // ------------------------------------------------------------------------------------------------------//

    @Override public org.metaborg.meta.nabl2.solver.ISolver.SeedResult
            seed(IEsopScopeGraph.Immutable<Scope, Label, Occurrence, ITerm> solution, IMessageInfo message)
                    throws InterruptedException {
        scopeGraph.addAll(solution);
        return SeedResult.empty();
    }

    @Override public Optional<SolveResult> solve(IScopeGraphConstraint constraint) {
        if(constraint.match(
                IScopeGraphConstraint.Cases.of(this::solve, this::solve, this::solve, this::solve, this::solve))) {
            return Optional.of(SolveResult.empty());
        } else {
            return Optional.empty();
        }
    }

    @Override public boolean update() throws InterruptedException {
        return scopeGraph.reduce(this::findScope, this::findOccurrence);
    }

    @Override public IEsopScopeGraph.Immutable<Scope, Label, Occurrence, ITerm> finish() {
        return scopeGraph.freeze();
    }

    // ------------------------------------------------------------------------------------------------------//

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

    private boolean solve(CGDirectEdge c) {
        ITerm sourceScopeRep = find(c.getSourceScope());
        if(!sourceScopeRep.isGround()) {
            return false;
        }
        Scope sourceScope = Scope.matcher().match(sourceScopeRep)
                .orElseThrow(() -> new TypeException("Expected a scope but got " + sourceScopeRep));
        return findScope(c.getTargetScope()).map(targetScope -> {
            scopeGraph.addDirectEdge(sourceScope, c.getLabel(), targetScope);
            return true;
        }).orElseGet(() -> {
            scopeGraph.addIncompleteDirectEdge(sourceScope, c.getLabel(), c.getTargetScope());
            return true;
        });
    }

    private boolean solve(CGImportEdge c) {
        ITerm scopeRep = find(c.getScope());
        if(!scopeRep.isGround()) {
            return false;
        }
        Scope scope = Scope.matcher().match(scopeRep)
                .orElseThrow(() -> new TypeException("Expected a scope but got " + scopeRep));
        return findOccurrence(c.getReference()).map(ref -> {
            scopeGraph.addImport(scope, c.getLabel(), ref);
            return true;
        }).orElseGet(() -> {
            scopeGraph.addIncompleteImportEdge(scope, c.getLabel(), c.getReference());
            return true;
        });
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

    private Optional<Scope> findScope(ITerm scopeTerm) {
        return Optional.of(find(scopeTerm)).filter(ITerm::isGround).map(
                st -> Scope.matcher().match(st).orElseThrow(() -> new TypeException("Expected a scope, got " + st)));
    }

    private Optional<Occurrence> findOccurrence(ITerm occurrenceTerm) {
        return Optional.of(find(occurrenceTerm)).filter(ITerm::isGround).map(ot -> Occurrence.matcher().match(ot)
                .orElseThrow(() -> new TypeException("Expected an occurrence, got " + ot)));
    }

}