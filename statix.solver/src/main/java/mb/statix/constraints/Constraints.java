package mb.statix.constraints;

import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.Action1;
import org.metaborg.util.functions.CheckedFunction1;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.PartialFunction1;
import org.metaborg.util.optionals.Optionals;
import org.metaborg.util.unit.Unit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.IConstraint;
import mb.statix.spec.RuleUtil;

public final class Constraints {

    private Constraints() {
    }

    /**
     * Bottom up transformation, where the transformation is applied starting from the leaves, then to the transformed
     * parents until the root.
     */
    public static Function1<IConstraint, IConstraint> bottomup(Function1<IConstraint, IConstraint> f,
            boolean recurseInLogicalScopes) {
        return constraint -> {
            switch(constraint.constraintTag()) {
                case CConj: {
                    CConj c = (CConj) constraint;
                    return f.apply(new CConj(bottomup(f, recurseInLogicalScopes).apply(c.left()),
                        bottomup(f, recurseInLogicalScopes).apply(c.right()), c.cause().orElse(null)));
                }
                case CExists: {
                    CExists c = (CExists) constraint;
                    return f.apply(new CExists(c.vars(), bottomup(f, recurseInLogicalScopes).apply(c.constraint()),
                        c.cause().orElse(null)));
                }
                case CTry: {
                    CTry c = (CTry) constraint;
                    return f.apply(recurseInLogicalScopes ? new CTry(bottomup(f, recurseInLogicalScopes).apply(c.constraint()),
                        c.cause().orElse(null), c.message().orElse(null)) : c);
                }
                case CArith:
                case CEqual:
                case CFalse:
                case CInequal:
                case CNew:
                case IResolveQuery:
                case CTellEdge:
                case CAstId:
                case CAstProperty:
                case CTrue:
                case CUser:
                    return f.apply(constraint);
            }
            // N.B. don't use this in default case branch, instead use IDE to catch non-exhaustive switch statements
            throw new RuntimeException("Missing case for IConstraint subclass/tag");
        };
    }

    /**
     * In order transformation of the leaf constraints.
     */
    public static Function1<IConstraint, IConstraint> map(Function1<IConstraint, IConstraint> f,
            boolean recurseInLogicalScopes) {
        return constraint -> {
            switch(constraint.constraintTag()) {
                case CConj: {
                    CConj c = (CConj) constraint;
                    final IConstraint left = map(f, recurseInLogicalScopes).apply(c.left());
                    final IConstraint right = map(f, recurseInLogicalScopes).apply(c.right());
                    return new CConj(left, right, c.cause().orElse(null));
                }
                case CExists: {
                    CExists c = (CExists) constraint;
                    final IConstraint body = map(f, recurseInLogicalScopes).apply(c.constraint());
                    return new CExists(c.vars(), body, c.cause().orElse(null));
                }
                case CTry: {
                    CTry c = (CTry) constraint;
                    if(recurseInLogicalScopes) {
                        final IConstraint body = map(f, recurseInLogicalScopes).apply(c.constraint());
                        return new CTry(body, c.cause().orElse(null), c.message().orElse(null));
                    } else {
                        return c;
                    }
                }
                case CArith:
                case CEqual:
                case CFalse:
                case CInequal:
                case CNew:
                case IResolveQuery:
                case CTellEdge:
                case CAstId:
                case CAstProperty:
                case CTrue:
                case CUser:
                    return f.apply(constraint);
            }
            // N.B. don't use this in default case branch, instead use IDE to catch non-exhaustive switch statements
            throw new RuntimeException("Missing case for IConstraint subclass/tag");
        };
    }

