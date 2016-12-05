package org.metaborg.meta.nabl2.terms;

import java.util.function.Function;
import java.util.function.Supplier;

import org.metaborg.meta.nabl2.functions.CheckedFunction0;
import org.metaborg.meta.nabl2.functions.CheckedFunction1;
import org.metaborg.meta.nabl2.terms.ITerm.Cases;
import org.metaborg.meta.nabl2.terms.ITerm.CheckedCases;

public class Terms {

    // safe

    public static <T> ITerm.Cases<T> cases(
        // @formatter:off
        Function<? super IApplTerm, T> onAppl,
        Function<? super ITupleTerm, T> onTuple,
        Function<? super IListTerm, T> onList,
        Function<? super IStringTerm, T> onString,
        Function<? super IIntTerm, T> onInt,
        Function<? super ITermVar, T> onVar
        // @formatter:on
    ) {
        return new ITerm.Cases<T>() {

            @Override public T caseAppl(IApplTerm applTerm) {
                return onAppl.apply(applTerm);
            }

            @Override public T caseTuple(ITupleTerm tuple) {
                return onTuple.apply(tuple);
            }

            @Override public T caseList(IListTerm list) {
                return onList.apply(list);
            }

            @Override public T caseString(IStringTerm string) {
                return onString.apply(string);
            }

            @Override public T caseInt(IIntTerm integer) {
                return onInt.apply(integer);
            }

            @Override public T caseVar(ITermVar var) {
                return onVar.apply(var);
            }

            @Override public T apply(ITerm t) {
                return t.match(this);
            }

        };
    }

    public static <T> CaseBuilder<T> cases() {
        return new CaseBuilder<>();
    }

    public static class CaseBuilder<T> {

        private Function<? super IApplTerm,T> onAppl;
        private Function<? super ITupleTerm,T> onTuple;
        private Function<? super IListTerm,T> onList;
        private Function<? super IStringTerm,T> onString;
        private Function<? super IIntTerm,T> onInt;
        private Function<? super ITermVar,T> onVar;

        public CaseBuilder<T> appl(Function<? super IApplTerm,T> onAppl) {
            this.onAppl = onAppl;
            return this;
        }

        public CaseBuilder<T> tuple(Function<? super ITupleTerm,T> onTuple) {
            this.onTuple = onTuple;
            return this;
        }

        public CaseBuilder<T> list(Function<? super IListTerm,T> onList) {
            this.onList = onList;
            return this;
        }

        public CaseBuilder<T> string(Function<? super IStringTerm,T> onString) {
            this.onString = onString;
            return this;
        }

        public CaseBuilder<T> integer(Function<? super IIntTerm,T> onInt) {
            this.onInt = onInt;
            return this;
        }

        public CaseBuilder<T> var(Function<? super ITermVar,T> onVar) {
            this.onVar = onVar;
            return this;
        }

        public Cases<T> otherwise(Supplier<T> otherwise) {
            return new ITerm.Cases<T>() {

                @Override public T caseAppl(IApplTerm appl) {
                    return (onAppl != null) ? onAppl.apply(appl) : otherwise.get();
                }

                @Override public T caseTuple(ITupleTerm tuple) {
                    return (onTuple != null) ? onTuple.apply(tuple) : otherwise.get();
                }

                @Override public T caseList(IListTerm list) {
                    return (onList != null) ? onList.apply(list) : otherwise.get();
                }

                @Override public T caseString(IStringTerm string) {
                    return (onString != null) ? onString.apply(string) : otherwise.get();
                }

                @Override public T caseInt(IIntTerm integer) {
                    return (onInt != null) ? onInt.apply(integer) : otherwise.get();
                }

                @Override public T caseVar(ITermVar var) {
                    return (onVar != null) ? onVar.apply(var) : otherwise.get();
                }

                @Override public T apply(ITerm t) {
                    return t.match(this);
                }

            };
        }

    }

    // checked

