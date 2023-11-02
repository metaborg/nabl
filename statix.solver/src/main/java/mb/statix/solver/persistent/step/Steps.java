package mb.statix.solver.persistent.step;

import org.metaborg.util.functions.Function1;

import jakarta.annotation.Nullable;

public class Steps {

    private Steps() { }

    public static <R> IStep.Cases<R> cases(
            Function1<CArithStep, R> onArith,
            Function1<CAstIdStep, R> onAstId,
            Function1<CAstPropertyStep, R> onAstProperty,
            Function1<CConjStep, R> onConj,
            Function1<CEqualStep, R> onEqual,
            Function1<CExistsStep, R> onExists,
            Function1<CFalseStep, R> onFalse,
            Function1<CInequalStep, R> onInequal,
            Function1<CNewStep, R> onNew,
            Function1<AResolveQueryStep, R> onResolveQuery,
            Function1<CTellEdgeStep, R> onTellEdge,
            Function1<CTrueStep, R> onTrue,
            Function1<CTryStep, R> onTry,
            Function1<CUserStep, R> onUser
    ) {
        return new IStep.Cases<R>() {

            public R caseArith(CArithStep step) {
                return onArith.apply(step);
            }

            public R caseAstId(CAstIdStep step) {
                return onAstId.apply(step);
            }

            public R caseAstProperty(CAstPropertyStep step) {
                return onAstProperty.apply(step);
            }

            public R caseConj(CConjStep step) {
                return onConj.apply(step);
            }

            public R caseEqual(CEqualStep step) {
                return onEqual.apply(step);
            }

            public R caseExists(CExistsStep step) {
                return onExists.apply(step);
            }

            public R caseFalse(CFalseStep step) {
                return onFalse.apply(step);
            }

            public R caseInequal(CInequalStep step) {
                return onInequal.apply(step);
            }

            public R caseNew(CNewStep step) {
                return onNew.apply(step);
            }

            public R caseResolveQuery(AResolveQueryStep step) {
                return onResolveQuery.apply(step);
            }

            public R caseTellEdge(CTellEdgeStep step) {
                return onTellEdge.apply(step);
            }

            public R caseTrue(CTrueStep step) {
                return onTrue.apply(step);
            }

            public R caseTry(CTryStep step) {
                return onTry.apply(step);
            }

            public R caseUser(CUserStep step) {
                return onUser.apply(step);
            }

        };
    }

    public static <R> CasesBuilder<R> cases() {
        return new CasesBuilder<>();
    }

    public static class CasesBuilder<R> {
        private @Nullable Function1<CArithStep, R> onArith;
        private @Nullable Function1<CAstIdStep, R> onAstId;
        private @Nullable Function1<CAstPropertyStep, R> onAstProperty;
        private @Nullable Function1<CConjStep, R> onConj;
        private @Nullable Function1<CEqualStep, R> onEqual;
        private @Nullable Function1<CExistsStep, R> onExists;
        private @Nullable Function1<CFalseStep, R> onFalse;
        private @Nullable Function1<CInequalStep, R> onInequal;
        private @Nullable Function1<CNewStep, R> onNew;
        private @Nullable Function1<AResolveQueryStep, R> onResolveQuery;
        private @Nullable Function1<CTellEdgeStep, R> onTellEdge;
        private @Nullable Function1<CTrueStep, R> onTrue;
        private @Nullable Function1<CTryStep, R> onTry;
        private @Nullable Function1<CUserStep, R> onUser;

        private CasesBuilder() { }

        public CasesBuilder<R> arith(Function1<CArithStep, R> onArith) {
            this.onArith = onArith;
            return this;
        }

        public CasesBuilder<R> astId(Function1<CAstIdStep, R> onAstId) {
            this.onAstId = onAstId;
            return this;
        }

        public CasesBuilder<R> astProperty(Function1<CAstPropertyStep, R> onAstProperty) {
            this.onAstProperty = onAstProperty;
            return this;
        }

        public CasesBuilder<R> conj(Function1<CConjStep, R> onConj) {
            this.onConj = onConj;
            return this;
        }

