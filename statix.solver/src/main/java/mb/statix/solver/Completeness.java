package mb.statix.solver;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.Predicate2;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.optionals.Optionals;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.scopegraph.terms.AScope;
import mb.statix.solver.constraint.Constraints;
import mb.statix.spec.Spec;

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
        return incomplete.stream().flatMap(c -> Iterables2.stream(criticalEdges(c, state.spec())))
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

    public static Collection<CriticalEdge> criticalEdges(IConstraint constraint, Spec spec) {
        // @formatter:off
        final Function1<IConstraint, Collection<CriticalEdge>> criticalEdges = Constraints.cases(
            onEqual -> ImmutableList.of(),
            onFalse -> ImmutableList.of(),
            onInequal -> ImmutableList.of(),
            onNew -> ImmutableList.of(),
            onPathDst -> ImmutableList.of(),
            onPathLabels -> ImmutableList.of(),
            onPathLt -> ImmutableList.of(),
            onPathMatch -> ImmutableList.of(),
            onPathScopes -> ImmutableList.of(),
            onPathSrc -> ImmutableList.of(),
            onResolveQuery -> ImmutableList.of(),
            onTellEdge -> ImmutableList.of(CriticalEdge.of(onTellEdge.sourceTerm(), onTellEdge.label())),
            onTellRel -> ImmutableList.of(CriticalEdge.of(onTellRel.scopeTerm(), onTellRel.relation())),
            onTermId -> ImmutableList.of(),
            onTrue -> ImmutableList.of(),
            onUser -> spec.scopeExtensions().get(onUser.name()).stream()
                              .map(il -> CriticalEdge.of(onUser.args().get(il._1()), il._2()))
                              .collect(Collectors.toList())
        );
        // @formatter:on
        return criticalEdges.apply(constraint);
    }

    public static List<CriticalEdge> criticalEdges(IConstraint constraint, State state) {
        return criticalEdges(constraint, state.spec()).stream().flatMap(ce -> {
            final Optional<CriticalEdge> edge =
                    AScope.matcher().match(ce.scope(), state.unifier()).map(s -> CriticalEdge.of(s, ce.label()));
            return Optionals.stream(edge);
        }).collect(Collectors.toList());
    }

}