    /**
     * In order transformation of the leaf constraints, fail if the given function fails on any of the leaves.
     */
    public static Function1<IConstraint, Optional<IConstraint>> filter(Function1<IConstraint, Optional<IConstraint>> f,
            boolean recurseInLogicalScopes) {
        return constraint -> {
            switch(constraint.constraintTag()) {
                case CConj: {
                    CConj c = (CConj) constraint;
                    final Optional<IConstraint> left = filter(f, recurseInLogicalScopes).apply(c.left());
                    final Optional<IConstraint> right = filter(f, recurseInLogicalScopes).apply(c.right());
                    return Optionals.lift(left, right, (l, r) -> new CConj(l, r, c.cause().orElse(null)));
                }
                case CExists: {
                    CExists c = (CExists) constraint;
                    final Optional<IConstraint> body = filter(f, recurseInLogicalScopes).apply(c.constraint());
                    return body.map(b -> new CExists(c.vars(), b, c.cause().orElse(null)));
                }
                case CTry: {
                    CTry c = (CTry) constraint;
                    if(recurseInLogicalScopes) {
                        final Optional<IConstraint> body = filter(f, recurseInLogicalScopes).apply(c.constraint());
                        return body.map(b -> new CTry(b, c.cause().orElse(null), c.message().orElse(null)));
                    } else {
                        return Optional.of(c);
                    }
                }
                case CArith:
                case CEqual:
                case CFalse:
                case CInequal:
                case CNew:
                case IResolveQuery:
                case CTellEdge:
                case CAstId:
                case CAstProperty:
                case CTrue:
                case CUser:
                    return f.apply(constraint);
            }
            // N.B. don't use this in default case branch, instead use IDE to catch non-exhaustive switch statements
            throw new RuntimeException("Missing case for IConstraint subclass/tag");
        };
    }

    /**
     * In order transformation of the leaf constraints, return a list of new constraints.
     */
    public static Function1<IConstraint, Stream<IConstraint>> flatMap(Function1<IConstraint, Stream<IConstraint>> f,
            boolean recurseInLogicalScopes) {
        return constraint -> {
            switch(constraint.constraintTag()) {
                case CConj: {
                    CConj c = (CConj) constraint;
                    return flatMap(f, recurseInLogicalScopes).apply(c.left()).flatMap(l -> {
                        return flatMap(f, recurseInLogicalScopes).apply(c.right()).map(r -> {
                            return new CConj(l, r, c.cause().orElse(null));
                        });
                    });
                }
                case CExists: {
                    CExists c = (CExists) constraint;
                    return flatMap(f, recurseInLogicalScopes).apply(c.constraint()).map(b -> {
                        return new CExists(c.vars(), b, c.cause().orElse(null));
                    });
                }
                case CTry: {
                    CTry c = (CTry) constraint;
                    if(recurseInLogicalScopes) {
                        return flatMap(f, recurseInLogicalScopes).apply(c.constraint()).map(b -> {
                            return new CTry(b, c.cause().orElse(null), c.message().orElse(null));
                        });
                    } else {
                        return Stream.of(c);
                    }
                }
                case CArith:
                case CEqual:
                case CFalse:
                case CInequal:
                case CNew:
                case IResolveQuery:
                case CTellEdge:
                case CAstId:
                case CAstProperty:
                case CTrue:
                case CUser:
                    return f.apply(constraint);
            }
            // N.B. don't use this in default case branch, instead use IDE to catch non-exhaustive switch statements
            throw new RuntimeException("Missing case for IConstraint subclass/tag");
        };
    }

    public static <T> Function1<IConstraint, List<T>> collectBase(PartialFunction1<IConstraint, T> f,
            boolean recurseInLogicalScopes) {
        return c -> {
            final ImmutableList.Builder<T> ts = ImmutableList.builder();
            collectBase(c, f, ts, recurseInLogicalScopes);
            return ts.build();
        };
    }

    private static <T> void collectBase(IConstraint constraint, PartialFunction1<IConstraint, T> f,
        ImmutableList.Builder<T> ts, boolean recurseInLogicalScopes) {
        switch(constraint.constraintTag()) {
            case CConj: {
                CConj c = (CConj) constraint;
                disjoin(c).forEach(cc -> collectBase(cc, f, ts, recurseInLogicalScopes));
                break;
            }
            case CExists: {
                CExists c = (CExists) constraint;
                disjoin(c.constraint()).forEach(
                    cc -> collectBase(cc, f, ts, recurseInLogicalScopes));
                break;
            }
            case CTry: {
                CTry c = (CTry) constraint;
                if(recurseInLogicalScopes) {
                    disjoin(c.constraint()).forEach(
                        cc -> collectBase(cc, f, ts, recurseInLogicalScopes));
                }
                break;
            }
            case CArith:
            case CEqual:
            case CFalse:
            case CInequal:
            case CNew:
            case IResolveQuery:
            case CTellEdge:
            case CAstId:
            case CAstProperty:
            case CTrue:
            case CUser: {
                f.apply(constraint).ifPresent(ts::add);
                break;
            }
        }
    }

