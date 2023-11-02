package mb.statix.concurrent.util;

import static mb.nabl2.terms.matching.TermPattern.P;
import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;

import jakarta.annotation.Nullable;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.ImList;
import org.metaborg.util.unit.Unit;

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
import mb.statix.solver.query.QueryProject;
import mb.statix.spec.Rule;

public class Patching {

    private static final String SCOPE_OP = "Scope";

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
                } else if(appl.getOp().equals(SCOPE_OP) && appl.getArity() == 2) {
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
                    for(ITerm arg : appl.getArgs()) {
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
            if(appl.getOp().equals(SCOPE_OP) && appl.getArgs().size() == 2) {
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
        final List<Pattern> params = rule.params();
        ImList.Mutable<Pattern> newParamsBuilder = null;
        for(int i = 0; i < params.size(); i++) {
            final Pattern param = params.get(i);
            final Pattern newParam = patch(param, patches);
            if(newParam != null) {
                if(newParamsBuilder == null) {
                    newParamsBuilder = new ImList.Mutable<>(params.size());
                    for(int j = 0; j < i; j++) {
                        newParamsBuilder.add(params.get(j));
                    }
                }
                newParamsBuilder.add(newParam);
            } else if(newParamsBuilder != null) {
                newParamsBuilder.add(param);
            }
        }

        final IConstraint newBody = patch(rule.body(), patches);
        if(newParamsBuilder == null && newBody == null) {
            // no change
            return null;
        }

        Rule result = rule;
        if(newParamsBuilder != null) {
            result = result.withParams(newParamsBuilder.freeze());
        }
        if(newBody != null) {
            result = result.withBody(newBody);
        }

        return result;
    }

    public static IConstraint patch(IConstraint constraint, IPatchCollection<Scope> patches) {
        return constraint.match(new Cases<IConstraint>() {

            @Override public IConstraint caseArith(CArith c) {
                return null;
            }

            @Override public IConstraint caseConj(CConj c) {
                IConstraint newLeft = patch(c.left(), patches);
                IConstraint newRight = patch(c.right(), patches);

                if(newLeft == null && newRight == null) {
                    return null;
                }

                newLeft = newLeft == null ? c.left() : newLeft;
                newRight = newRight == null ? c.right() : newRight;

                return c.withArguments(newLeft, newRight);
            }

            @Override public IConstraint caseEqual(CEqual c) {
                ITerm newTerm1 = patch(c.term1(), patches);
                ITerm newTerm2 = patch(c.term2(), patches);

                if(newTerm1 == null && newTerm2 == null) {
                    return null;
                }

                newTerm1 = newTerm1 == null ? c.term1() : newTerm1;
                newTerm2 = newTerm2 == null ? c.term2() : newTerm2;

                return c.withArguments(newTerm1, newTerm2);
            }

            @Override public IConstraint caseExists(CExists c) {
                // TODO: preserve free vars?
                final IConstraint newConstraint = patch(c.constraint(), patches);
                return newConstraint == null ? null : c.withArguments(c.vars(), newConstraint);
            }

            @Override public IConstraint caseFalse(CFalse c) {
                return null;
            }

            @Override public IConstraint caseInequal(CInequal c) {
                ITerm newTerm1 = patch(c.term1(), patches);
                ITerm newTerm2 = patch(c.term2(), patches);

                if(newTerm1 == null && newTerm2 == null) {
                    return null;
                }

                newTerm1 = newTerm1 == null ? c.term1() : newTerm1;
                newTerm2 = newTerm2 == null ? c.term2() : newTerm2;

                return c.withArguments(c.universals(), newTerm1, newTerm2);
            }

            @Override public IConstraint caseNew(CNew c) {
                ITerm newScopeTerm = patch(c.scopeTerm(), patches);
                ITerm newDatumTerm = patch(c.datumTerm(), patches);

                if(newScopeTerm == null && newDatumTerm == null) {
                    return null;
                }

                newScopeTerm = newScopeTerm == null ? c.scopeTerm() : newScopeTerm;
                newDatumTerm = newDatumTerm == null ? c.datumTerm() : newDatumTerm;

                return c.withArguments(newScopeTerm, newDatumTerm);
            }

            @Override public IConstraint caseResolveQuery(IResolveQuery c) {
                final ITerm scopeTerm = patch(c.scopeTerm(), patches);
                final ITerm resultTerm = patch(c.resultTerm(), patches);

                final Rule newDataWf = patch(c.filter().getDataWF(), patches);
                final Rule newDataEquiv = patch(c.min().getDataEquiv(), patches);

                if(scopeTerm == null && resultTerm == null && newDataWf == null && newDataEquiv == null) {
                    return null;
                }

                final ITerm newScopeTerm = scopeTerm == null ? c.scopeTerm() : scopeTerm;
                final ITerm newResultTerm = resultTerm == null ? c.resultTerm() : resultTerm;
                final QueryFilter newFilter = new QueryFilter(c.filter().getLabelWF(),
                        newDataWf == null ? c.filter().getDataWF() : newDataWf);
                final QueryMin newMin = new QueryMin(c.min().getLabelOrder(),
                        newDataEquiv == null ? c.min().getDataEquiv() : newDataEquiv);
                final QueryProject project = c.project();

                return c.match(new IResolveQuery.Cases<IResolveQuery>() {
                    @Override public IResolveQuery caseResolveQuery(CResolveQuery q) {
                        return q.withArguments(newFilter, newMin, project, newScopeTerm, newResultTerm);
                    }

                    @Override public IResolveQuery caseCompiledQuery(CCompiledQuery q) {
                        return q.withArguments(newFilter, newMin, project, newScopeTerm, newResultTerm, q.stateMachine());
                    }
                });
            }

            @Override public IConstraint caseTellEdge(CTellEdge c) {
                // TODO: patch critical edges?
                ITerm newSourceTerm = patch(c.sourceTerm(), patches);
                ITerm newTargetTerm = patch(c.targetTerm(), patches);

                if(newSourceTerm == null && newTargetTerm == null) {
                    return null;
                }

                newSourceTerm = newSourceTerm == null ? c.sourceTerm() : newSourceTerm;
                newTargetTerm = newTargetTerm == null ? c.targetTerm() : newTargetTerm;

                return c.withArguments(newSourceTerm, c.label(), newTargetTerm);
            }

            @Override public IConstraint caseTermId(CAstId c) {
                ITerm newAstTerm = patch(c.astTerm(), patches);
                ITerm newIdTerm = patch(c.idTerm(), patches);

                if(newAstTerm == null && newIdTerm == null) {
                    return null;
                }

                newAstTerm = newAstTerm == null ? c.astTerm() : newAstTerm;
                newIdTerm = newIdTerm == null ? c.idTerm() : newIdTerm;

                return c.withArguments(newAstTerm, newIdTerm);
            }

            @Override public IConstraint caseTermProperty(CAstProperty c) {
                ITerm newIdTerm = patch(c.idTerm(), patches);
                ITerm newValue = patch(c.value(), patches);

                if(newIdTerm == null && newValue == null) {
                    return null;
                }

                newIdTerm = newIdTerm == null ? c.idTerm() : newIdTerm;
                newValue = newValue == null ? c.value() : newValue;

                return c.withArguments(newIdTerm, c.property(), c.op(), newValue);
            }

            @Override public IConstraint caseTrue(CTrue c) {
                return null;
            }

            @Override public IConstraint caseTry(CTry c) {
                final IConstraint newConstraint = patch(c.constraint(), patches);

                return newConstraint == null ? null : c.withArguments(newConstraint);
            }

            @Override public IConstraint caseUser(CUser c) {
                final List<ITerm> args = c.args();
                final int size = args.size();
                ImList.Mutable<ITerm> newArgsBuilder = null;
                for(int i = 0; i < size; i++) {
                    final ITerm arg = args.get(i);
                    final ITerm newArg = patch(arg, patches);
                    if(newArg != null) {
                        if(newArgsBuilder == null) {
                            newArgsBuilder = new ImList.Mutable<>(size);
                            for(int j = 0; j < i; j++) {
                                newArgsBuilder.add(args.get(j));
                            }
                        }
                        newArgsBuilder.add(newArg);
                    } else if(newArgsBuilder != null) {
                        newArgsBuilder.add(arg);
                    }
                }

                if(newArgsBuilder == null) {
                    return null;
                }

                // TODO Patch ownCriticalEdges?
                return c.withArguments(c.name(), newArgsBuilder.freeze())
                        .withOwnCriticalEdges(c.ownCriticalEdges().orElse(null));
            }
        });
    }

    public static Pattern patch(Pattern pattern, IPatchCollection<Scope> patches) {
        if(pattern instanceof ApplPattern) {
            final ApplPattern appl = (ApplPattern) pattern;
            final List<Pattern> args = appl.getArgs();
            final int size = args.size();
            if(appl.getOp().equals(SCOPE_OP) && size == 2) {
                final Pattern arg1 = appl.getArgs().get(0);
                final Pattern arg2 = appl.getArgs().get(1);

                if(arg1 instanceof StringPattern && arg2 instanceof StringPattern) {
                    final String resource = ((StringPattern) arg1).getValue();
                    final String id = ((StringPattern) arg2).getValue();

                    final Scope scope = Scope.of(resource, id);
                    final Scope newScope = patches.patch(scope);

                    if(scope == newScope) {
                        return null;
                    }

                    final ImList.Immutable<Pattern> newArgs =
                        ImList.Immutable.of(P.newString(newScope.getResource(), arg1.getAttachments()),
                                    P.newString(newScope.getName(), arg2.getAttachments()));
                    return P.newAppl(SCOPE_OP, newArgs, appl.getAttachments());
                }
            }

            ImList.Mutable<Pattern> newArgsBuilder = null;
            for(int i = 0; i < size; i++) {
                final Pattern arg = args.get(i);
                final Pattern newArg = patch(arg, patches);
                if(newArg != null) {
                    if(newArgsBuilder == null) {
                        newArgsBuilder = new ImList.Mutable<>(size);
                        for(int j = 0; j < i; j++) {
                            newArgsBuilder.add(args.get(j));
                        }
                    }
                    newArgsBuilder.add(newArg);
                } else if(newArgsBuilder != null) {
                    newArgsBuilder.add(arg);
                }
            }

            if(newArgsBuilder == null) {
                return null;
            }

            return P.newAppl(appl.getOp(), newArgsBuilder.freeze(), appl.getAttachments());
        } else if(pattern instanceof ConsPattern) {
            final ConsPattern cons = (ConsPattern) pattern;
            Pattern head = patch(cons.getHead(), patches);
            Pattern tail = patch(cons.getTail(), patches);

            if(head == null && tail == null) {
                return null;
            }

            head = head == null ? cons.getHead() : head;
            tail = tail == null ? cons.getTail() : tail;

            return P.newCons(head, tail, cons.getAttachments());
        } else if(pattern instanceof PatternAs) {
            final PatternAs as = (PatternAs) pattern;
            final Pattern sub = patch(as.getPattern(), patches);

            return sub == null ? null : P.newAs(as.getVar(), sub);
        } else {
            return null;
        }

    }

    public static ITerm patch(ITerm term, IPatchCollection<Scope> patches) {
        return term.match(new ITerm.Cases<ITerm>() {

            @Override public ITerm caseVar(ITermVar var) {
                return null;
            }

            @Override public ITerm caseString(IStringTerm string) {
                return null;
            }

            @Override public ITerm caseList(IListTerm cons) {
                return patch(cons, patches);
            }

            @Override public ITerm caseInt(IIntTerm integer) {
                return null;
            }

            @Override public ITerm caseBlob(IBlobTerm integer) {
                return null;
            }

            @Override public ITerm caseAppl(IApplTerm appl) {
                if(appl instanceof Scope) {
                    final Scope newScope = patches.patch((Scope) appl);
                    return newScope == appl ? null : newScope;
                } else if(appl.getOp().equals(SCOPE_OP) && appl.getArity() == 2) {
                    final ITerm arg1 = appl.getArgs().get(0);
                    final ITerm arg2 = appl.getArgs().get(1);

                    if(arg1 instanceof IStringTerm && arg2 instanceof IStringTerm) {
                        final String resource = ((IStringTerm) arg1).getValue();
                        final String name = ((IStringTerm) arg2).getValue();

                        final Scope oldScope = Scope.of(resource, name);
                        final Scope newScope = patches.patch(oldScope);

                        return newScope == appl ? null : newScope;
                    }
                } else if(appl.getArity() == 0) {
                    return null;
                }

                final List<ITerm> args = appl.getArgs();
                final int size = args.size();
                ImList.Mutable<ITerm> newArgsBuilder = null;

                for(int i = 0; i < size; i++) {
                    final ITerm arg = args.get(i);
                    final ITerm newArg = patch(arg, patches);
                    if(newArg != null) {
                        if(newArgsBuilder == null) {
                            newArgsBuilder = new ImList.Mutable<>(size);
                            for(int j = 0; j < i; j++) {
                                newArgsBuilder.add(args.get(j));
                            }
                        }
                        newArgsBuilder.add(newArg);
                    } else if(newArgsBuilder != null) {
                        newArgsBuilder.add(arg);
                    }
                }

                if(newArgsBuilder == null) {
                    return null;
                }

                return B.newAppl(appl.getOp(), newArgsBuilder.freeze(), appl.getAttachments());
            }

        });
    }

    public static IListTerm patch(IListTerm term, IPatchCollection<Scope> patches) {
        return term.match(new IListTerm.Cases<IListTerm>() {

            @Override public IListTerm caseCons(IConsTerm cons) {
                ITerm newHead = patch(cons.getHead(), patches);
                IListTerm newTail = patch(cons.getTail(), patches);

                if(newHead == null && newTail == null) {
                    return null;
                }

                newHead = newHead == null ? cons.getHead() : newHead;
                newTail = newTail == null ? cons.getTail() : newTail;

                return B.newCons(newHead, newTail, cons.getAttachments());
            }

            @Override public IListTerm caseNil(INilTerm nil) {
                return null;
            }

            @Override public IListTerm caseVar(ITermVar var) {
                return null;
            }

        });
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
