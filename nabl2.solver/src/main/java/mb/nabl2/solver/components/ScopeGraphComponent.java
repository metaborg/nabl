package mb.nabl2.solver.components;

import java.util.Optional;

import com.google.common.collect.Iterables;

import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.constraints.scopegraph.CGDecl;
import mb.nabl2.constraints.scopegraph.CGDirectEdge;
import mb.nabl2.constraints.scopegraph.CGExportEdge;
import mb.nabl2.constraints.scopegraph.CGImportEdge;
import mb.nabl2.constraints.scopegraph.CGRef;
import mb.nabl2.constraints.scopegraph.IScopeGraphConstraint;
import mb.nabl2.scopegraph.esop.IEsopScopeGraph;
import mb.nabl2.scopegraph.terms.Label;
import mb.nabl2.scopegraph.terms.Occurrence;
import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.solver.ASolver;
import mb.nabl2.solver.ISolver;
import mb.nabl2.solver.ISolver.SeedResult;
import mb.nabl2.solver.ISolver.SolveResult;
import mb.nabl2.solver.SolverCore;
import mb.nabl2.solver.TypeException;
import mb.nabl2.solver.exceptions.DelayException;
import mb.nabl2.solver.exceptions.VariableDelayException;
import mb.nabl2.terms.ITerm;

public class ScopeGraphComponent extends ASolver {

    private final IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> scopeGraph;

    public ScopeGraphComponent(SolverCore core, IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> initial) {
        super(core);
        this.scopeGraph = initial;
    }

    // ------------------------------------------------------------------------------------------------------//

    public ISolver.SeedResult seed(IEsopScopeGraph.Immutable<Scope, Label, Occurrence, ITerm> solution,
            @SuppressWarnings("unused") IMessageInfo message) throws InterruptedException {
        scopeGraph.addAll(solution, unifier()::getVars);
        return SeedResult.empty();
    }

    public SolveResult solve(IScopeGraphConstraint constraint) throws DelayException {
        return constraint.matchOrThrow(
                IScopeGraphConstraint.CheckedCases.of(this::solve, this::solve, this::solve, this::solve, this::solve));
    }

    public IEsopScopeGraph.Immutable<Scope, Label, Occurrence, ITerm> finish() {
        return scopeGraph.freeze();
    }

    // ------------------------------------------------------------------------------------------------------//

    private SolveResult solve(CGDecl c) throws DelayException {
        final ITerm scopeTerm = c.getScope();
        final ITerm declTerm = c.getDeclaration();
        if(!(unifier().isGround(scopeTerm) && unifier().isGround(declTerm))) {
            throw new VariableDelayException(
                    Iterables.concat(unifier().getVars(scopeTerm), unifier().getVars(declTerm)));
        }
        Scope scope = Scope.matcher().match(scopeTerm, unifier())
                .orElseThrow(() -> new TypeException("Expected a scope as first agument to " + c));
        Occurrence decl = Occurrence.matcher().match(declTerm, unifier())
                .orElseThrow(() -> new TypeException("Expected an occurrence as second argument to " + c));
        scopeGraph.addDecl(scope, decl);
        return SolveResult.empty();
    }

    private SolveResult solve(CGRef c) throws DelayException {
        final ITerm scopeTerm = c.getScope();
        final ITerm refTerm = c.getReference();
        if(!(unifier().isGround(scopeTerm) && unifier().isGround(refTerm))) {
            throw new VariableDelayException(
                    Iterables.concat(unifier().getVars(scopeTerm), unifier().getVars(refTerm)));
        }
        Occurrence ref = Occurrence.matcher().match(refTerm, unifier())
                .orElseThrow(() -> new TypeException("Expected an occurrence as first argument to " + c));
        Scope scope = Scope.matcher().match(scopeTerm, unifier())
                .orElseThrow(() -> new TypeException("Expected a scope as second argument to " + c));
        scopeGraph.addRef(ref, scope);
        return SolveResult.empty();
    }

    private SolveResult solve(CGDirectEdge c) throws DelayException {
        ITerm sourceScopeRep = c.getSourceScope();
        if(!unifier().isGround(sourceScopeRep)) {
            throw new VariableDelayException(unifier().getVars(sourceScopeRep));
        }
        Scope sourceScope = Scope.matcher().match(sourceScopeRep, unifier())
                .orElseThrow(() -> new TypeException("Expected a scope but got " + sourceScopeRep));
        return findScope(c.getTargetScope()).map(targetScope -> {
            scopeGraph.addDirectEdge(sourceScope, c.getLabel(), targetScope);
            return SolveResult.empty();
        }).orElseGet(() -> {
            scopeGraph.addIncompleteDirectEdge(sourceScope, c.getLabel(), c.getTargetScope(), unifier()::getVars);
            return SolveResult.empty();
        });
    }

    private SolveResult solve(CGImportEdge c) throws DelayException {
        ITerm scopeRep = c.getScope();
        if(!unifier().isGround(scopeRep)) {
            throw new VariableDelayException(unifier().getVars(scopeRep));
        }
        Scope scope = Scope.matcher().match(scopeRep, unifier())
                .orElseThrow(() -> new TypeException("Expected a scope but got " + scopeRep));
        return findOccurrence(c.getReference()).map(ref -> {
            scopeGraph.addImportEdge(scope, c.getLabel(), ref);
            return SolveResult.empty();
        }).orElseGet(() -> {
            scopeGraph.addIncompleteImportEdge(scope, c.getLabel(), c.getReference(), unifier()::getVars);
            return SolveResult.empty();
        });
    }

    private SolveResult solve(CGExportEdge c) throws DelayException {
        ITerm scopeTerm = c.getScope();
        ITerm declTerm = c.getDeclaration();
        if(!(unifier().isGround(scopeTerm) && unifier().isGround(declTerm))) {
            throw new VariableDelayException(
                    Iterables.concat(unifier().getVars(scopeTerm), unifier().getVars(declTerm)));
        }
        Scope scope = Scope.matcher().match(scopeTerm, unifier())
                .orElseThrow(() -> new TypeException("Expected a scope as third argument to " + c));
        Occurrence decl = Occurrence.matcher().match(declTerm, unifier())
                .orElseThrow(() -> new TypeException("Expected an occurrence as first argument to " + c));
        scopeGraph.addExportEdge(decl, c.getLabel(), scope);
        return SolveResult.empty();
    }

    // ------------------------------------------------------------------------------------------------------//

    private Optional<Scope> findScope(ITerm scopeTerm) {
        return Optional.of(scopeTerm).filter(unifier()::isGround).map(st -> Scope.matcher().match(st, unifier())
                .orElseThrow(() -> new TypeException("Expected a scope, got " + st)));
    }

    private Optional<Occurrence> findOccurrence(ITerm occurrenceTerm) {
        return Optional.of(occurrenceTerm).filter(unifier()::isGround).map(ot -> Occurrence.matcher()
                .match(ot, unifier()).orElseThrow(() -> new TypeException("Expected an occurrence, got " + ot)));
    }

}