package mb.statix.concurrent.util;

import static mb.nabl2.terms.matching.TermPattern.P;

import javax.annotation.Nullable;

import org.metaborg.util.collection.CapsuleUtil;

import com.google.common.collect.ImmutableList;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.Set.Immutable;
import io.usethesource.capsule.util.stream.CapsuleCollectors;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.build.TermVar;
import mb.nabl2.terms.matching.Pattern;
import mb.nabl2.terms.matching.Transform.T;
import mb.nabl2.terms.substitution.FreshVars;
import mb.scopegraph.patching.IPatchCollection;
import mb.statix.constraints.CArith;
import mb.statix.constraints.CAstId;
import mb.statix.constraints.CAstProperty;
import mb.statix.constraints.CCompiledQuery;
import mb.statix.constraints.CConj;
import mb.statix.constraints.CEqual;
import mb.statix.constraints.CExists;
import mb.statix.constraints.CFalse;
import mb.statix.constraints.CInequal;
import mb.statix.constraints.CNew;
import mb.statix.constraints.CResolveQuery;
import mb.statix.constraints.CTellEdge;
import mb.statix.constraints.CTrue;
import mb.statix.constraints.CTry;
import mb.statix.constraints.CUser;
import mb.statix.constraints.IResolveQuery;
import mb.statix.constraints.messages.IMessage;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IConstraint.Cases;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.solver.query.QueryFilter;
import mb.statix.solver.query.QueryMin;
import mb.statix.spec.Rule;

public class Patching {

    // Collect domain of scopes.

    public static Set.Immutable<Scope> ruleScopes(Rule rule) {
        // @formatter:off
        final Set.Immutable<Scope> headScopes = rule.params().stream()
            .map(Patching::patternScopes)
            .flatMap(Set.Immutable::stream)
            .collect(CapsuleCollectors.toSet());
        // @formatter:on

        final Set.Immutable<Scope> bodyScopes = constraintScopes(rule.body());

        return headScopes.__insertAll(bodyScopes);
    }

    public static Set.Immutable<Scope> constraintScopes(IConstraint constraint) {
        return constraint.match(new Cases<Set.Immutable<Scope>>() {

            @Override public Immutable<Scope> caseArith(CArith c) {
                return CapsuleUtil.immutableSet();
            }

            @Override public Immutable<Scope> caseConj(CConj c) {
                return constraintScopes(c.left()).__insertAll(constraintScopes(c.right()));
            }

            @Override public Immutable<Scope> caseEqual(CEqual c) {
                return termScopes(c.term1()).__insertAll(termScopes(c.term2()));
            }

            @Override public Immutable<Scope> caseExists(CExists c) {
                return constraintScopes(c.constraint());
            }

            @Override public Immutable<Scope> caseFalse(CFalse c) {
                return CapsuleUtil.immutableSet();
            }

            @Override public Immutable<Scope> caseInequal(CInequal c) {
                return termScopes(c.term1()).__insertAll(termScopes(c.term2()));
            }

            @Override public Immutable<Scope> caseNew(CNew c) {
                return termScopes(c.datumTerm());
            }

            @Override public Immutable<Scope> caseResolveQuery(IResolveQuery c) {
                final Set.Immutable<Scope> scopeTermScopes = termScopes(c.scopeTerm());
                final Set.Immutable<Scope> resultTermScopes = termScopes(c.resultTerm());

                final Set.Immutable<Scope> dataWfScopes = ruleScopes(c.filter().getDataWF());
                final Set.Immutable<Scope> dataEquivScopes = ruleScopes(c.min().getDataEquiv());

                return scopeTermScopes.__insertAll(resultTermScopes).__insertAll(dataWfScopes)
                        .__insertAll(dataEquivScopes);
            }

            @Override public Immutable<Scope> caseTellEdge(CTellEdge c) {
                return termScopes(c.sourceTerm()).__insertAll(termScopes(c.targetTerm()));
            }

            @Override public Immutable<Scope> caseTermId(CAstId c) {
                return termScopes(c.astTerm()).__insertAll(termScopes(c.idTerm()));
            }

            @Override public Immutable<Scope> caseTermProperty(CAstProperty c) {
                return termScopes(c.idTerm()).__insertAll(termScopes(c.value()));
            }

            @Override public Immutable<Scope> caseTrue(CTrue c) {
                return CapsuleUtil.immutableSet();
            }

            @Override public Immutable<Scope> caseTry(CTry c) {
                return constraintScopes(c.constraint());
            }

            @Override public Immutable<Scope> caseUser(CUser c) {
                return c.args().stream().map(Patching::termScopes).flatMap(Set.Immutable::stream)
                        .collect(CapsuleCollectors.toSet());
            }
        });
    }

    public static Set.Immutable<Scope> termScopes(ITerm term) {
        return CapsuleUtil.toSet(T.collecttd(Scope.matcher()::match).apply(term));
    }

    public static Set.Immutable<Scope> patternScopes(Pattern pattern) {
        return CapsuleUtil.toSet(termScopes(pattern.asTerm(var -> var.orElse(TermVar.of("", "wld")))._1()));
    }

    // Apply patch collection to rule/constraint/pattern/term

    public static Rule patch(Rule rule, IPatchCollection<Scope> patches) {
        // @formatter:off
        final ImmutableList<Pattern> newParams = rule.params().stream()
            .map(p -> patch(p, patches))
            .collect(ImmutableList.toImmutableList());
        // @formatter:on

        final IConstraint newBody = patch(rule.body(), patches);

        return rule.withParams(newParams).withBody(newBody);
    }

