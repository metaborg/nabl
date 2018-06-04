package mb.statix.solver.constraint;

import java.util.Optional;

import org.metaborg.util.functions.Function1;

import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.solver.Config;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.State;

public class CTellEdge implements IConstraint {

    final ITerm sourceTerm;
    final ITerm label;
    final ITerm targetTerm;

    public CTellEdge(ITerm sourceTerm, ITerm label, ITerm targetTerm) {
        this.sourceTerm = sourceTerm;
        this.label = label;
        this.targetTerm = targetTerm;
    }

    @Override public IConstraint apply(Function1<ITerm, ITerm> map) {
        return new CTellEdge(map.apply(sourceTerm), label, map.apply(targetTerm));
    }

    @Override public Optional<Config> solve(State state, IDebugContext debug) {
        final IUnifier.Immutable unifier = state.unifier();
        final Optional<Scope> source = Scope.matcher().match(sourceTerm, unifier);
        final Optional<Scope> target = Scope.matcher().match(targetTerm, unifier);
        if(!source.isPresent() || !target.isPresent()) {
            return Optional.empty();
        }
        final IScopeGraph.Immutable<ITerm, ITerm, ITerm, ITerm> scopeGraph =
                state.scopeGraph().addEdge(source.get(), label, target.get());
        return Optional.of(Config.builder().state(state.withScopeGraph(scopeGraph)).build());
    }

    @Override public String toString(IUnifier unifier) {
        final StringBuilder sb = new StringBuilder();
        sb.append(unifier.toString(sourceTerm));
        sb.append(" -");
        sb.append(unifier.toString(label));
        sb.append("-> ");
        sb.append(unifier.toString(targetTerm));
        return sb.toString();
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(sourceTerm);
        sb.append(" -");
        sb.append(label);
        sb.append("-> ");
        sb.append(targetTerm);
        return sb.toString();
    }

}