    public static <T, E extends Throwable> ITerm.CheckedCases<T,E> checkedCases(
        // @formatter:off
        CheckedFunction1<? super IApplTerm, T, E> onAppl,
        CheckedFunction1<? super ITupleTerm, T, E> onTuple,
        CheckedFunction1<? super IListTerm, T, E> onList,
        CheckedFunction1<? super IStringTerm, T, E> onString,
        CheckedFunction1<? super IIntTerm, T, E> onInt,
        CheckedFunction1<? super ITermVar, T, E> onVar
        // @formatter:on
    ) {
        return new ITerm.CheckedCases<T,E>() {

            @Override public T caseAppl(IApplTerm applTerm) throws E {
                return onAppl.apply(applTerm);
            }

            @Override public T caseTuple(ITupleTerm tuple) throws E {
                return onTuple.apply(tuple);
            }

            @Override public T caseList(IListTerm list) throws E {
                return onList.apply(list);
            }

            @Override public T caseString(IStringTerm string) throws E {
                return onString.apply(string);
            }

            @Override public T caseInt(IIntTerm integer) throws E {
                return onInt.apply(integer);
            }

            @Override public T caseVar(ITermVar var) throws E {
                return onVar.apply(var);
            }

            @Override public T apply(ITerm term) throws E {
                return term.matchOrThrow(this);
            }

        };
    }

    public static <T, E extends Throwable> CheckedCaseBuilder<T,E> checkedCases() {
        return new CheckedCaseBuilder<>();
    }

    public static class CheckedCaseBuilder<T, E extends Throwable> {

        private CheckedFunction1<? super IApplTerm,T,E> onAppl;
        private CheckedFunction1<? super ITupleTerm,T,E> onTuple;
        private CheckedFunction1<? super IListTerm,T,E> onList;
        private CheckedFunction1<? super IStringTerm,T,E> onString;
        private CheckedFunction1<? super IIntTerm,T,E> onInt;
        private CheckedFunction1<? super ITermVar,T,E> onVar;

        public CheckedCaseBuilder<T,E> appl(CheckedFunction1<? super IApplTerm,T,E> onAppl) {
            this.onAppl = onAppl;
            return this;
        }

        public CheckedCaseBuilder<T,E> tuple(CheckedFunction1<? super ITupleTerm,T,E> onTuple) {
            this.onTuple = onTuple;
            return this;
        }

        public CheckedCaseBuilder<T,E> list(CheckedFunction1<? super IListTerm,T,E> onList) {
            this.onList = onList;
            return this;
        }

        public CheckedCaseBuilder<T,E> string(CheckedFunction1<? super IStringTerm,T,E> onString) {
            this.onString = onString;
            return this;
        }

        public CheckedCaseBuilder<T,E> integer(CheckedFunction1<? super IIntTerm,T,E> onInt) {
            this.onInt = onInt;
            return this;
        }

        public CheckedCaseBuilder<T,E> var(CheckedFunction1<? super ITermVar,T,E> onVar) {
            this.onVar = onVar;
            return this;
        }

        public CheckedCases<T,E> otherwise(CheckedFunction0<T,E> otherwise) {
            return new ITerm.CheckedCases<T,E>() {

                @Override public T caseAppl(IApplTerm appl) throws E {
                    return (onAppl != null) ? onAppl.apply(appl) : otherwise.apply();
                }

                @Override public T caseTuple(ITupleTerm tuple) throws E {
                    return (onTuple != null) ? onTuple.apply(tuple) : otherwise.apply();
                }

                @Override public T caseList(IListTerm list) throws E {
                    return (onList != null) ? onList.apply(list) : otherwise.apply();
                }

                @Override public T caseString(IStringTerm string) throws E {
                    return (onString != null) ? onString.apply(string) : otherwise.apply();
                }

                @Override public T caseInt(IIntTerm integer) throws E {
                    return (onInt != null) ? onInt.apply(integer) : otherwise.apply();
                }

                @Override public T caseVar(ITermVar var) throws E {
                    return (onVar != null) ? onVar.apply(var) : otherwise.apply();
                }

                @Override public T apply(ITerm t) throws E {
                    return t.matchOrThrow(this);
                }

            };
        }

    }

}