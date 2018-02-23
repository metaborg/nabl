package org.metaborg.meta.nabl2.terms;

import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.util.functions.CheckedFunction1;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.Function2;

import com.google.common.collect.ImmutableClassToInstanceMap;

public class Terms {

    public static final String TUPLE_OP = "";

    public static final ImmutableClassToInstanceMap<Object> NO_ATTACHMENTS =
            ImmutableClassToInstanceMap.builder().build();

    // SAFE

    public static <T> ITerm.Cases<T> cases(
    // @formatter:off
        Function1<? super IApplTerm, ? extends T> onAppl,
        Function1<? super IListTerm, ? extends T> onList,
        Function1<? super IStringTerm, ? extends T> onString,
        Function1<? super IIntTerm, ? extends T> onInt,
        Function1<? super IBlobTerm, ? extends T> onBlob,
        Function1<? super ITermVar, ? extends T> onVar
        // @formatter:on
    ) {
        return new ITerm.Cases<T>() {

            @Override public T caseAppl(IApplTerm appl) {
                return onAppl.apply(appl);
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

            @Override public T caseBlob(IBlobTerm blob) {
                return onBlob.apply(blob);
            }

            @Override public T caseVar(ITermVar var) {
                return onVar.apply(var);
            }

        };
    }

    public static <T> CaseBuilder<T> cases() {
        return new CaseBuilder<>();
    }

    public static class CaseBuilder<T> {

        private Function1<? super IApplTerm, ? extends T> onAppl = null;
        private Function1<? super IListTerm, ? extends T> onList = null;
        private Function1<? super IStringTerm, ? extends T> onString = null;
        private Function1<? super IIntTerm, ? extends T> onInt = null;
        private Function1<? super IBlobTerm, ? extends T> onBlob = null;
        private Function1<? super ITermVar, ? extends T> onVar = null;

        public CaseBuilder<T> appl(Function1<? super IApplTerm, ? extends T> onAppl) {
            this.onAppl = onAppl;
            return this;
        }

        public CaseBuilder<T> list(Function1<? super IListTerm, ? extends T> onList) {
            this.onList = onList;
            return this;
        }

        public CaseBuilder<T> string(Function1<? super IStringTerm, ? extends T> onString) {
            this.onString = onString;
            return this;
        }

        public CaseBuilder<T> integer(Function1<? super IIntTerm, ? extends T> onInt) {
            this.onInt = onInt;
            return this;
        }

        public CaseBuilder<T> blob(Function1<? super IBlobTerm, ? extends T> onBlob) {
            this.onBlob = onBlob;
            return this;
        }

        public CaseBuilder<T> var(Function1<? super ITermVar, ? extends T> onVar) {
            this.onVar = onVar;
            return this;
        }

        public ITerm.Cases<T> otherwise(Function1<? super ITerm, ? extends T> otherwise) {
            return new ITerm.Cases<T>() {

                @Override public T caseAppl(IApplTerm appl) {
                    return onAppl != null ? onAppl.apply(appl) : otherwise.apply(appl);
                }

                @Override public T caseList(IListTerm list) {
                    return onList != null ? onList.apply(list) : otherwise.apply(list);
                }

                @Override public T caseString(IStringTerm string) {
                    return onString != null ? onString.apply(string) : otherwise.apply(string);
                }

                @Override public T caseInt(IIntTerm integer) {
                    return onInt != null ? onInt.apply(integer) : otherwise.apply(integer);
                }

                @Override public T caseBlob(IBlobTerm blob) {
                    return onBlob != null ? onBlob.apply(blob) : otherwise.apply(blob);
                }

                @Override public T caseVar(ITermVar var) {
                    return onVar != null ? onVar.apply(var) : otherwise.apply(var);
                }

            };
        }

    }

    public static <T> ITerm.Cases<T> casesFix(
    // @formatter:off
        Function2<ITerm.Cases<T>, ? super IApplTerm, ? extends T> onAppl,
        Function2<ITerm.Cases<T>, ? super IListTerm, ? extends T> onList,
        Function2<ITerm.Cases<T>, ? super IStringTerm, ? extends T> onString,
        Function2<ITerm.Cases<T>, ? super IIntTerm, ? extends T> onInt,
        Function2<ITerm.Cases<T>, ? super IBlobTerm, ? extends T> onBlob,
        Function2<ITerm.Cases<T>, ? super ITermVar, ? extends T> onVar
        // @formatter:on
    ) {
        return new ITerm.Cases<T>() {

            @Override public T caseAppl(IApplTerm appl) {
                return onAppl.apply(this, appl);
            }

            @Override public T caseList(IListTerm list) {
                return onList.apply(this, list);
            }

            @Override public T caseString(IStringTerm string) {
                return onString.apply(this, string);
            }

            @Override public T caseInt(IIntTerm integer) {
                return onInt.apply(this, integer);
            }

            @Override public T caseBlob(IBlobTerm blob) {
                return onBlob.apply(this, blob);
            }

            @Override public T caseVar(ITermVar var) {
                return onVar.apply(this, var);
            }

        };
    }

    // CHECKED

    public static <T, E extends Throwable> ITerm.CheckedCases<T, E> checkedCases(
    // @formatter:off
            CheckedFunction1<? super IApplTerm, T, E> onAppl, CheckedFunction1<? super IListTerm, T, E> onList,
            CheckedFunction1<? super IStringTerm, T, E> onString, CheckedFunction1<? super IIntTerm, T, E> onInt,
            CheckedFunction1<? super IBlobTerm, T, E> onBlob, CheckedFunction1<? super ITermVar, T, E> onVar
    // @formatter:on
    ) {
        return new ITerm.CheckedCases<T, E>() {

            @Override public T caseAppl(IApplTerm applTerm) throws E {
                return onAppl.apply(applTerm);
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

            @Override public T caseBlob(IBlobTerm blob) throws E {
                return onBlob.apply(blob);
            }

            @Override public T caseVar(ITermVar var) throws E {
                return onVar.apply(var);
            }

        };
    }

    // vars

    public static Set<ITermVar> unlockedVars(ITerm term) {
        return term.getVars().stream().filter(v -> !v.isLocked()).collect(Collectors.toSet());
    }

}
