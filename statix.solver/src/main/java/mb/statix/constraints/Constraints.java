package mb.statix.constraints;

import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.metaborg.util.functions.Action1;
import org.metaborg.util.functions.CheckedFunction1;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.PartialFunction1;
import org.metaborg.util.optionals.Optionals;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.CapsuleUtil;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.IConstraint;
import mb.statix.spec.RuleUtil;

public final class Constraints {

    private Constraints() {
    }

    // @formatter:off
    public static <R> IConstraint.Cases<R> cases(
                Function1<CArith, R> onArith,
                Function1<CConj,R> onConj,
                Function1<CEqual,R> onEqual,
                Function1<CExists,R> onExists,
                Function1<CFalse,R> onFalse,
                Function1<CInequal,R> onInequal,
                Function1<CNew,R> onNew,
                Function1<CResolveQuery,R> onResolveQuery,
                Function1<CTellEdge,R> onTellEdge,
                Function1<CTellRel,R> onTellRel,
                Function1<CAstId,R> onTermId,
                Function1<CAstProperty,R> onTermProperty,
                Function1<CTrue,R> onTrue,
                Function1<CTry,R> onTry,
                Function1<CUser,R> onUser
            ) {
        return new IConstraint.Cases<R>() {

            @Override public R caseArith(CArith c) {
                return onArith.apply(c);
            }

            @Override public R caseConj(CConj c) {
                return onConj.apply(c);
            }

            @Override public R caseEqual(CEqual c) {
                return onEqual.apply(c);
            }

            @Override public R caseExists(CExists c) {
                return onExists.apply(c);
            }

            @Override public R caseFalse(CFalse c) {
                return onFalse.apply(c);
            }

            @Override public R caseInequal(CInequal c) {
                return onInequal.apply(c);
            }

            @Override public R caseNew(CNew c) {
                return onNew.apply(c);
            }

            @Override public R caseResolveQuery(CResolveQuery c) {
                return onResolveQuery.apply(c);
            }

            @Override public R caseTellEdge(CTellEdge c) {
                return onTellEdge.apply(c);
            }

            @Override public R caseTellRel(CTellRel c) {
                return onTellRel.apply(c);
            }

            @Override public R caseTermId(CAstId c) {
                return onTermId.apply(c);
            }

            @Override public R caseTermProperty(CAstProperty c) {
                return onTermProperty.apply(c);
            }

            @Override public R caseTrue(CTrue c) {
                return onTrue.apply(c);
            }

            @Override public R caseTry(CTry c) {
                return onTry.apply(c);
            }

            @Override public R caseUser(CUser c) {
                return onUser.apply(c);
            }

        };
    }
    // @formatter:on

    public static <R> CaseBuilder<R> cases() {
        return new CaseBuilder<>();
    }

    public static class CaseBuilder<R> {

        private Function1<CArith, R> onArith;
        private Function1<CConj, R> onConj;
        private Function1<CEqual, R> onEqual;
        private Function1<CExists, R> onExists;
        private Function1<CFalse, R> onFalse;
        private Function1<CInequal, R> onInequal;
        private Function1<CNew, R> onNew;
        private Function1<CResolveQuery, R> onResolveQuery;
        private Function1<CTellEdge, R> onTellEdge;
        private Function1<CTellRel, R> onTellRel;
        private Function1<CAstId, R> onTermId;
        private Function1<CAstProperty, R> onTermProperty;
        private Function1<CTrue, R> onTrue;
        private Function1<CTry, R> onTry;
        private Function1<CUser, R> onUser;

        public CaseBuilder<R> arith(Function1<CArith, R> onArith) {
            this.onArith = onArith;
            return this;
        }

        public CaseBuilder<R> conj(Function1<CConj, R> onConj) {
            this.onConj = onConj;
            return this;
        }

        public CaseBuilder<R> equal(Function1<CEqual, R> onEqual) {
            this.onEqual = onEqual;
            return this;
        }

        public CaseBuilder<R> exists(Function1<CExists, R> onExists) {
            this.onExists = onExists;
            return this;
        }

