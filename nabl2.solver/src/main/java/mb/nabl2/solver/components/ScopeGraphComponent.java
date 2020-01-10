package mb.nabl2.solver.components;

import java.util.Optional;

import org.metaborg.util.functions.Function1;

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

    public Optional<SolveResult> solve(IScopeGraphConstraint constraint) {
        if(constraint.match(
                IScopeGraphConstraint.Cases.of(this::solve, this::solve, this::solve, this::solve, this::solve))) {
            return Optional.of(SolveResult.empty());
        } else {
            return Optional.empty();
        }
    }

    public IEsopScopeGraph.Immutable<Scope, Label, Occurrence, ITerm> finish() {
        return scopeGraph.freeze();
    }

    // ------------------------------------------------------------------------------------------------------//

    private boolean solve(CGDecl c) {
        final ITerm scopeTerm = c.getScope();
        final ITerm declTerm = c.getDeclaration();
        if(!(unifier().isGround(scopeTerm) && unifier().isGround(declTerm))) {
            return false;
        }
        Scope scope = Scope.matcher().match(scopeTerm, unifier())
                .orElseThrow(() -> new TypeException("Expected a scope as first agument to " + c));
        Occurrence decl = Occurrence.matcher().match(declTerm, unifier())
                .orElseThrow(() -> new TypeException("Expected an occurrence as second argument to " + c));
        scopeGraph.addDecl(scope, decl);
        return true;
    }

    private boolean solve(CGRef c) {
        final ITerm scopeTerm = c.getScope();
        final ITerm refTerm = c.getReference();
        if(!(unifier().isGround(scopeTerm) && unifier().isGround(refTerm))) {
            return false;
        }
        Occurrence ref = Occurrence.matcher().match(refTerm, unifier())
                .orElseThrow(() -> new TypeException("Expected an occurrence as first argument to " + c));
        Scope scope = Scope.matcher().match(scopeTerm, unifier())
                .orElseThrow(() -> new TypeException("Expected a scope as second argument to " + c));
        scopeGraph.addRef(ref, scope);
        return true;
    }

    private boolean solve(CGDirectEdge c) {
        ITerm sourceScopeRep = c.getSourceScope();
        if(!unifier().isGround(sourceScopeRep)) {
            return false;
        }
        Scope sourceScope = Scope.matcher().match(sourceScopeRep, unifier())
                .orElseThrow(() -> new TypeException("Expected a scope but got " + sourceScopeRep));
        return findScope(c.getTargetScope()).map(targetScope -> {
            scopeGraph.addDirectEdge(sourceScope, c.getLabel(), targetScope);
            return true;
        }).orElseGet(() -> {
            scopeGraph.addIncompleteDirectEdge(sourceScope, c.getLabel(), c.getTargetScope(), unifier()::getVars);
            return true;
        });
    }

    private boolean solve(CGImportEdge c) {
        ITerm scopeRep = c.getScope();
        if(!unifier().isGround(scopeRep)) {
            return false;
        }
        Scope scope = Scope.matcher().match(scopeRep, unifier())
                .orElseThrow(() -> new TypeException("Expected a scope but got " + scopeRep));
        return findOccurrence(c.getReference()).map(ref -> {
            scopeGraph.addImportEdge(scope, c.getLabel(), ref);
            return true;
        }).orElseGet(() -> {
            scopeGraph.addIncompleteImportEdge(scope, c.getLabel(), c.getReference(), unifier()::getVars);
            return true;
        });
    }

    private boolean solve(CGExportEdge c) {
        ITerm scopeTerm = c.getScope();
        ITerm declTerm = c.getDeclaration();
        if(!(unifier().isGround(scopeTerm) && unifier().isGround(declTerm))) {
            return false;
        }
        Scope scope = Scope.matcher().match(scopeTerm, unifier())
                .orElseThrow(() -> new TypeException("Expected a scope as third argument to " + c));
        Occurrence decl = Occurrence.matcher().match(declTerm, unifier())
                .orElseThrow(() -> new TypeException("Expected an occurrence as first argument to " + c));
        scopeGraph.addExportEdge(decl, c.getLabel(), scope);
        return true;
    }

    // ------------------------------------------------------------------------------------------------------//

    public void updateAll(Function1<ITerm, ? extends Iterable<? extends ITerm>> norm) throws InterruptedException {
        scopeGraph.reduceAll(norm);
    }

    public void update(Iterable<? extends ITerm> vars, Function1<ITerm, ? extends Iterable<? extends ITerm>> norm)
            throws InterruptedException {
        scopeGraph.reduce(vars, norm, this::findScope, this::findOccurrence);
    }

    private Optional<Scope> findScope(ITerm scopeTerm) {
        return Optional.of(scopeTerm).filter(unifier()::isGround).map(st -> Scope.matcher().match(st, unifier())
                .orElseThrow(() -> new TypeException("Expected a scope, got " + st)));
    }

    private Optional<Occurrence> findOccurrence(ITerm occurrenceTerm) {
        return Optional.of(occurrenceTerm).filter(unifier()::isGround).map(ot -> Occurrence.matcher()
                .match(ot, unifier()).orElseThrow(() -> new TypeException("Expected an occurrence, got " + ot)));
    }

}