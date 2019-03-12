package mb.statix.solver;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.util.functions.Predicate2;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.optionals.Optionals;

import com.google.common.collect.ImmutableSet;

import io.usethesource.capsule.Set;
import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.scopegraph.reference.CriticalEdge;

public class Completeness {

    private final Set.Immutable<IConstraint> incomplete;

    public Completeness() {
        this(Set.Immutable.of());
    }

    public Completeness(Set.Immutable<IConstraint> incomplete) {
        this.incomplete = incomplete;
    }

    public boolean isComplete(ITerm scope, ITerm label, State state) {
        final IUnifier unifier = state.unifier();
        final Predicate2<ITerm, ITerm> equal = (t1, t2) -> {
            return t2.equals(label) && unifier.areEqual(t1, scope).orElse(false /* (1) */);
            /* (1) This assumes well-formed constraints and specifications,
             * which guarantee us that a non-ground scope variable is never
             * instantiated to an already known scope.
             */
        };
        return incomplete.stream().flatMap(c -> Iterables2.stream(c.criticalEdges(state.spec())))
                .noneMatch(sl -> equal.test(sl.scope(), sl.label()));
    }

    public Completeness add(IConstraint constraint) {
        return new Completeness(incomplete.__insert(constraint));
    }

    public Completeness addAll(Iterable<IConstraint> constraints) {
        return new Completeness(incomplete.__insertAll(ImmutableSet.copyOf(constraints)));
    }

    public Completeness remove(IConstraint constraint) {
        return new Completeness(incomplete.__remove(constraint));
    }

    public Completeness removeAll(Iterable<IConstraint> constraints) {
        return new Completeness(incomplete.__removeAll(ImmutableSet.copyOf(constraints)));
    }

    public static List<CriticalEdge> criticalEdges(IConstraint constraint, State state) {
        return constraint.criticalEdges(state.spec()).stream().flatMap(ce -> {
            final Optional<CriticalEdge> edge =
                    Scope.matcher().match(ce.scope(), state.unifier()).map(s -> CriticalEdge.of(s, ce.label()));
            return Optionals.stream(edge);
        }).collect(Collectors.toList());
    }

}