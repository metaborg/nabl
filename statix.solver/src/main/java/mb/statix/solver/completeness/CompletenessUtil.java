package mb.statix.solver.completeness;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Collection;

import org.metaborg.util.functions.Action2;
import org.metaborg.util.tuple.Tuple2;
import org.metaborg.util.unit.Unit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.SetMultimap;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.terms.unification.ud.PersistentUniDisunifier;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.statix.constraints.CCompiledQuery;
import mb.statix.constraints.CConj;
import mb.statix.constraints.CExists;
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
        switch(constraint.constraintTag()) {
            case CConj: {
                CConj onConj = (CConj) constraint;
                Constraints.disjoin(onConj).forEach(c -> criticalEdges(c, spec, criticalEdge));
                break;
            }
            case CExists: {
                CExists onExists = (CExists) constraint;
                criticalEdges(onExists.constraint(), spec, (s, l) -> {
                    if(!onExists.vars().contains(s)) {
                        criticalEdge.apply(s, l);
                    }
                });
                break;
            }
            case CNew: {
                CNew onNew = (CNew) constraint;
                criticalEdge.apply(onNew.scopeTerm(), EdgeOrData.data());
                break;
            }
            case CTellEdge: {
                CTellEdge onTellEdge = (CTellEdge) constraint;
                criticalEdge.apply(onTellEdge.sourceTerm(), EdgeOrData.edge(onTellEdge.label()));
                break;
            }
            case CUser: {
                CUser onUser = (CUser) constraint;
                spec.scopeExtensions().get(onUser.name()).stream()
                    .forEach(il -> criticalEdge.apply(onUser.args().get(il._1()), EdgeOrData.edge(il._2())));
                break;
            }
            case CArith:
            case CEqual:
            case CFalse:
            case CInequal:
            case IResolveQuery:
            case CAstId:
            case CAstProperty:
            case CTrue:
            case CTry:
                break;
        }
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
        switch(constraint.constraintTag()) {
            case CConj: {
                CConj cconj = (CConj) constraint;
                final IConstraint newLeft = precomputeCriticalEdges(cconj.left(), spec, criticalEdge);
                final IConstraint newRight = precomputeCriticalEdges(cconj.right(), spec, criticalEdge);
                return new CConj(newLeft, newRight, cconj.cause().orElse(null));
            }
            case CExists: {
                CExists cexists = (CExists) constraint;
                final ICompleteness.Transient bodyCriticalEdges = Completeness.Transient.of();
                final IConstraint newBody = precomputeCriticalEdges(cexists.constraint(), spec, (s, l) -> {
                    if(cexists.vars().contains(s)) {
                        bodyCriticalEdges.add(s, l, PersistentUniDisunifier.Immutable.of());
                    } else {
                        criticalEdge.apply(s, l);
                    }
                });
                return cexists.withConstraint(newBody).withBodyCriticalEdges(bodyCriticalEdges.freeze());
            }
            case CNew: {
                CNew cnew = (CNew) constraint;
                final ICompleteness.Transient ownCriticalEdges = Completeness.Transient.of();
                final ITerm scopeOrVar;
                if((scopeOrVar = scopeOrVar().match(cnew.scopeTerm()).orElse(null)) != null) {
                    ownCriticalEdges.add(scopeOrVar, EdgeOrData.data(), PersistentUniDisunifier.Immutable.of());
                    criticalEdge.apply(scopeOrVar, EdgeOrData.data());
                }
                return new CNew(cnew.scopeTerm(), cnew.datumTerm(), cnew.cause().orElse(null), ownCriticalEdges.freeze());
            }
            case IResolveQuery: {
                IResolveQuery iresolveQuery = (IResolveQuery) constraint;
                final QueryFilter newFilter =
                    new QueryFilter(iresolveQuery.filter().getLabelWF(), precomputeCriticalEdges(iresolveQuery.filter().getDataWF(), spec));
                final QueryMin newMin =
                    new QueryMin(iresolveQuery.min().getLabelOrder(), precomputeCriticalEdges(iresolveQuery.min().getDataEquiv(), spec));
                switch(iresolveQuery.resolveQueryTag()) {
                    case CResolveQuery: { CResolveQuery q = (CResolveQuery) iresolveQuery;
                        return new CResolveQuery(newFilter, newMin, q.scopeTerm(), q.resultTerm(),
                            q.cause().orElse(null), q.message().orElse(null));
                    }

                    case CCompiledQuery: { CCompiledQuery q = (CCompiledQuery) iresolveQuery;
                        return new CCompiledQuery(newFilter, newMin, q.scopeTerm(), q.resultTerm(),
                            q.cause().orElse(null), q.message().orElse(null), q.stateMachine());
                    }
                }
            }
            case CTellEdge: {
                CTellEdge ctellEdge = (CTellEdge) constraint;
                final ICompleteness.Transient ownCriticalEdges = Completeness.Transient.of();
                final ITerm scopeOrVar;
                if((scopeOrVar = scopeOrVar().match(ctellEdge.sourceTerm()).orElse(null)) != null) {
                    ownCriticalEdges.add(scopeOrVar, EdgeOrData.edge(ctellEdge.label()), PersistentUniDisunifier.Immutable.of());
                    criticalEdge.apply(scopeOrVar, EdgeOrData.edge(ctellEdge.label()));
                }
                return new CTellEdge(ctellEdge.sourceTerm(), ctellEdge.label(), ctellEdge.targetTerm(),
                    ctellEdge.cause().orElse(null), ownCriticalEdges.freeze());
            }
            case CTry: {
                CTry ctry = (CTry) constraint;
                final IConstraint newBody = precomputeCriticalEdges(ctry.constraint(), spec, criticalEdge);
                return new CTry(newBody, ctry.cause().orElse(null), ctry.message().orElse(null));
            }
            case CUser: {
                CUser cuser = (CUser) constraint;
                final ICompleteness.Transient ownCriticalEdges = Completeness.Transient.of();
                spec.get(cuser.name()).stream().forEach(il -> {
                    final ITerm scopeOrVar;
                    if((scopeOrVar = scopeOrVar().match(cuser.args().get(il._1())).orElse(null)) != null) {
                        final EdgeOrData<ITerm> label = EdgeOrData.edge(il._2());
                        ownCriticalEdges.add(scopeOrVar, label, PersistentUniDisunifier.Immutable.of());
                        criticalEdge.apply(scopeOrVar, label);
                    }
                });
                return new CUser(cuser.name(), cuser.args(), cuser.cause().orElse(null), cuser.message().orElse(null), ownCriticalEdges.freeze());
            }
            case CArith:
            case CEqual:
            case CFalse:
            case CInequal:
            case CAstId:
            case CAstProperty:
            case CTrue: {
                return constraint;
            }
        }
        // N.B. don't use this in default case branch, instead use IDE to catch non-exhaustive switch statements
        throw new RuntimeException("Missing case for IConstraint subclass/tag");
    }

}
