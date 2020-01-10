package mb.nabl2.scopegraph;

import java.util.List;

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
        final List<CriticalEdge> criticalEdges =
                scopeGraph.reduceAll(unifier.get()::getVars, this::findScope, this::findOccurrence);
        return criticalEdges;
    }

    public List<CriticalEdge> update(Iterable<? extends ITerm> vars) throws InterruptedException {
        final List<CriticalEdge> criticalEdges =
                scopeGraph.reduce(vars, unifier.get()::getVars, this::findScope, this::findOccurrence);
        return criticalEdges;
    }

    private Scope findScope(ITerm scopeTerm) {
        return Scope.matcher().match(scopeTerm, unifier.get())
                .orElseThrow(() -> new TypeException("Expected a scope, got " + unifier.get().toString(scopeTerm)));
    }

    private Occurrence findOccurrence(ITerm occurrenceTerm) {
        return Occurrence.matcher().match(occurrenceTerm, unifier.get()).orElseThrow(
                () -> new TypeException("Expected an occurrence, got " + unifier.get().toString(occurrenceTerm)));
    }

}