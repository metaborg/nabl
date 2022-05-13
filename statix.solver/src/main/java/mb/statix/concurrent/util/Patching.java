package mb.statix.concurrent.util;

import static mb.nabl2.terms.matching.TermPattern.P;

import javax.annotation.Nullable;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.unit.Unit;

import com.google.common.collect.ImmutableList;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.IBlobTerm;
import mb.nabl2.terms.IConsTerm;
import mb.nabl2.terms.IIntTerm;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.INilTerm;
import mb.nabl2.terms.IStringTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.ApplPattern;
import mb.nabl2.terms.matching.ConsPattern;
import mb.nabl2.terms.matching.Pattern;
import mb.nabl2.terms.matching.PatternAs;
import mb.nabl2.terms.matching.StringPattern;
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
        final LazyScopeSetSupplier scopes = new LazyScopeSetSupplier();
        ruleScopes(rule, scopes);
        return scopes.freeze();
    }

    public static Set.Immutable<Scope> constraintScopes(IConstraint constraint) {
        final LazyScopeSetSupplier scopes = new LazyScopeSetSupplier();
        constraintScopes(constraint, scopes);
        return scopes.freeze();
    }

    public static Set.Immutable<Scope> termScopes(ITerm term) {
        final LazyScopeSetSupplier scopes = new LazyScopeSetSupplier();
        termScopes(term, scopes);
        return scopes.freeze();
    }

    public static Set.Immutable<Scope> patternScopes(Pattern pattern) {
        final LazyScopeSetSupplier scopes = new LazyScopeSetSupplier();
        patternScopes(pattern, scopes);
        return scopes.freeze();
    }

    private static void ruleScopes(Rule rule, LazyScopeSetSupplier scopes) {
        for(Pattern pattern : rule.params()) {
            patternScopes(pattern, scopes);
        }
        constraintScopes(rule.body(), scopes);
    }

    private static void constraintScopes(IConstraint constraint, LazyScopeSetSupplier scopes) {
        constraint.match(new Cases<Unit>() {

            @Override public Unit caseArith(CArith c) {
                return Unit.unit;
            }

            @Override public Unit caseConj(CConj c) {
                constraintScopes(c.left(), scopes);
                constraintScopes(c.right(), scopes);
                return Unit.unit;
            }

            @Override public Unit caseEqual(CEqual c) {
                termScopes(c.term1(), scopes);
                termScopes(c.term2(), scopes);
                return Unit.unit;
            }

            @Override public Unit caseExists(CExists c) {
                constraintScopes(c.constraint(), scopes);
                return Unit.unit;
            }

            @Override public Unit caseFalse(CFalse c) {
                return Unit.unit;
            }

            @Override public Unit caseInequal(CInequal c) {
                termScopes(c.term1(), scopes);
                termScopes(c.term2(), scopes);
                return Unit.unit;
            }

            @Override public Unit caseNew(CNew c) {
                termScopes(c.datumTerm(), scopes);
                return Unit.unit;
            }

            @Override public Unit caseResolveQuery(IResolveQuery c) {
                termScopes(c.scopeTerm(), scopes);
                termScopes(c.resultTerm(), scopes);

                ruleScopes(c.filter().getDataWF(), scopes);
                ruleScopes(c.min().getDataEquiv(), scopes);

                return Unit.unit;
            }

            @Override public Unit caseTellEdge(CTellEdge c) {
                termScopes(c.sourceTerm(), scopes);
                termScopes(c.targetTerm(), scopes);
                return Unit.unit;
            }

            @Override public Unit caseTermId(CAstId c) {
                termScopes(c.astTerm(), scopes);
                termScopes(c.idTerm(), scopes);
                return Unit.unit;
            }

            @Override public Unit caseTermProperty(CAstProperty c) {
                termScopes(c.idTerm(), scopes);
                termScopes(c.value(), scopes);
                return Unit.unit;
            }

            @Override public Unit caseTrue(CTrue c) {
                return Unit.unit;
            }

            @Override public Unit caseTry(CTry c) {
                constraintScopes(c.constraint(), scopes);
                return Unit.unit;
            }

            @Override public Unit caseUser(CUser c) {
                for(ITerm arg : c.args()) {
                    termScopes(arg, scopes);
                }
                return Unit.unit;
            }
        });
    }

    private static void termScopes(ITerm term, LazyScopeSetSupplier scopes) {
        term.match(new ITerm.Cases<Unit>() {

            @Override public Unit caseAppl(IApplTerm appl) {
                if(appl instanceof Scope) {
                    scopes.get().__insert((Scope) appl);
                } else if(appl.getOp().equals("Scope") && appl.getArity() == 2) {
                    final ITerm arg1 = appl.getArgs().get(0);
                    final ITerm arg2 = appl.getArgs().get(1);

                    if(arg1 instanceof IStringTerm && arg2 instanceof IStringTerm) {
                        final String resource = ((IStringTerm) arg1).getValue();
                        final String id = ((IStringTerm) arg2).getValue();
                        scopes.get().__insert(Scope.of(resource, id));
                    } else {
                        termScopes(arg1, scopes);
                        termScopes(arg2, scopes);
                    }
                } else {
                    for(ITerm arg: appl.getArgs()) {
                        termScopes(arg, scopes);
                    }
                }

                return Unit.unit;
            }

            @Override public Unit caseBlob(IBlobTerm integer) {
                return Unit.unit;
            }

            @Override public Unit caseInt(IIntTerm integer) {
                return Unit.unit;
            }

            @Override public Unit caseList(IListTerm list) {
                listTermScopes(list, scopes);
                return Unit.unit;
            }

            @Override public Unit caseString(IStringTerm string) {
                return Unit.unit;
            }

            @Override public Unit caseVar(ITermVar var) {
                return Unit.unit;
            }

        });
    }

    private static void listTermScopes(IListTerm list, LazyScopeSetSupplier scopes) {
        list.match(new IListTerm.Cases<Unit>() {

            @Override public Unit caseCons(IConsTerm cons) {
                termScopes(cons.getHead(), scopes);
                listTermScopes(cons.getTail(), scopes);

                return Unit.unit;
            }

            @Override public Unit caseNil(INilTerm nil) {
                return Unit.unit;
            }

            @Override public Unit caseVar(ITermVar var) {
                return Unit.unit;
            }

        });
    }

    private static void patternScopes(Pattern pattern, LazyScopeSetSupplier scopes) {
        if(pattern instanceof ApplPattern) {
            final ApplPattern appl = (ApplPattern) pattern;
            if(appl.getOp().equals("Scope") && appl.getArgs().size() == 2) {
                final Pattern arg1 = appl.getArgs().get(0);
                final Pattern arg2 = appl.getArgs().get(1);

                if(arg1 instanceof StringPattern && arg2 instanceof StringPattern) {
                    final String resource = ((StringPattern) arg1).getValue();
                    final String id = ((StringPattern) arg2).getValue();

                    scopes.get().__insert(Scope.of(resource, id));
                }
            } else {
                for(Pattern arg : appl.getArgs()) {
                    patternScopes(arg, scopes);
                }
            }
        } else if(pattern instanceof ConsPattern) {
            final ConsPattern cons = (ConsPattern) pattern;
            patternScopes(cons.getHead(), scopes);
            patternScopes(cons.getTail(), scopes);
        } else if(pattern instanceof PatternAs) {
            final PatternAs as = (PatternAs) pattern;
            patternScopes(as.getPattern(), scopes);
        } // else cannot contain scope
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
                        return new CCompiledQuery(newFilter, newMin, newScopeTerm, newResultTerm, cause, message,
                                q.stateMachine());
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
                final ImmutableList<ITerm> newArgs =
                        c.args().stream().map(arg -> patch(arg, patches)).collect(ImmutableList.toImmutableList());
                // TODO Patch ownCriticalEdges?
                return new CUser(c.name(), newArgs, c.cause().orElse(null), c.message().orElse(null),
                        c.ownCriticalEdges().orElse(null));
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

    private static final class LazyScopeSetSupplier {

        private Set.Transient<Scope> value;

        public Set.Transient<Scope> get() {
            Set.Transient<Scope> result = value;
            if(value == null) {
                result = CapsuleUtil.transientSet();
                value = result;
            }
            return result;
        }

        public Set.Immutable<Scope> freeze() {
            final Set.Transient<Scope> value = this.value;
            return value == null ? CapsuleUtil.immutableSet() : value.freeze();
        }


    }

}