        public CaseBuilder<R> _false(Function1<CFalse, R> onFalse) {
            this.onFalse = onFalse;
            return this;
        }

        public CaseBuilder<R> inequal(Function1<CInequal, R> onInequal) {
            this.onInequal = onInequal;
            return this;
        }

        public CaseBuilder<R> _new(Function1<CNew, R> onNew) {
            this.onNew = onNew;
            return this;
        }

        public CaseBuilder<R> resolveQuery(Function1<CResolveQuery, R> onResolveQuery) {
            this.onResolveQuery = onResolveQuery;
            return this;
        }

        public CaseBuilder<R> tellEdge(Function1<CTellEdge, R> onTellEdge) {
            this.onTellEdge = onTellEdge;
            return this;
        }

        public CaseBuilder<R> tellRel(Function1<CTellRel, R> onTellRel) {
            this.onTellRel = onTellRel;
            return this;
        }

        public CaseBuilder<R> termId(Function1<CAstId, R> onTermId) {
            this.onTermId = onTermId;
            return this;
        }

        public CaseBuilder<R> termProperty(Function1<CAstProperty, R> onTermProperty) {
            this.onTermProperty = onTermProperty;
            return this;
        }

        public CaseBuilder<R> _true(Function1<CTrue, R> onTrue) {
            this.onTrue = onTrue;
            return this;
        }

        public CaseBuilder<R> _try(Function1<CTry, R> onTry) {
            this.onTry = onTry;
            return this;
        }

        public CaseBuilder<R> user(Function1<CUser, R> onUser) {
            this.onUser = onUser;
            return this;
        }

        public IConstraint.Cases<R> otherwise(Function1<IConstraint, R> otherwise) {
            return new IConstraint.Cases<R>() {

                @Override public R caseArith(CArith c) {
                    return onArith != null ? onArith.apply(c) : otherwise.apply(c);
                }

                @Override public R caseConj(CConj c) {
                    return onConj != null ? onConj.apply(c) : otherwise.apply(c);
                }

                @Override public R caseEqual(CEqual c) {
                    return onEqual != null ? onEqual.apply(c) : otherwise.apply(c);
                }

                @Override public R caseExists(CExists c) {
                    return onExists != null ? onExists.apply(c) : otherwise.apply(c);
                }

                @Override public R caseFalse(CFalse c) {
                    return onFalse != null ? onFalse.apply(c) : otherwise.apply(c);
                }

                @Override public R caseInequal(CInequal c) {
                    return onInequal != null ? onInequal.apply(c) : otherwise.apply(c);
                }

                @Override public R caseNew(CNew c) {
                    return onNew != null ? onNew.apply(c) : otherwise.apply(c);
                }

                @Override public R caseResolveQuery(CResolveQuery c) {
                    return onResolveQuery != null ? onResolveQuery.apply(c) : otherwise.apply(c);
                }

                @Override public R caseTellEdge(CTellEdge c) {
                    return onTellEdge != null ? onTellEdge.apply(c) : otherwise.apply(c);
                }

                @Override public R caseTellRel(CTellRel c) {
                    return onTellRel != null ? onTellRel.apply(c) : otherwise.apply(c);
                }

                @Override public R caseTermId(CAstId c) {
                    return onTermId != null ? onTermId.apply(c) : otherwise.apply(c);
                }

                @Override public R caseTermProperty(CAstProperty c) {
                    return onTermProperty != null ? onTermProperty.apply(c) : otherwise.apply(c);
                }

                @Override public R caseTrue(CTrue c) {
                    return onTrue != null ? onTrue.apply(c) : otherwise.apply(c);
                }

                @Override public R caseTry(CTry c) {
                    return onTry != null ? onTry.apply(c) : otherwise.apply(c);
                }

                @Override public R caseUser(CUser c) {
                    return onUser != null ? onUser.apply(c) : otherwise.apply(c);
                }

            };
        }

    }