    public static IConstraint patch(IConstraint constraint, IPatchCollection<Scope> patches) {
        return constraint.match(new Cases<IConstraint>() {

            @Override public IConstraint caseArith(CArith c) {
                return c;
            }

            @Override public IConstraint caseConj(CConj c) {
                final IConstraint newLeft = patch(c.left(), patches);
                final IConstraint newRight = patch(c.right(), patches);

                return new CConj(newLeft, newRight, c.cause().orElse(null));
            }

            @Override public IConstraint caseEqual(CEqual c) {
                final ITerm newTerm1 = patch(c.term1(), patches);
                final ITerm newTerm2 = patch(c.term2(), patches);

                return new CEqual(newTerm1, newTerm2, c.cause().orElse(null), c.message().orElse(null));
            }

            @Override public IConstraint caseExists(CExists c) {
                // TODO: preserve free vars?
                return c.withConstraint(patch(c.constraint(), patches));
            }

            @Override public IConstraint caseFalse(CFalse c) {
                return c;
            }

            @Override public IConstraint caseInequal(CInequal c) {
                final ITerm newTerm1 = patch(c.term1(), patches);
                final ITerm newTerm2 = patch(c.term2(), patches);

                final @Nullable IConstraint cause = c.cause().orElse(null);
                final @Nullable IMessage message = c.message().orElse(null);

                return new CInequal(c.universals(), newTerm1, newTerm2, cause, message);
            }

            @Override public IConstraint caseNew(CNew c) {
                final ITerm newScopeTerm = patch(c.scopeTerm(), patches);
                final ITerm newDatumTerm = patch(c.datumTerm(), patches);

                return new CNew(newScopeTerm, newDatumTerm, c.cause().orElse(null), c.ownCriticalEdges().orElse(null));
            }

            @Override public IConstraint caseResolveQuery(IResolveQuery c) {
                final ITerm newScopeTerm = patch(c.scopeTerm(), patches);
                final ITerm newResultTerm = patch(c.resultTerm(), patches);

                final Rule newDataWf = patch(c.filter().getDataWF(), patches);
                final QueryFilter newFilter = new QueryFilter(c.filter().getLabelWF(), newDataWf);

                final Rule newDataEquiv = patch(c.min().getDataEquiv(), patches);
                final QueryMin newMin = new QueryMin(c.min().getLabelOrder(), newDataEquiv);

                final @Nullable IConstraint cause = c.cause().orElse(null);
                final @Nullable IMessage message = c.message().orElse(null);

                return c.match(new IResolveQuery.Cases<IResolveQuery>() {
                    @Override public IResolveQuery caseResolveQuery(CResolveQuery q) {
                        return new CResolveQuery(newFilter, newMin, newScopeTerm, newResultTerm, cause, message);
                    }

                    @Override public IResolveQuery caseCompiledQuery(CCompiledQuery q) {
                        return new CCompiledQuery(newFilter, newMin, newScopeTerm, newResultTerm, cause, message, q.stateMachine());
                    }
                });
            }

            @Override public IConstraint caseTellEdge(CTellEdge c) {
                // TODO: patch critical edges?
                final ITerm newSourceTerm = patch(c.sourceTerm(), patches);
                final ITerm newTargetTerm = patch(c.targetTerm(), patches);

                final @Nullable IConstraint cause = c.cause().orElse(null);
                final @Nullable ICompleteness.Immutable bodyCriticalEdges = c.bodyCriticalEdges().orElse(null);

                return new CTellEdge(newSourceTerm, c.label(), newTargetTerm, cause, bodyCriticalEdges);
            }

            @Override public IConstraint caseTermId(CAstId c) {
                final ITerm newAstTerm = patch(c.astTerm(), patches);
                final ITerm newIdTerm = patch(c.idTerm(), patches);

                return new CAstId(newAstTerm, newIdTerm, c.cause().orElse(null));
            }

            @Override public IConstraint caseTermProperty(CAstProperty c) {
                final ITerm newIdTerm = patch(c.idTerm(), patches);
                final ITerm newValue = patch(c.value(), patches);

                return new CAstProperty(newIdTerm, c.property(), c.op(), newValue, c.cause().orElse(null));
            }

            @Override public IConstraint caseTrue(CTrue c) {
                return c;
            }

            @Override public IConstraint caseTry(CTry c) {
                final IConstraint newConstraint = patch(c.constraint(), patches);

                return new CTry(newConstraint, c.cause().orElse(null), c.message().orElse(null));
            }

            @Override public IConstraint caseUser(CUser c) {
                final ImmutableList<ITerm> newArgs = c.args().stream().map(arg -> patch(arg, patches)).collect(ImmutableList.toImmutableList());
                // TODO Patch ownCriticalEdges?
                return new CUser(c.name(), newArgs, c.cause().orElse(null), c.message().orElse(null), c.ownCriticalEdges().orElse(null));
            }
        });
    }

    public static Pattern patch(Pattern pattern, IPatchCollection<Scope> patches) {
        final FreshVars fresh = new FreshVars(pattern.getVars());
        final ITerm patternTerm = pattern.asTerm(var -> var.orElseGet(() -> fresh.fresh("wld")))._1();
        final Set.Immutable<ITermVar> wildCards = fresh.reset();

        final ITerm patchedTerm = patch(patternTerm, patches);
        return P.fromTerm(patchedTerm, wildCards::contains);
    }

    public static ITerm patch(ITerm term, IPatchCollection<Scope> patches) {
        return T.sometd(Scope.matcher().<ITerm>map(patches::patch)::match).apply(term);
    }

}
