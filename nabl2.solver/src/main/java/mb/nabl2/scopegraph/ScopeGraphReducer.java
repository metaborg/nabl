package mb.nabl2.scopegraph;

import java.util.List;
import java.util.Optional;

import org.metaborg.util.Ref;

import mb.nabl2.scopegraph.esop.CriticalEdge;
import mb.nabl2.scopegraph.esop.IEsopScopeGraph;
import mb.nabl2.scopegraph.esop.IEsopScopeGraph.Transient;
import mb.nabl2.scopegraph.terms.Label;
import mb.nabl2.scopegraph.terms.Occurrence;
import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.solver.TypeException;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.terms.unification.u.IUnifier.Immutable;

public class ScopeGraphReducer {

    private final IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> scopeGraph;
    private final Ref<IUnifier.Immutable> unifier;

    public ScopeGraphReducer(Transient<Scope, Label, Occurrence, ITerm> scopeGraph, Ref<Immutable> unifier) {
        this.scopeGraph = scopeGraph;
        this.unifier = unifier;
    }

    public List<CriticalEdge> updateAll() throws InterruptedException {
        return update(scopeGraph.incompleteVars());
    }

    public List<CriticalEdge> update(Iterable<? extends ITerm> vars) throws InterruptedException {
        return scopeGraph.reduce(vars, unifier.get()::getVars, this::findScope, this::findOccurrence);
    }

    private Optional<Scope> findScope(ITerm scopeTerm) {
        return Optional.of(scopeTerm).filter(unifier.get()::isGround).map(st -> Scope.matcher().match(st, unifier.get())
                .orElseThrow(() -> new TypeException("Expected a scope, got " + st)));
    }

    private Optional<Occurrence> findOccurrence(ITerm occurrenceTerm) {
        return Optional.of(occurrenceTerm).filter(unifier.get()::isGround).map(ot -> Occurrence.matcher()
                .match(ot, unifier.get()).orElseThrow(() -> new TypeException("Expected an occurrence, got " + ot)));
    }

}