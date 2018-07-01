package mb.statix.solver.constraint;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.solver.Completeness;
import mb.statix.solver.Delay;
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

    private final @Nullable IConstraint cause;

    public CTellRel(ITerm scopeTerm, ITerm relation, Iterable<ITerm> datumTerms) {
        this(scopeTerm, relation, datumTerms, null);
    }

    public CTellRel(ITerm scopeTerm, ITerm relation, Iterable<ITerm> datumTerms, @Nullable IConstraint cause) {
        this.scopeTerm = scopeTerm;
        this.relation = relation;
        this.datumTerms = ImmutableList.copyOf(datumTerms);
        this.cause = cause;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CTellRel withCause(@Nullable IConstraint cause) {
        return new CTellRel(scopeTerm, relation, datumTerms, cause);
    }

    @Override public Iterable<Tuple2<ITerm, ITerm>> scopeExtensions(Spec spec) {
        return Iterables2.from(ImmutableTuple2.of(scopeTerm, relation));
    }

    @Override public CTellRel apply(ISubstitution.Immutable subst) {
        return new CTellRel(subst.apply(scopeTerm), relation, subst.apply(datumTerms));
    }

    @Override public Optional<Result> solve(State state, Completeness completeness, IDebugContext debug) throws Delay {
        final Type type = state.spec().relations().get(relation);
        if(type == null) {
            debug.error("Ignoring data for unknown relation {}", relation);
            return Optional.empty();
        }
        if(type.getArity() != datumTerms.size()) {
            debug.error("Ignoring {}-ary data for {}-ary relation {}", datumTerms.size(), type.getArity(), relation);
            return Optional.empty();
        }

        final IUnifier.Immutable unifier = state.unifier();
        if(!unifier.isGround(scopeTerm)) {
            throw new Delay();
        }
        final Scope scope = Scope.matcher().match(scopeTerm, unifier)
                .orElseThrow(() -> new IllegalArgumentException("Expected scope, got " + unifier.toString(scopeTerm)));

        final ITerm key = B.newTuple(datumTerms.stream().limit(type.getInputArity()).collect(Collectors.toList()));
        if(!unifier.isGround(key)) {
            throw new Delay();
        }
        Optional<ITerm> existingValue = state.scopeGraph().getData().get(scope, relation).stream().filter(dt -> {
            return unifier.areEqual(key,
                    B.newTuple(dt.stream().limit(type.getInputArity()).collect(Collectors.toList())));
        }).findFirst().map(dt -> {
            return B.newTuple(dt.stream().skip(type.getInputArity()).collect(Collectors.toList()));
        });
        if(existingValue.isPresent()) {
            final ITerm value = B.newTuple(datumTerms.stream().skip(type.getInputArity()).collect(Collectors.toList()));
            final IConstraint eq = new CEqual(value, existingValue.get(), this);
            return Optional.of(Result.of(state, ImmutableSet.of(eq)));
        } else {
            final IScopeGraph.Immutable<ITerm, ITerm, ITerm> scopeGraph =
                    state.scopeGraph().addDatum(scope, relation, datumTerms);
            return Optional.of(Result.of(state.withScopeGraph(scopeGraph), ImmutableSet.of()));
        }
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
        return toString(PersistentUnifier.Immutable.of());
    }

}