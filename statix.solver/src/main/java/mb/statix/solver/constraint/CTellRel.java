package mb.statix.solver.constraint;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.util.functions.Function1;

import com.google.common.collect.ImmutableList;

import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.solver.Config;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.State;

public class CTellRel implements IConstraint {

    final ITerm scopeTerm;
    final ITerm relation;
    final List<ITerm> datumTerms;

    public CTellRel(ITerm scopeTerm, ITerm relation, Iterable<ITerm> datumTerms) {
        this.scopeTerm = scopeTerm;
        this.relation = relation;
        this.datumTerms = ImmutableList.copyOf(datumTerms);
    }

    @Override public IConstraint apply(Function1<ITerm, ITerm> map) {
        return new CTellRel(map.apply(scopeTerm), relation,
                datumTerms.stream().map(map::apply).collect(Collectors.toList()));
    }

    @Override public Optional<Config> solve(State state, IDebugContext debug) {
        final IUnifier.Immutable unifier = state.unifier();
        if(!unifier.isGround(scopeTerm)) {
            return Optional.empty();
        }
        final Scope scope = Scope.matcher().match(scopeTerm, unifier)
                .orElseThrow(() -> new IllegalArgumentException("Expected scope, got " + scopeTerm));
        final ITerm datumTerm = B.newTuple(datumTerms);
        if(!unifier.isGround(datumTerm)) {
            return Optional.empty();
        }
        final ITerm datum = unifier.findTerm(datumTerm);
        // FIXME: use relation type to check components before the arrow are ground, or return empty
        final IScopeGraph.Immutable<ITerm, ITerm, ITerm, ITerm> scopeGraph =
                state.scopeGraph().addDatum(scope, relation, datum);
        return Optional.of(Config.builder().state(state.withScopeGraph(scopeGraph)).build());
    }

    @Override public String toString(IUnifier unifier) {
        final StringBuilder sb = new StringBuilder();
        sb.append(unifier.toString(scopeTerm));
        sb.append(" -");
        sb.append(unifier.toString(relation));
        sb.append("-[] ");
        sb.append(unifier.toString(B.newTuple(datumTerms)));
        return sb.toString();
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(scopeTerm);
        sb.append(" -");
        sb.append(relation);
        sb.append("-[] ");
        sb.append(B.newTuple(datumTerms));
        return sb.toString();
    }

}