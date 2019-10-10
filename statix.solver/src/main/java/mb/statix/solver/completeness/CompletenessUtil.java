package mb.statix.solver.completeness;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Collection;

import org.metaborg.util.functions.Action2;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.constraints.Constraints;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.spec.Spec;

public class CompletenessUtil {

    /**
     * Discover critical edges in constraint. The scopeTerm is not guaranteed to be ground or instantiated.
     */
    static void criticalEdges(IConstraint constraint, Spec spec, Action2<ITerm, ITerm> criticalEdge) {
        // @formatter:off
        constraint.match(Constraints.cases(
            onArith -> null,
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
            onTry -> null,
            onUser -> {
                spec.scopeExtensions().get(onUser.name()).stream()
                        .forEach(il -> criticalEdge.apply(onUser.args().get(il._1()), il._2()));
                return null;
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