        public CasesBuilder<R> equal(Function1<CEqualStep, R> onEqual) {
            this.onEqual = onEqual;
            return this;
        }

        public CasesBuilder<R> exists(Function1<CExistsStep, R> onExists) {
            this.onExists = onExists;
            return this;
        }

        public CasesBuilder<R> _false(Function1<CFalseStep, R> onFalse) {
            this.onFalse = onFalse;
            return this;
        }

        public CasesBuilder<R> inequal(Function1<CInequalStep, R> onInequal) {
            this.onInequal = onInequal;
            return this;
        }

        public CasesBuilder<R> _new(Function1<CNewStep, R> onNew) {
            this.onNew = onNew;
            return this;
        }

        public CasesBuilder<R> resolveQuery(Function1<AResolveQueryStep, R> onResolveQuery) {
            this.onResolveQuery = onResolveQuery;
            return this;
        }

        public CasesBuilder<R> tellEdge(Function1<CTellEdgeStep, R> onTellEdge) {
            this.onTellEdge = onTellEdge;
            return this;
        }

        public CasesBuilder<R> _true(Function1<CTrueStep, R> onTrue) {
            this.onTrue = onTrue;
            return this;
        }

        public CasesBuilder<R> _try(Function1<CTryStep, R> onTry) {
            this.onTry = onTry;
            return this;
        }

        public CasesBuilder<R> user(Function1<CUserStep, R> onUser) {
            this.onUser = onUser;
            return this;
        }

        public IStep.Cases<R> otherwise(Function1<IStep, R> otherwise) {
            return new IStep.Cases<R>() {

                @Override
                public R caseArith(CArithStep step) {
                    return onArith == null ? otherwise.apply(step) : onArith.apply(step);
                }

                @Override
                public R caseAstId(CAstIdStep step) {
                    return onAstId == null ? otherwise.apply(step) : onAstId.apply(step);
                }

                @Override
                public R caseAstProperty(CAstPropertyStep step) {
                    return onAstProperty == null ? otherwise.apply(step) : onAstProperty.apply(step);
                }

                @Override
                public R caseConj(CConjStep step) {
                    return onConj == null ? otherwise.apply(step) : onConj.apply(step);
                }

                @Override
                public R caseEqual(CEqualStep step) {
                    return onEqual == null ? otherwise.apply(step) : onEqual.apply(step);
                }

                @Override
                public R caseExists(CExistsStep step) {
                    return onExists == null ? otherwise.apply(step) : onExists.apply(step);
                }

                @Override
                public R caseFalse(CFalseStep step) {
                    return onFalse == null ? otherwise.apply(step) : onFalse.apply(step);
                }

                @Override
                public R caseInequal(CInequalStep step) {
                    return onInequal == null ? otherwise.apply(step) : onInequal.apply(step);
                }

                @Override
                public R caseNew(CNewStep step) {
                    return onNew == null ? otherwise.apply(step) : onNew.apply(step);
                }

                @Override
                public R caseResolveQuery(AResolveQueryStep step) {
                    return onResolveQuery == null ? otherwise.apply(step) : onResolveQuery.apply(step);
                }

                @Override
                public R caseTellEdge(CTellEdgeStep step) {
                    return onTellEdge == null ? otherwise.apply(step) : onTellEdge.apply(step);
                }

                @Override
                public R caseTrue(CTrueStep step) {
                    return onTrue == null ? otherwise.apply(step) : onTrue.apply(step);
                }

                @Override
                public R caseTry(CTryStep step) {
                    return onTry == null ? otherwise.apply(step) : onTry.apply(step);
                }

                @Override
                public R caseUser(CUserStep step) {
                    return onUser == null ? otherwise.apply(step) : onUser.apply(step);
                }

            };
        }

    }

    // Step Result Utilities

    public static boolean isSuccess(IStep step) {
        return isSuccess(step.result());
    }

    public static boolean isSuccess(StepResult result) {
        return result.match(
                (st, uvs, ncs, cpl, nes) -> true,
                ex -> false,
                dl -> false
        );
    }

}