    public static List<IConstraint> apply(List<IConstraint> constraints, ISubstitution.Immutable subst) {
        return Constraints.apply(constraints, subst, null);
    }

    public static List<IConstraint> apply(List<IConstraint> constraints, ISubstitution.Immutable subst,
            @Nullable IConstraint cause) {
        return constraints.stream().map(c -> c.apply(subst).withCause(cause)).collect(ImmutableList.toImmutableList());
    }

    public static String toString(Iterable<? extends IConstraint> constraints, TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(IConstraint constraint : constraints) {
            if(!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(constraint.toString(termToString));
        }
        return sb.toString();
    }

    /**
     * Convert constraints into a conjunction constraint.
     */
    public static IConstraint conjoin(Iterable<? extends IConstraint> constraints) {
        // FIXME What about causes? Unfolding this conjunction might overwrite
        //       causes in the constraints by null.
        IConstraint conj = null;
        for(IConstraint constraint : constraints) {
            conj = (conj != null) ? new CConj(constraint, conj) : constraint;
        }
        return (conj != null) ? conj : new CTrue();
    }

    /**
     * Convert constraints into a conjunction constraint, using the given constraint as tail.
     */
    public static IConstraint conjoin(Iterable<? extends IConstraint> constraints, IConstraint tail) {
        // FIXME What about causes? Unfolding this conjunction might overwrite
        //       causes in the constraints by null.
        IConstraint conj = tail;
        for(IConstraint constraint : constraints) {
            conj = (conj != null) ? new CConj(constraint, conj) : constraint;
        }
        return conj;
    }

    /**
     * Split a conjunction constraint into constraints.
     */
    public static List<IConstraint> disjoin(IConstraint constraint) {
        ImmutableList.Builder<IConstraint> constraints = ImmutableList.builder();
        disjoin(constraint, constraints::add);
        return constraints.build();
    }

    public static void disjoin(IConstraint constraint, Action1<IConstraint> action) {
        Deque<IConstraint> worklist = Lists.newLinkedList();
        worklist.push(constraint);
        while(!worklist.isEmpty()) {
            IConstraint c = worklist.pop();
            if(c.constraintTag() == IConstraint.Tag.CConj) {
                CConj conj = (CConj) c;
                // HEURISTIC Use the cause of the surrounding conjunction, or keep
                //           the cause of the constraints. This is a heuristic which seems
                //           to work well, but in general maintaining causes requires some
                //           care throughout the solver code.
                worklist.push(
                    conj.left().withCause(conj.cause().orElse(conj.left().cause().orElse(null))));
                worklist.push(
                    conj.right().withCause(conj.cause().orElse(conj.right().cause().orElse(null))));
            } else {
                action.apply(c);
            }
        }
    }

    public static Set.Immutable<ITermVar> freeVars(Iterable<? extends IConstraint> constraints) {
        Set.Transient<ITermVar> freeVars = CapsuleUtil.transientSet();
        visitFreeVars(constraints, freeVars::__insert);
        return freeVars.freeze();
    }

    public static void visitFreeVars(Iterable<? extends IConstraint> constraints, Action1<ITermVar> onFreeVar) {
        constraints.forEach(c -> c.visitFreeVars(onFreeVar));
    }

    public static Set.Immutable<ITermVar> vars(IConstraint constraint) {
        Set.Transient<ITermVar> vars = CapsuleUtil.transientSet();
        vars(constraint, vars::__insert);
        return vars.freeze();
    }

    public static void vars(IConstraint constraint, Action1<ITermVar> onVar) {
        switch(constraint.constraintTag()) {
            case CArith: {
                CArith onArith = (CArith) constraint;
                onArith.expr1().isTerm().ifPresent(t -> t.getVars().forEach(onVar::apply));
                onArith.expr2().isTerm().ifPresent(t -> t.getVars().forEach(onVar::apply));
                break;
            }
            case CConj: {
                CConj onConj = (CConj) constraint;
                Constraints.disjoin(onConj).forEach(c -> vars(c, onVar));
                break;
            }
            case CEqual: {
                CEqual onEqual = (CEqual) constraint;
                onEqual.term1().getVars().forEach(onVar::apply);
                onEqual.term2().getVars().forEach(onVar::apply);
                break;
            }
            case CExists: {
                CExists onExists = (CExists) constraint;
                onExists.vars().forEach(onVar::apply);
                vars(onExists.constraint(), onVar);
                break;
            }
            case CFalse:
            case CTrue:
                break;
            case CInequal: {
                CInequal onInequal = (CInequal) constraint;
                onInequal.term1().getVars().stream()
                    .filter(v -> !onInequal.universals().contains(v)).forEach(onVar::apply);
                onInequal.term2().getVars().stream()
                    .filter(v -> !onInequal.universals().contains(v)).forEach(onVar::apply);
                break;
            }
            case CNew: {
                CNew onNew = (CNew) constraint;
                onNew.scopeTerm().getVars().forEach(onVar::apply);
                onNew.datumTerm().getVars().forEach(onVar::apply);
                break;
            }
            case IResolveQuery: {
                IResolveQuery onResolveQuery = (IResolveQuery) constraint;
                onResolveQuery.scopeTerm().getVars().forEach(onVar::apply);
                RuleUtil.vars(onResolveQuery.filter().getDataWF(), onVar);
                RuleUtil.vars(onResolveQuery.min().getDataEquiv(), onVar);
                onResolveQuery.resultTerm().getVars().forEach(onVar::apply);
                break;
            }
            case CTellEdge: {
                CTellEdge onTellEdge = (CTellEdge) constraint;
                onTellEdge.sourceTerm().getVars().forEach(onVar::apply);
                onTellEdge.targetTerm().getVars().forEach(onVar::apply);
                break;
            }
            case CAstId: {
                CAstId onTermId = (CAstId) constraint;
                onTermId.astTerm().getVars().forEach(onVar::apply);
                onTermId.idTerm().getVars().forEach(onVar::apply);
                break;
            }
            case CAstProperty: {
                CAstProperty onTermProperty = (CAstProperty) constraint;
                onTermProperty.idTerm().getVars().forEach(onVar::apply);
                onTermProperty.value().getVars().forEach(onVar::apply);
                break;
            }
            case CTry: {
                CTry onTry = (CTry) constraint;
                vars(onTry.constraint(), onVar);
                break;
            }
            case CUser: {
                CUser onUser = (CUser) constraint;
                onUser.args().forEach(t -> t.getVars().forEach(onVar::apply));
                break;
            }
        }
    }

    public static IConstraint exists(Iterable<ITermVar> vars, IConstraint body) {
        final io.usethesource.capsule.Set.Immutable<ITermVar> varSet = CapsuleUtil.toSet(vars);
        if(varSet.isEmpty()) return body;
        CExists existsConstraint = new CExists(varSet, body);
        if(body.bodyCriticalEdges().isPresent()) {
            existsConstraint = existsConstraint.withBodyCriticalEdges(body.bodyCriticalEdges().get());
        }
        return existsConstraint;
    }

    /**
     * Checks whether the constraint is {@link CTrue} or {@link CFalse}.
     *
     * @param constraint the constraint to check
     * @return {@code true} when the constraint is {@link CTrue};
     * {@code false} when the constraint is {@link CFalse};
     * otherwise, none
     */
    public static Optional<Boolean> trivial(IConstraint constraint) {
        switch(constraint.constraintTag()) {
            case CTrue:
                return Optional.of(true);
            case CFalse:
                return Optional.of(false);
            default:
                return Optional.empty();
        }
    }

}