    // @formatter:off
    public static <R, E extends Throwable> IConstraint.CheckedCases<R, E> checkedCases(
                CheckedFunction1<CArith, R, E> onArith,
                CheckedFunction1<CConj, R, E> onConj,
                CheckedFunction1<CEqual, R, E> onEqual,
                CheckedFunction1<CExists, R, E> onExists,
                CheckedFunction1<CFalse, R, E> onFalse,
                CheckedFunction1<CInequal, R, E> onInequal,
                CheckedFunction1<CNew, R, E> onNew,
                CheckedFunction1<CResolveQuery, R, E> onResolveQuery,
                CheckedFunction1<CTellEdge, R, E> onTellEdge,
                CheckedFunction1<CTellRel, R, E> onTellRel,
                CheckedFunction1<CAstId, R, E> onTermId,
                CheckedFunction1<CAstProperty, R, E> onTermProperty,
                CheckedFunction1<CTrue, R, E> onTrue,
                CheckedFunction1<CTry, R, E> onTry,
                CheckedFunction1<CUser, R, E> onUser
            ) {
        return new IConstraint.CheckedCases<R, E>() {

            @Override public R caseArith(CArith c) throws E {
                return onArith.apply(c);
            }

            @Override public R caseConj(CConj c) throws E {
                return onConj.apply(c);
            }

            @Override public R caseEqual(CEqual c) throws E {
                return onEqual.apply(c);
            }

            @Override public R caseExists(CExists c) throws E {
                return onExists.apply(c);
            }

            @Override public R caseFalse(CFalse c) throws E {
                return onFalse.apply(c);
            }

            @Override public R caseInequal(CInequal c) throws E {
                return onInequal.apply(c);
            }

            @Override public R caseNew(CNew c) throws E {
                return onNew.apply(c);
            }

            @Override public R caseResolveQuery(CResolveQuery c) throws E {
                return onResolveQuery.apply(c);
            }

            @Override public R caseTellEdge(CTellEdge c) throws E {
                return onTellEdge.apply(c);
            }

            @Override public R caseTellRel(CTellRel c) throws E {
                return onTellRel.apply(c);
            }

            @Override public R caseTermId(CAstId c) throws E {
                return onTermId.apply(c);
            }

            @Override public R caseTermProperty(CAstProperty c) throws E {
                return onTermProperty.apply(c);
            }

            @Override public R caseTrue(CTrue c) throws E {
                return onTrue.apply(c);
            }

            @Override public R caseTry(CTry c) throws E {
                return onTry.apply(c);
            }

            @Override public R caseUser(CUser c) throws E {
                return onUser.apply(c);
            }

        };
    }
    // @formatter:on

    /**
     * Bottom up transformation, where the transformation is applied starting from the leaves, then to the transformed
     * parents until the root.
     */
    public static Function1<IConstraint, IConstraint> bottomup(Function1<IConstraint, IConstraint> f,
            boolean recurseInLogicalScopes) {
        // @formatter:off
        return cases(
            c -> f.apply(c),
            c -> f.apply(new CConj(bottomup(f, recurseInLogicalScopes).apply(c.left()), bottomup(f, recurseInLogicalScopes).apply(c.right()), c.cause().orElse(null))),
            c -> f.apply(c),
            c -> f.apply(new CExists(c.vars(), bottomup(f, recurseInLogicalScopes).apply(c.constraint()), c.cause().orElse(null))),
            c -> f.apply(c),
            c -> f.apply(c),
            c -> f.apply(c),
            c -> f.apply(c),
            c -> f.apply(c),
            c -> f.apply(c),
            c -> f.apply(c),
            c -> f.apply(c),
            c -> f.apply(c),
            c -> f.apply(recurseInLogicalScopes ? new CTry(bottomup(f, recurseInLogicalScopes).apply(c.constraint()), c.cause().orElse(null), c.message().orElse(null)) : c),
            c -> f.apply(c)
        );
        // @formatter:on
    }

