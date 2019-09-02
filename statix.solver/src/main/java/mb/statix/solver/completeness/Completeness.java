package mb.statix.solver.completeness;

import java.util.Collection;
import java.util.List;

import org.metaborg.util.functions.Action2;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.constraints.Constraints;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.scopegraph.terms.AScope;
import mb.statix.solver.IConstraint;
import mb.statix.spec.Spec;

public class Completeness {

    static void criticalEdges(IConstraint constraint, Spec spec, Action2<ITerm, ITerm> criticalEdge) {
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

    static Collection<CriticalEdge> criticalEdges(IConstraint constraint, Spec spec) {
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