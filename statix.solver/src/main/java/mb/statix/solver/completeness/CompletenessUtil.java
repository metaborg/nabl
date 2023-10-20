package mb.statix.solver.completeness;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Collection;

import org.metaborg.util.collection.ImList;
import org.metaborg.util.functions.Action2;
import org.metaborg.util.tuple.Tuple2;
import org.metaborg.util.unit.Unit;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.SetMultimap;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.terms.unification.ud.PersistentUniDisunifier;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.statix.constraints.CCompiledQuery;
import mb.statix.constraints.CConj;
import mb.statix.constraints.CNew;
import mb.statix.constraints.CResolveQuery;
import mb.statix.constraints.CTellEdge;
import mb.statix.constraints.CTry;
import mb.statix.constraints.CUser;
import mb.statix.constraints.Constraints;
import mb.statix.constraints.IResolveQuery;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.CriticalEdge;
import mb.statix.solver.IConstraint;
import mb.statix.solver.query.QueryFilter;
import mb.statix.solver.query.QueryMin;
import mb.statix.spec.Rule;
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
                spec.scopeExtensions().get(onUser.name()).forEach(il ->
                    criticalEdge.apply(onUser.args().get(il._1()), EdgeOrData.edge(il._2())));
                return Unit.unit;
            }
        ));
        // @formatter:on
    }

    /**
     * Return critical edges for this constraint.
     */
    public static Collection<CriticalEdge> criticalEdges(IConstraint constraint, Spec spec) {
        ImList.Mutable<CriticalEdge> criticalEdges = ImList.Mutable.of();
        criticalEdges(constraint, spec, (s, l) -> criticalEdges.add(CriticalEdge.of(s, l)));
        return criticalEdges.freeze();
    }

    /**
     * Return critical edges for this constraint, normalized against the given unifier.
     */
    public static Collection<CriticalEdge> criticalEdges(IConstraint constraint, Spec spec, IUnifier unifier) {
        ImList.Mutable<CriticalEdge> criticalEdges = ImList.Mutable.of();
        criticalEdges(constraint, spec, (s, l) -> {
            scopeOrVar().match(s, unifier).ifPresent(scopeOrVar -> {
                criticalEdges.add(CriticalEdge.of(scopeOrVar, l));
            });
        });
        return criticalEdges.freeze();
    }

    public static IMatcher<ITerm> scopeOrVar() {
        return M.cases(Scope.matcher(), M.var());
    }

    /**
     * Pre-compute the critical edges that are introduced when scoping constructs such as rules and existantial
     * constraints are unfolded.
     *
     * If critical edges escape from the top-level rule, an IllegalArgumentException is thrown.
     */
    public static Rule precomputeCriticalEdges(Rule rule, SetMultimap<String, Tuple2<Integer, ITerm>> spec) {
        return precomputeCriticalEdges(rule, spec, (s, l) -> {
            throw new IllegalArgumentException("Rule cannot have escaping critical edges.");
        });
    }

    public static Tuple2<IConstraint, ICompleteness.Immutable> precomputeCriticalEdges(IConstraint constraint,
            SetMultimap<String, Tuple2<Integer, ITerm>> spec) {
        final ICompleteness.Transient criticalEdges = Completeness.Transient.of();
        IConstraint newConstraint = precomputeCriticalEdges(constraint, spec, (s, l) -> {
            criticalEdges.add(s, l, PersistentUniDisunifier.Immutable.of());
        });
        return Tuple2.of(newConstraint, criticalEdges.freeze());
    }

    static Rule precomputeCriticalEdges(Rule rule, SetMultimap<String, Tuple2<Integer, ITerm>> spec,
            Action2<ITerm, EdgeOrData<ITerm>> criticalEdge) {
        final Set.Immutable<ITermVar> paramVars = rule.paramVars();
        final ICompleteness.Transient criticalEdges = Completeness.Transient.of();
        final IConstraint newBody = precomputeCriticalEdges(rule.body(), spec, (s, l) -> {
            if(paramVars.contains(s)) {
                criticalEdges.add(s, l, PersistentUniDisunifier.Immutable.of());
            } else {
                criticalEdge.apply(s, l);
            }
        });
        return rule.withBody(newBody).withBodyCriticalEdges(criticalEdges.freeze());
    }

    static IConstraint precomputeCriticalEdges(IConstraint constraint, SetMultimap<String, Tuple2<Integer, ITerm>> spec,
            Action2<ITerm, EdgeOrData<ITerm>> criticalEdge) {
        // @formatter:off
        return constraint.match(Constraints.cases(
            carith -> carith,
            cconj -> {
                final IConstraint newLeft = precomputeCriticalEdges(cconj.left(), spec, criticalEdge);
                final IConstraint newRight = precomputeCriticalEdges(cconj.right(), spec, criticalEdge);
                return cconj.withArguments(newLeft, newRight);
            },
            cequal -> cequal,
            cexists -> {
                final ICompleteness.Transient bodyCriticalEdges = Completeness.Transient.of();
                final IConstraint newBody = precomputeCriticalEdges(cexists.constraint(), spec, (s, l) -> {
                    if(cexists.vars().contains(s)) {
                        bodyCriticalEdges.add(s, l, PersistentUniDisunifier.Immutable.of());
                    } else {
                        criticalEdge.apply(s, l);
                    }
                });
                return cexists.withArguments(cexists.vars(), newBody).withBodyCriticalEdges(bodyCriticalEdges.freeze());
            },
            cfalse -> cfalse,
            cinequal -> cinequal,
            cnew -> {
                final ICompleteness.Transient ownCriticalEdges = Completeness.Transient.of();
                final ITerm scopeOrVar;
                if((scopeOrVar = scopeOrVar().match(cnew.scopeTerm()).orElse(null)) != null) {
                    ownCriticalEdges.add(scopeOrVar, EdgeOrData.data(), PersistentUniDisunifier.Immutable.of());
                    criticalEdge.apply(scopeOrVar, EdgeOrData.data());
                }
                return cnew.withArguments(cnew.scopeTerm(), cnew.datumTerm()).withOwnCriticalEdges(ownCriticalEdges.freeze());
            },
            iresolveQuery -> {
                final QueryFilter newFilter =
                        new QueryFilter(iresolveQuery.filter().getLabelWF(), precomputeCriticalEdges(iresolveQuery.filter().getDataWF(), spec));
                final QueryMin newMin =
                        new QueryMin(iresolveQuery.min().getLabelOrder(), precomputeCriticalEdges(iresolveQuery.min().getDataEquiv(), spec));
                return iresolveQuery.match(new IResolveQuery.Cases<IResolveQuery>() {

                    @Override public IResolveQuery caseResolveQuery(CResolveQuery q) {
                        return q.withArguments(newFilter, newMin, q.project(), q.scopeTerm(), q.resultTerm());
                    }

                    @Override public IResolveQuery caseCompiledQuery(CCompiledQuery q) {
                        return q.withArguments(newFilter, newMin, q.project(), q.scopeTerm(), q.resultTerm(), q.stateMachine());
                    }});
            },
            ctellEdge -> {
                final ICompleteness.Transient ownCriticalEdges = Completeness.Transient.of();
                final ITerm scopeOrVar;
                if((scopeOrVar = scopeOrVar().match(ctellEdge.sourceTerm()).orElse(null)) != null) {
                    ownCriticalEdges.add(scopeOrVar, EdgeOrData.edge(ctellEdge.label()), PersistentUniDisunifier.Immutable.of());
                    criticalEdge.apply(scopeOrVar, EdgeOrData.edge(ctellEdge.label()));
                }
                return ctellEdge.withArguments(ctellEdge.sourceTerm(), ctellEdge.label(), ctellEdge.targetTerm())
                        .withOwnCriticalEdges(ownCriticalEdges.freeze());
            },
            ctermId -> ctermId,
            ctermProperty -> ctermProperty,
            ctrue -> ctrue,
            ctry -> {
                final IConstraint newBody = precomputeCriticalEdges(ctry.constraint(), spec, criticalEdge);
                return ctry.withArguments(newBody);
            },
            cuser -> {
                final ICompleteness.Transient ownCriticalEdges = Completeness.Transient.of();
                spec.get(cuser.name()).forEach(il -> {
                    final ITerm scopeOrVar;
                    if((scopeOrVar = scopeOrVar().match(cuser.args().get(il._1())).orElse(null)) != null) {
                        final EdgeOrData<ITerm> label = EdgeOrData.edge(il._2());
                        ownCriticalEdges.add(scopeOrVar, label, PersistentUniDisunifier.Immutable.of());
                        criticalEdge.apply(scopeOrVar, label);
                    }
                });
                return cuser.withArguments(cuser.name(), cuser.args()).withOwnCriticalEdges(ownCriticalEdges.freeze());
            }
        ));
        // @formatter:on
    }

}