    /**
     * In order transformation of the leaf constraints.
     */
    public static Function1<IConstraint, IConstraint> map(Function1<IConstraint, IConstraint> f,
            boolean recurseInLogicalScopes) {
        // @formatter:off
        return cases(
            c -> f.apply(c),
            c -> {
                final IConstraint left = map(f, recurseInLogicalScopes).apply(c.left());
                final IConstraint right = map(f, recurseInLogicalScopes).apply(c.right());
                return new CConj(left, right, c.cause().orElse(null));
            },
            c -> f.apply(c),
            c -> {
                final IConstraint body = map(f, recurseInLogicalScopes).apply(c.constraint());
                return new CExists(c.vars(), body, c.cause().orElse(null));
            },
            c -> f.apply(c),
            c -> f.apply(c),
            c -> f.apply(c),
            c -> f.apply(c),
            c -> f.apply(c),
            c -> f.apply(c),
            c -> f.apply(c),
            c -> f.apply(c),
            c -> f.apply(c),
            c -> {
                if(recurseInLogicalScopes) {
                    final IConstraint body = map(f, recurseInLogicalScopes).apply(c.constraint());
                    return new CTry(body, c.cause().orElse(null), c.message().orElse(null));
                } else {
                    return c;
                }
            },
            c -> f.apply(c)
        );
        // @formatter:on
    }

    /**
     * In order transformation of the leaf constraints, fail if the given function fails on any of the leaves.
     */
    public static Function1<IConstraint, Optional<IConstraint>> filter(Function1<IConstraint, Optional<IConstraint>> f,
            boolean recurseInLogicalScopes) {
        // @formatter:off
        return cases(
            c -> f.apply(c),
            c -> {
                final Optional<IConstraint> left = filter(f, recurseInLogicalScopes).apply(c.left());
                final Optional<IConstraint> right = filter(f, recurseInLogicalScopes).apply(c.right());
                return Optionals.lift(left, right, (l, r) -> new CConj(l, r, c.cause().orElse(null)));
            },
            c -> f.apply(c),
            c -> {
                final Optional<IConstraint> body = filter(f, recurseInLogicalScopes).apply(c.constraint());
                return body.map(b -> new CExists(c.vars(), b, c.cause().orElse(null)));
            },
            c -> f.apply(c),
            c -> f.apply(c),
            c -> f.apply(c),
            c -> f.apply(c),
            c -> f.apply(c),
            c -> f.apply(c),
            c -> f.apply(c),
            c -> f.apply(c),
            c -> f.apply(c),
            c -> {
                if(recurseInLogicalScopes) {
                    final Optional<IConstraint> body = filter(f, recurseInLogicalScopes).apply(c.constraint());
                    return body.map(b -> new CTry(b, c.cause().orElse(null), c.message().orElse(null)));
                } else {
                    return Optional.of(c);
                }
            },
            c -> f.apply(c)
        );
        // @formatter:on
    }

    /**
     * In order transformation of the leaf constraints, return a list of new constraints.
     */
    public static Function1<IConstraint, Stream<IConstraint>> flatMap(Function1<IConstraint, Stream<IConstraint>> f,
            boolean recurseInLogicalScopes) {
        // @formatter:off
        return cases(
            c -> f.apply(c),
            c -> {
                return flatMap(f, recurseInLogicalScopes).apply(c.left()).flatMap(l -> {
                    return flatMap(f, recurseInLogicalScopes).apply(c.right()).map(r -> {
                        return new CConj(l, r, c.cause().orElse(null));
                    });
                });
            },
            c -> f.apply(c),
            c -> {
                return flatMap(f, recurseInLogicalScopes).apply(c.constraint()).map(b -> {
                    return new CExists(c.vars(), b, c.cause().orElse(null));
                });
            },
            c -> f.apply(c),
            c -> f.apply(c),
            c -> f.apply(c),
            c -> f.apply(c),
            c -> f.apply(c),
            c -> f.apply(c),
            c -> f.apply(c),
            c -> f.apply(c),
            c -> f.apply(c),
            c -> {
                if(recurseInLogicalScopes) {
                    return flatMap(f, recurseInLogicalScopes).apply(c.constraint()).map(b -> {
                        return new CTry(b, c.cause().orElse(null), c.message().orElse(null));
                    });
                } else {
                    return Stream.of(c);
                }
            },
            c -> f.apply(c)
        );
        // @formatter:on
    }

