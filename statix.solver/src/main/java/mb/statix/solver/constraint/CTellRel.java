package mb.statix.solver.constraint;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.solver.Completeness;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.Result;
import mb.statix.solver.State;
import mb.statix.spec.Spec;
import mb.statix.spec.Type;

public class CTellRel implements IConstraint {

    private final ITerm scopeTerm;
    private final ITerm relation;
    private final List<ITerm> datumTerms;

    public CTellRel(ITerm scopeTerm, ITerm relation, Iterable<ITerm> datumTerms) {
        this.scopeTerm = scopeTerm;
        this.relation = relation;
        this.datumTerms = ImmutableList.copyOf(datumTerms);
    }

    @Override public Iterable<Tuple2<ITerm, ITerm>> scopeExtensions(Spec spec) {
        return Iterables2.from(ImmutableTuple2.of(scopeTerm, relation));
    }

    @Override public IConstraint apply(Function1<ITerm, ITerm> map) {
        return new CTellRel(map.apply(scopeTerm), relation,
                datumTerms.stream().map(map::apply).collect(Collectors.toList()));
    }

    @Override public Optional<Result> solve(State state, Completeness completeness, IDebugContext debug) {
        final Type type = state.spec().relations().get(relation);
        if(type == null) {
            debug.error("Ignoring data for unknown relation {}", relation);
            return Optional.of(Result.of(state, ImmutableSet.of(new CFalse())));
        }
        if(type.getArity() != datumTerms.size()) {
            debug.error("Ignoring {}-ary data for {}-ary relation {}", datumTerms.size(), type.getArity(), relation);
            return Optional.of(Result.of(state, ImmutableSet.of(new CFalse())));
        }

        final IUnifier.Immutable unifier = state.unifier();
        if(!unifier.isGround(scopeTerm)) {
            return Optional.empty();
        }
        final Scope scope = Scope.matcher().match(scopeTerm, unifier)
                .orElseThrow(() -> new IllegalArgumentException("Expected scope, got " + scopeTerm));

        if(!datumTerms.stream().limit(type.getInputArity()).allMatch(unifier::isGround)) {
            return Optional.empty();
        }
        final ITerm datumTerm = type.getArity() == 1 ? datumTerms.get(0) : B.newTuple(datumTerms);

        final IScopeGraph.Immutable<ITerm, ITerm, ITerm> scopeGraph =
                state.scopeGraph().addDatum(scope, relation, datumTerm);
        return Optional.of(Result.of(state.withScopeGraph(scopeGraph), ImmutableSet.of()));
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