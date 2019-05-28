package mb.statix.solver.completeness;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.metaborg.util.functions.Action2;
import org.metaborg.util.functions.Predicate2;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.constraints.Constraints;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.scopegraph.terms.AScope;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.spec.Spec;

public class Completeness implements ICompleteness {

    private final Spec spec;
    private final Set<IConstraint> incomplete;

    public Completeness(Spec spec) {
        this.spec = spec;
        this.incomplete = new HashSet<>();
    }

    @Override public boolean isComplete(Scope scope, ITerm label, IUnifier unifier) {
        final Predicate2<ITerm, ITerm> equal = (t1, t2) -> {
            return t2.equals(label) && unifier.areEqual(t1, scope).orElse(false /* (1) */);
            /* (1) This assumes well-formed constraints and specifications,
             * which guarantee us that a non-ground scope variable is never
             * instantiated to an already known scope.
             */
        };
        return incomplete.stream().flatMap(c -> Iterables2.stream(criticalEdges(c, spec)))
                .noneMatch(sl -> equal.test(sl.scope(), sl.label()));
    }

    @Override public void add(IConstraint constraint, IUnifier unifier) {
        incomplete.add(constraint);
    }

    @Override public void remove(IConstraint constraint, IUnifier unifier) {
        incomplete.remove(constraint);
    }

    @Override public void update(ITermVar var, IUnifier unifier) {
    }

    public static void criticalEdges(IConstraint constraint, Spec spec, Action2<ITerm, ITerm> criticalEdge) {
        // @formatter:off
        constraint.match(Constraints.cases(
            onConj -> {
                criticalEdges(onConj.left(), spec, criticalEdge);
                criticalEdges(onConj.right(), spec, criticalEdge);
                return null;
            },
            onEqual -> null,
            onExists -> {
                criticalEdges(onExists.constraint(), spec, (s, l) -> {
                    if(!onExists.vars().contains(s)) {
                        criticalEdge.apply(s, l);
                    }
                });
                return null;
            },
            onFalse -> null,
            onInequal -> null,
            onNew -> null,
            onPathLt -> null,
            onPathMatch -> null,
            onResolveQuery -> null,
            onTellEdge -> {
                criticalEdge.apply(onTellEdge.sourceTerm(), onTellEdge.label());
                return null;
            },
            onTellRel -> {
                criticalEdge.apply(onTellRel.scopeTerm(), onTellRel.relation());
                return null;
            },
            onTermId -> null,
            onTermProperty -> null,
            onTrue -> null,
            onUser -> {
                spec.scopeExtensions().get(onUser.name()).stream()
                        .forEach(il -> criticalEdge.apply(onUser.args().get(il._1()), il._2()));
                return null;
            }
        ));
        // @formatter:on
    }

    public static Collection<CriticalEdge> criticalEdges(IConstraint constraint, Spec spec) {
        ImmutableList.Builder<CriticalEdge> criticalEdges = ImmutableList.builder();
        criticalEdges(constraint, spec, (s, l) -> criticalEdges.add(CriticalEdge.of(s, l)));
        return criticalEdges.build();
    }

    public static List<CriticalEdge> criticalEdges(IConstraint constraint, Spec spec, IUnifier unifier) {
        ImmutableList.Builder<CriticalEdge> criticalEdges = ImmutableList.builder();
        criticalEdges(constraint, spec, (scopeTerm, label) -> {
            AScope.matcher().match(scopeTerm, unifier).ifPresent(scope -> {
                criticalEdges.add(CriticalEdge.of(scope, label));
            });
        });
        return criticalEdges.build();
    }

}