    public static <T> Function1<IConstraint, List<T>> collectBase(PartialFunction1<IConstraint, T> f,
            boolean recurseInLogicalScopes) {
        return c -> {
            final ImmutableList.Builder<T> ts = ImmutableList.builder();
            collectBase(c, f, ts, recurseInLogicalScopes);
            return ts.build();
        };
    }

    private static <T> List<T> collectBase(IConstraint constraint, PartialFunction1<IConstraint, T> f,
            ImmutableList.Builder<T> ts, boolean recurseInLogicalScopes) {
        // @formatter:off
        return constraint.match(cases(
            c -> { f.apply(c).ifPresent(ts::add); return null; },
            c -> { disjoin(c).forEach(cc -> collectBase(cc, f, ts, recurseInLogicalScopes)); return null; },
            c -> { f.apply(c).ifPresent(ts::add); return null; },
            c -> { disjoin(c.constraint()).forEach(cc -> collectBase(cc, f, ts, recurseInLogicalScopes)); return null; },
            c -> { f.apply(c).ifPresent(ts::add); return null; },
            c -> { f.apply(c).ifPresent(ts::add); return null; },
            c -> { f.apply(c).ifPresent(ts::add); return null; },
            c -> { f.apply(c).ifPresent(ts::add); return null; },
            c -> { f.apply(c).ifPresent(ts::add); return null; },
            c -> { f.apply(c).ifPresent(ts::add); return null; },
            c -> { f.apply(c).ifPresent(ts::add); return null; },
            c -> { f.apply(c).ifPresent(ts::add); return null; },
            c -> { f.apply(c).ifPresent(ts::add); return null; },
            c -> { if(recurseInLogicalScopes) { disjoin(c.constraint()).forEach(cc -> collectBase(cc, f, ts, recurseInLogicalScopes)); } return null; },
            c -> { f.apply(c).ifPresent(ts::add); return null; }
        ));
        // @formatter:on
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
            worklist.pop().match(Constraints.cases().conj(conj -> {
                worklist.push(conj.left().withCause(conj.cause().orElse(null)));
                worklist.push(conj.right().withCause(conj.cause().orElse(null)));
                return null;
            }).otherwise(c -> {
                action.apply(c);
                return null;
            }));
        }
    }

    public static Set<ITermVar> freeVars(IConstraint constraint) {
        ImmutableSet.Builder<ITermVar> freeVars = ImmutableSet.builder();
        freeVars(constraint, freeVars::add);
        return freeVars.build();
    }

    public static void freeVars(IConstraint constraint, Action1<ITermVar> onVar) {
        // @formatter:off
        constraint.match(Constraints.cases(
            onArith -> {
                onArith.expr1().isTerm().ifPresent(t -> t.getVars().forEach(onVar::apply));
                onArith.expr2().isTerm().ifPresent(t -> t.getVars().forEach(onVar::apply));
                return null;
            },
            onConj -> {
                Constraints.disjoin(onConj).forEach(c -> freeVars(c, onVar));
                return null;
            },
            onEqual -> {
                onEqual.term1().getVars().forEach(onVar::apply);
                onEqual.term2().getVars().forEach(onVar::apply);
                return null;
            },
            onExists -> {
                freeVars(onExists.constraint(), v -> {
                    if(!onExists.vars().contains(v)) {
                        onVar.apply(v);
                    }
                });
                return null;
            },
            onFalse -> {
                return null;
            },
            onInequal -> {
                onInequal.term1().getVars().stream().filter(v -> !onInequal.universals().contains(v)).forEach(onVar::apply);
                onInequal.term2().getVars().stream().filter(v -> !onInequal.universals().contains(v)).forEach(onVar::apply);
                return null;
            },
            onNew -> {
                onNew.terms().forEach(t -> t.getVars().forEach(onVar::apply));
                return null;
            },
            onResolveQuery -> {
                onResolveQuery.scopeTerm().getVars().forEach(onVar::apply);
                RuleUtil.freeVars(onResolveQuery.filter().getDataWF(), onVar);
                RuleUtil.freeVars(onResolveQuery.min().getDataEquiv(), onVar);
                onResolveQuery.resultTerm().getVars().forEach(onVar::apply);
                return null;
            },
            onTellEdge -> {
                onTellEdge.sourceTerm().getVars().forEach(onVar::apply);
                onTellEdge.targetTerm().getVars().forEach(onVar::apply);
                return null;
            },
            onTellRel -> {
                onTellRel.scopeTerm().getVars().forEach(onVar::apply);
                onTellRel.datumTerm().getVars().forEach(onVar::apply);
                return null;
            },
            onTermId -> {
                onTermId.astTerm().getVars().forEach(onVar::apply);
                onTermId.idTerm().getVars().forEach(onVar::apply);
                return null;
            },
            onTermProperty -> {
                onTermProperty.idTerm().getVars().forEach(onVar::apply);
                onTermProperty.value().getVars().forEach(onVar::apply);
                return null;
            },
            onTrue -> null,
            onTry -> {
                freeVars(onTry.constraint(), onVar);
                return null;
            },
            onUser -> {
                onUser.args().forEach(t -> t.getVars().forEach(onVar::apply));
                return null;
            }
        ));
        // @formatter:on

    }

    public static Set<ITermVar> vars(IConstraint constraint) {
        ImmutableSet.Builder<ITermVar> vars = ImmutableSet.builder();
        vars(constraint, vars::add);
        return vars.build();
    }

    public static void vars(IConstraint constraint, Action1<ITermVar> onVar) {
        // @formatter:off
        constraint.match(Constraints.cases(
            onArith -> {
                onArith.expr1().isTerm().ifPresent(t -> t.getVars().forEach(onVar::apply));
                onArith.expr2().isTerm().ifPresent(t -> t.getVars().forEach(onVar::apply));
                return null;
            },
            onConj -> {
                Constraints.disjoin(onConj).forEach(c -> vars(c, onVar));
                return null;
            },
            onEqual -> {
                onEqual.term1().getVars().forEach(onVar::apply);
                onEqual.term2().getVars().forEach(onVar::apply);
                return null;
            },
            onExists -> {
                vars(onExists.constraint(), onVar);
                return null;
            },
            onFalse -> {
                return null;
            },
            onInequal -> {
                onInequal.term1().getVars().stream().filter(v -> !onInequal.universals().contains(v)).forEach(onVar::apply);
                onInequal.term2().getVars().stream().filter(v -> !onInequal.universals().contains(v)).forEach(onVar::apply);
                return null;
            },
            onNew -> {
                onNew.terms().forEach(t -> t.getVars().forEach(onVar::apply));
                return null;
            },
            onResolveQuery -> {
                onResolveQuery.scopeTerm().getVars().forEach(onVar::apply);
                RuleUtil.vars(onResolveQuery.filter().getDataWF(), onVar);
                RuleUtil.vars(onResolveQuery.min().getDataEquiv(), onVar);
                onResolveQuery.resultTerm().getVars().forEach(onVar::apply);
                return null;
            },
            onTellEdge -> {
                onTellEdge.sourceTerm().getVars().forEach(onVar::apply);
                onTellEdge.targetTerm().getVars().forEach(onVar::apply);
                return null;
            },
            onTellRel -> {
                onTellRel.scopeTerm().getVars().forEach(onVar::apply);
                onTellRel.datumTerm().getVars().forEach(onVar::apply);
                return null;
            },
            onTermId -> {
                onTermId.astTerm().getVars().forEach(onVar::apply);
                onTermId.idTerm().getVars().forEach(onVar::apply);
                return null;
            },
            onTermProperty -> {
                onTermProperty.idTerm().getVars().forEach(onVar::apply);
                onTermProperty.value().getVars().forEach(onVar::apply);
                return null;
            },
            onTrue -> null,
            onTry -> {
                vars(onTry.constraint(), onVar);
                return null;
            },
            onUser -> {
                onUser.args().forEach(t -> t.getVars().forEach(onVar::apply));
                return null;
            }
        ));
        // @formatter:on

    }

    public static IConstraint exists(Iterable<ITermVar> vars, IConstraint body) {
        final io.usethesource.capsule.Set.Immutable<ITermVar> varSet = CapsuleUtil.toSet(vars);
        return varSet.isEmpty() ? body : new CExists(varSet, body);
    }


}