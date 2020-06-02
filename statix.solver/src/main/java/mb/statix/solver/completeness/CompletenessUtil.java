package mb.statix.solver.completeness;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Collection;

import org.metaborg.util.functions.Action2;
import org.metaborg.util.unit.Unit;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.statix.constraints.Constraints;
import mb.statix.scopegraph.reference.EdgeOrData;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.CriticalEdge;
import mb.statix.solver.IConstraint;
import mb.statix.spec.Spec;

public class CompletenessUtil {

    /**
     * Discover critical edges in constraint. The scopeTerm is not guaranteed to be ground or instantiated.
     */
    static void criticalEdges(IConstraint constraint, Spec spec, Action2<ITerm, EdgeOrData<ITerm>> criticalEdge) {
        // @formatter:off
        constraint.match(Constraints.cases(
            onArith -> Unit.unit,
            onConj -> {
                Constraints.disjoin(onConj).forEach(c -> criticalEdges(c, spec, criticalEdge));
                return Unit.unit;
            },
            onEqual -> Unit.unit,
            onExists -> {
                criticalEdges(onExists.constraint(), spec, (s, l) -> {
                    if(!onExists.vars().contains(s)) {
                        criticalEdge.apply(s, l);
                    }
                });
                return Unit.unit;
            },
            onFalse -> Unit.unit,
            onInequal -> Unit.unit,
            onNew -> {
                criticalEdge.apply(onNew.scopeTerm(), EdgeOrData.data());
                return Unit.unit;
            },
            onResolveQuery -> Unit.unit,
            onTellEdge -> {
                criticalEdge.apply(onTellEdge.sourceTerm(), EdgeOrData.edge(onTellEdge.label()));
                return Unit.unit;
            },
            onTermId -> Unit.unit,
            onTermProperty -> Unit.unit,
            onTrue -> Unit.unit,
            onTry -> Unit.unit,
            onUser -> {
                spec.scopeExtensions().get(onUser.name()).stream()
                        .forEach(il -> criticalEdge.apply(onUser.args().get(il._1()), EdgeOrData.edge(il._2())));
                return Unit.unit;
            }
        ));
        // @formatter:on
    }

    /**
     * Return critical edges for this constraint.
     */
    public static Collection<CriticalEdge> criticalEdges(IConstraint constraint, Spec spec) {
        ImmutableList.Builder<CriticalEdge> criticalEdges = ImmutableList.builder();
        criticalEdges(constraint, spec, (s, l) -> criticalEdges.add(CriticalEdge.of(s, l)));
        return criticalEdges.build();
    }

    /**
     * Return critical edges for this constraint, normalized against the given unifier.
     */
    public static Collection<CriticalEdge> criticalEdges(IConstraint constraint, Spec spec, IUnifier unifier) {
        ImmutableList.Builder<CriticalEdge> criticalEdges = ImmutableList.builder();
        criticalEdges(constraint, spec, (s, l) -> {
            scopeOrVar().match(s, unifier).ifPresent(scopeOrVar -> {
                criticalEdges.add(CriticalEdge.of(scopeOrVar, l));
            });
        });
        return criticalEdges.build();
    }

    public static IMatcher<ITerm> scopeOrVar() {
        return M.cases(Scope.matcher(), M.var());
    }

}