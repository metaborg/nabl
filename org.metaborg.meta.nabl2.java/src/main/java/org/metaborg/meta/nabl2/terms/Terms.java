package org.metaborg.meta.nabl2.terms;

import java.util.List;
import java.util.Optional;

import org.metaborg.meta.nabl2.functions.CheckedFunction1;
import org.metaborg.meta.nabl2.functions.CheckedFunction2;
import org.metaborg.meta.nabl2.functions.CheckedFunction3;
import org.metaborg.meta.nabl2.functions.CheckedFunction4;
import org.metaborg.meta.nabl2.functions.Function1;
import org.metaborg.meta.nabl2.functions.Function2;
import org.metaborg.meta.nabl2.functions.Function3;
import org.metaborg.meta.nabl2.functions.Function4;
import org.metaborg.meta.nabl2.functions.Function5;
import org.metaborg.meta.nabl2.functions.Function6;

import com.google.common.collect.Lists;

public class Terms {

    // SAFE

    public static <T> ITerm.Cases<T> cases(
        // @formatter:off
        Function1<? super IApplTerm, ? extends T> onAppl,
        Function1<? super IListTerm, ? extends T> onList,
        Function1<? super IStringTerm, ? extends T> onString,
        Function1<? super IIntTerm, ? extends T> onInt,
        Function1<? super ITermVar, ? extends T> onVar
        // @formatter:on
    ) {
        return new ITerm.Cases<T>() {

            @Override public T caseAppl(IApplTerm applTerm) {
                return onAppl.apply(applTerm);
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

        };
    }

    public static class M {

        // term

        public static IMatcher<ITerm> term() {
            return term -> Optional.of(term);
        }

        public static <R> IMatcher<R> term(Function1<? super ITerm,? extends R> f) {
            return term -> Optional.of(f.apply(term));
        }


        // appl

        public static IMatcher<IApplTerm> appl() {
            return term -> term.match(Terms.cases(Optional::of, Terms::empty, Terms::empty, Terms::empty,
                    Terms::empty));
        }

        public static <R> IMatcher<R> appl(Function1<IApplTerm,? extends R> f) {
            return term -> term.match(Terms.cases(appl -> Optional.of(f.apply(appl)), Terms::empty, Terms::empty,
                    Terms::empty, Terms::empty));
        }

        public static <R> IMatcher<R> appl(String op, Function1<IApplTerm,? extends R> f) {
            return term -> term.match(Terms.cases(appl -> appl.getOp().equals(op) ? Optional.of(f.apply(appl))
                    : Optional.empty(), Terms::empty, Terms::empty, Terms::empty, Terms::empty));
        }


        public static IMatcher<IApplTerm> appl0(String op) {
            return appl0(op, (appl) -> appl);
        }

        public static <T, R> IMatcher<R> appl0(String op, Function1<? super IApplTerm,? extends R> f) {
            return term -> {
                return term.match(Terms.cases(appl -> {
                    if (!(op.equals(appl.getOp()) && appl.getArity() == 0)) {
                        return Optional.empty();
                    }
                    return Optional.of(f.apply(appl));
                }, Terms::empty, Terms::empty, Terms::empty, Terms::empty));
            };
        }


        public static <T> IMatcher<IApplTerm> appl1(String op, IMatcher<? extends T> m) {
            return appl1(op, m, (appl, t) -> appl);
        }

        public static <T, R> IMatcher<R> appl1(String op, IMatcher<? extends T> m,
                Function2<? super IApplTerm,? super T,? extends R> f) {
            return term -> {
                return term.match(Terms.cases(appl -> {
                    if (!(op.equals(appl.getOp()) && appl.getArity() == 1)) {
                        return Optional.empty();
                    }
                    Optional<? extends T> o1 = m.match(appl.getArgs().get(0));
                    if (!o1.isPresent()) {
                        return Optional.empty();
                    }
                    T t = o1.get();
                    return Optional.of(f.apply(appl, t));
                }, Terms::empty, Terms::empty, Terms::empty, Terms::empty));
            };
        }


        public static <T1, T2> IMatcher<IApplTerm> appl2(String op, IMatcher<? extends T1> m1,
                IMatcher<? extends T2> m2) {
            return appl2(op, m1, m2, (appl, t1, t2) -> appl);
        }

        public static <T1, T2, R> IMatcher<R> appl2(String op, IMatcher<? extends T1> m1, IMatcher<? extends T2> m2,
                Function3<? super IApplTerm,? super T1,? super T2,? extends R> f) {
            return term -> {
                return term.match(Terms.cases(appl -> {
                    if (!(op.equals(appl.getOp()) && appl.getArity() == 2)) {
                        return Optional.empty();
                    }
                    Optional<? extends T1> o1 = m1.match(appl.getArgs().get(0));
                    if (!o1.isPresent()) {
                        return Optional.empty();
                    }
                    T1 t1 = o1.get();
                    Optional<? extends T2> o2 = m2.match(appl.getArgs().get(1));
                    if (!o2.isPresent()) {
                        return Optional.empty();
                    }
                    T2 t2 = o2.get();
                    return Optional.of(f.apply(appl, t1, t2));
                }, Terms::empty, Terms::empty, Terms::empty, Terms::empty));
            };
        }


        public static <T1, T2, T3> IMatcher<IApplTerm> appl3(String op, IMatcher<? extends T1> m1,
                IMatcher<? extends T2> m2, IMatcher<T3> m3) {
            return appl3(op, m1, m2, m3, (appl, t1, t2, t3) -> appl);
        }

        public static <T1, T2, T3, R> IMatcher<R> appl3(String op, IMatcher<? extends T1> m1, IMatcher<? extends T2> m2,
                IMatcher<? extends T3> m3,
                Function4<? super IApplTerm,? super T1,? super T2,? super T3,? extends R> f) {
            return term -> {
                return term.match(Terms.cases(appl -> {
                    if (!(op.equals(appl.getOp()) && appl.getArity() == 3)) {
                        return Optional.empty();
                    }
                    Optional<? extends T1> o1 = m1.match(appl.getArgs().get(0));
                    if (!o1.isPresent()) {
                        return Optional.empty();
                    }
                    T1 t1 = o1.get();
                    Optional<? extends T2> o2 = m2.match(appl.getArgs().get(1));
                    if (!o2.isPresent()) {
                        return Optional.empty();
                    }
                    T2 t2 = o2.get();
                    Optional<? extends T3> o3 = m3.match(appl.getArgs().get(2));
                    if (!o3.isPresent()) {
                        return Optional.empty();
                    }
                    T3 t3 = o3.get();
                    return Optional.of(f.apply(appl, t1, t2, t3));
                }, Terms::empty, Terms::empty, Terms::empty, Terms::empty));
            };
        }


        public static <T1, T2, T3, T4> IMatcher<IApplTerm> appl4(String op, IMatcher<? extends T1> m1,
                IMatcher<? extends T2> m2, IMatcher<T3> m3, IMatcher<T4> m4) {
            return appl4(op, m1, m2, m3, m4, (appl, t1, t2, t3, t4) -> appl);
        }

        public static <T1, T2, T3, T4, R> IMatcher<R> appl4(String op, IMatcher<? extends T1> m1,
                IMatcher<? extends T2> m2, IMatcher<? extends T3> m3, IMatcher<? extends T4> m4,
                Function5<? super IApplTerm,? super T1,? super T2,? super T3,? super T4,? extends R> f) {
            return term -> {
                return term.match(Terms.cases(appl -> {
                    if (!(op.equals(appl.getOp()) && appl.getArity() == 4)) {
                        return Optional.empty();
                    }
                    Optional<? extends T1> o1 = m1.match(appl.getArgs().get(0));
                    if (!o1.isPresent()) {
                        return Optional.empty();
                    }
                    T1 t1 = o1.get();
                    Optional<? extends T2> o2 = m2.match(appl.getArgs().get(1));
                    if (!o2.isPresent()) {
                        return Optional.empty();
                    }
                    T2 t2 = o2.get();
                    Optional<? extends T3> o3 = m3.match(appl.getArgs().get(2));
                    if (!o3.isPresent()) {
                        return Optional.empty();
                    }
                    T3 t3 = o3.get();
                    Optional<? extends T4> o4 = m4.match(appl.getArgs().get(3));
                    if (!o4.isPresent()) {
                        return Optional.empty();
                    }
                    T4 t4 = o4.get();
                    return Optional.of(f.apply(appl, t1, t2, t3, t4));
                }, Terms::empty, Terms::empty, Terms::empty, Terms::empty));
            };
        }


        public static <T1, T2, T3, T4, T5> IMatcher<IApplTerm> appl5(String op, IMatcher<? extends T1> m1,
                IMatcher<? extends T2> m2, IMatcher<T3> m3, IMatcher<T4> m4, IMatcher<T5> m5) {
            return appl5(op, m1, m2, m3, m4, m5, (appl, t1, t2, t3, t4, t5) -> appl);
        }

        public static <T1, T2, T3, T4, T5, R> IMatcher<R> appl5(String op, IMatcher<? extends T1> m1,
                IMatcher<? extends T2> m2, IMatcher<? extends T3> m3, IMatcher<? extends T4> m4, IMatcher<? extends T5> m5,
                Function6<? super IApplTerm,? super T1,? super T2,? super T3,? super T4,? super T5,? extends R> f) {
            return term -> {
                return term.match(Terms.cases(appl -> {
                    if (!(op.equals(appl.getOp()) && appl.getArity() == 5)) {
                        return Optional.empty();
                    }
                    Optional<? extends T1> o1 = m1.match(appl.getArgs().get(0));
                    if (!o1.isPresent()) {
                        return Optional.empty();
                    }
                    T1 t1 = o1.get();
                    Optional<? extends T2> o2 = m2.match(appl.getArgs().get(1));
                    if (!o2.isPresent()) {
                        return Optional.empty();
                    }
                    T2 t2 = o2.get();
                    Optional<? extends T3> o3 = m3.match(appl.getArgs().get(2));
                    if (!o3.isPresent()) {
                        return Optional.empty();
                    }
                    T3 t3 = o3.get();
                    Optional<? extends T4> o4 = m4.match(appl.getArgs().get(3));
                    if (!o4.isPresent()) {
                        return Optional.empty();
                    }
                    T4 t4 = o4.get();
                    Optional<? extends T5> o5 = m5.match(appl.getArgs().get(4));
                    if (!o5.isPresent()) {
                        return Optional.empty();
                    }
                    T5 t5 = o5.get();
                    return Optional.of(f.apply(appl, t1, t2, t3, t4, t5));
                }, Terms::empty, Terms::empty, Terms::empty, Terms::empty));
            };
        }

        // list

        public static IMatcher<IListTerm> list() {
            return term -> term.match(Terms.cases(Terms::empty, Optional::of, Terms::empty, Terms::empty,
                    Terms::empty));
        }

        public static <R> IMatcher<R> list(Function1<? super IListTerm,R> f) {
            return term -> term.match(Terms.cases(Terms::empty, list -> Optional.of(f.apply(list)), Terms::empty,
                    Terms::empty, Terms::empty));
        }

        public static <T> IMatcher<Iterable<? extends T>> listElems(IMatcher<? extends T> m) {
            return listElems(m, (t, ts) -> ts);
        }

        public static <T, R> IMatcher<R> listElems(IMatcher<? extends T> m,
                Function2<? super IListTerm,Iterable<T>,? extends R> f) {
            return term -> {
                return term.match(Terms.cases(Terms::empty, list -> {
                    List<T> ts = Lists.newArrayList();
                    for (ITerm t : list) {
                        Optional<? extends T> o = m.match(t);
                        if (!o.isPresent()) {
                            return Optional.empty();
                        }
                        ts.add(o.get());
                    }
                    return Optional.of(f.apply(list, ts));
                }, Terms::empty, Terms::empty, Terms::empty));
            };
        }


        // string

        public static IMatcher<IStringTerm> string() {
            return term -> term.match(Terms.cases(Terms::empty, Terms::empty, Optional::of, Terms::empty,
                    Terms::empty));
        }

        public static <R> IMatcher<R> string(Function1<? super IStringTerm,R> f) {
            return term -> term.match(Terms.cases(Terms::empty, Terms::empty, string -> Optional.of(f.apply(string)),
                    Terms::empty, Terms::empty));
        }

        public static IMatcher<String> stringValue() {
            return term -> term.match(Terms.cases(Terms::empty, Terms::empty, string -> Optional.of(string.getValue()),
                    Terms::empty, Terms::empty));
        }

        public static IMatcher<IStringTerm> stringValue(String value) {
            return term -> term.match(Terms.cases(Terms::empty, Terms::empty, string -> string.getValue().equals(value)
                    ? Optional.of(string) : Optional.empty(), Terms::empty, Terms::empty));
        }

        // integer

        public static IMatcher<IIntTerm> integer() {
            return term -> term.match(Terms.cases(Terms::empty, Terms::empty, Terms::empty, Optional::of,
                    Terms::empty));
        }

        public static <R> IMatcher<R> integer(Function1<? super IIntTerm,R> f) {
            return term -> term.match(Terms.cases(Terms::empty, Terms::empty, Terms::empty, integer -> Optional.of(f
                    .apply(integer)), Terms::empty));
        }

        public static IMatcher<Integer> integerValue() {
            return term -> term.match(Terms.cases(Terms::empty, Terms::empty, Terms::empty, integer -> Optional.of(
                    integer.getValue()), Terms::empty));
        }

        public static IMatcher<Integer> integerValue(int value) {
            return term -> term.match(Terms.cases(Terms::empty, Terms::empty, Terms::empty, integer -> (integer
                    .getValue() == value) ? Optional.of(value) : Optional.empty(), Terms::empty));
        }

        // var

        public static IMatcher<ITermVar> var() {
            return term -> term.match(Terms.cases(Terms::empty, Terms::empty, Terms::empty, Terms::empty,
                    Optional::of));
        }

        public static <R> IMatcher<R> var(Function1<? super ITermVar,R> f) {
            return term -> term.match(Terms.cases(Terms::empty, Terms::empty, Terms::empty, Terms::empty,
                    var -> Optional.of(f.apply(var))));
        }

        // cases

        @SafeVarargs public static <T> IMatcher<T> cases(IMatcher<? extends T>... matchers) {
            return term -> {
                for (IMatcher<? extends T> matcher : matchers) {
                    Optional<? extends T> result = matcher.match(term);
                    if (result.isPresent()) {
                        return Optional.of(result.get());
                    }
                }
                return Optional.empty();
            };
        }

        public static <T> IMatcher<T> casesFix(Function1<IMatcher<T>,Iterable<IMatcher<? extends T>>> f) {
            return term -> {
                for (IMatcher<? extends T> matcher : f.apply(casesFix(f))) {
                    Optional<? extends T> result = matcher.match(term);
                    if (result.isPresent()) {
                        return Optional.of(result.get());
                    }
                }
                return Optional.empty();
            };
        }

    }

    @FunctionalInterface
    public interface IMatcher<T> {

        Optional<T> match(ITerm term);

    }

    // CHECKED

    public static <T, E extends Throwable> ITerm.CheckedCases<T,E> checkedCases(
        // @formatter:off
        CheckedFunction1<? super IApplTerm, T, E> onAppl,
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

        };
    }

    public static class CM {

        // appl

        public static <R, E extends Throwable> ICheckedMatcher<R,E> appl(
                CheckedFunction1<? super IApplTerm,? extends R,? extends E> f) {
            return term -> term.matchOrThrow(Terms.checkedCases(appl -> Optional.of(f.apply(appl)), Terms::empty,
                    Terms::empty, Terms::empty, Terms::empty));
        }

        public static <T, R, E extends Throwable> ICheckedMatcher<R,E> appl0(String op,
                CheckedFunction1<? super IApplTerm,? extends R,? extends E> f) {
            return term -> {
                return term.matchOrThrow(Terms.checkedCases(appl -> {
                    if (!(op.equals(appl.getOp()) && appl.getArity() == 0)) {
                        return Optional.empty();
                    }
                    return Optional.of(f.apply(appl));
                }, Terms::empty, Terms::empty, Terms::empty, Terms::empty));
            };
        }

        public static <T, R, E extends Throwable> ICheckedMatcher<R,E> appl1(String op,
                ICheckedMatcher<? extends T,? extends E> m,
                CheckedFunction2<? super IApplTerm,? super T,? extends R,? extends E> f) {
            return term -> {
                return term.matchOrThrow(Terms.checkedCases(appl -> {
                    if (!(op.equals(appl.getOp()) && appl.getArity() == 1)) {
                        return Optional.empty();
                    }
                    Optional<? extends T> o1 = m.matchOrThrow(appl.getArgs().get(0));
                    if (!o1.isPresent()) {
                        return Optional.empty();
                    }
                    T t = o1.get();
                    return Optional.of(f.apply(appl, t));
                }, Terms::empty, Terms::empty, Terms::empty, Terms::empty));
            };
        }

        public static <T1, T2, R, E extends Throwable> ICheckedMatcher<R,E> appl2(String op,
                ICheckedMatcher<? extends T1,? extends E> m1, ICheckedMatcher<? extends T2,? extends E> m2,
                CheckedFunction3<? super IApplTerm,? super T1,? super T2,? extends R,? extends E> f) {
            return term -> {
                return term.matchOrThrow(Terms.checkedCases(appl -> {
                    if (!(op.equals(appl.getOp()) && appl.getArity() == 2)) {
                        return Optional.empty();
                    }
                    Optional<? extends T1> o1 = m1.matchOrThrow(appl.getArgs().get(0));
                    if (!o1.isPresent()) {
                        return Optional.empty();
                    }
                    T1 t1 = o1.get();
                    Optional<? extends T2> o2 = m2.matchOrThrow(appl.getArgs().get(1));
                    if (!o2.isPresent()) {
                        return Optional.empty();
                    }
                    T2 t2 = o2.get();
                    return Optional.of(f.apply(appl, t1, t2));
                }, Terms::empty, Terms::empty, Terms::empty, Terms::empty));
            };
        }

        public static <T1, T2, T3, R, E extends Throwable> ICheckedMatcher<R,E> appl3(String op,
                ICheckedMatcher<? extends T1,? extends E> m1, ICheckedMatcher<? extends T2,? extends E> m2,
                ICheckedMatcher<? extends T3,? extends E> m3,
                CheckedFunction4<? super IApplTerm,? super T1,? super T2,? super T3,? extends R,? extends E> f) {
            return term -> {
                return term.matchOrThrow(Terms.checkedCases(appl -> {
                    if (!(op.equals(appl.getOp()) && appl.getArity() == 3)) {
                        return Optional.empty();
                    }
                    Optional<? extends T1> o1 = m1.matchOrThrow(appl.getArgs().get(0));
                    if (!o1.isPresent()) {
                        return Optional.empty();
                    }
                    T1 t1 = o1.get();
                    Optional<? extends T2> o2 = m2.matchOrThrow(appl.getArgs().get(1));
                    if (!o2.isPresent()) {
                        return Optional.empty();
                    }
                    T2 t2 = o2.get();
                    Optional<? extends T3> o3 = m3.matchOrThrow(appl.getArgs().get(2));
                    if (!o3.isPresent()) {
                        return Optional.empty();
                    }
                    T3 t3 = o3.get();
                    return Optional.of(f.apply(appl, t1, t2, t3));
                }, Terms::empty, Terms::empty, Terms::empty, Terms::empty));
            };
        }

        // list

        public static <R, E extends Throwable> ICheckedMatcher<R,E> list(
                CheckedFunction1<? super IListTerm,? extends R,? extends E> f) {
            return term -> term.matchOrThrow(Terms.checkedCases(Terms::empty, list -> Optional.of(f.apply(list)),
                    Terms::empty, Terms::empty, Terms::empty));
        }

        public static <T, R, E extends Throwable> ICheckedMatcher<R,E> listElems(
                ICheckedMatcher<? extends T,? extends E> m,
                CheckedFunction2<? super IListTerm,Iterable<T>,? extends R,? extends E> f) {
            return term -> {
                return term.matchOrThrow(Terms.checkedCases(Terms::empty, list -> {
                    List<T> ts = Lists.newArrayList();
                    for (ITerm t : list) {
                        Optional<? extends T> o = m.matchOrThrow(t);
                        if (!o.isPresent()) {
                            return Optional.empty();
                        }
                        ts.add(o.get());
                    }
                    return Optional.of(f.apply(list, ts));
                }, Terms::empty, Terms::empty, Terms::empty));
            };
        }

        // integer

        public static <R, E extends Throwable> ICheckedMatcher<R,E> integer(
                CheckedFunction1<IIntTerm,? extends R,? extends E> f) {
            return term -> term.matchOrThrow(Terms.checkedCases(Terms::empty, Terms::empty, Terms::empty,
                    string -> Optional.of(f.apply(string)), Terms::empty));
        }

        // string

        public static <R, E extends Throwable> ICheckedMatcher<R,E> string(
                CheckedFunction1<IStringTerm,? extends R,? extends E> f) {
            return term -> term.matchOrThrow(Terms.checkedCases(Terms::empty, Terms::empty, string -> Optional.of(f
                    .apply(string)), Terms::empty, Terms::empty));
        }

        // var

        public static <R, E extends Throwable> ICheckedMatcher<R,E> var(
                CheckedFunction1<? super ITermVar,R,? extends E> f) {
            return term -> term.matchOrThrow(Terms.checkedCases(Terms::empty, Terms::empty, Terms::empty, Terms::empty,
                    var -> Optional.of(f.apply(var))));
        }

        // cases

        @SafeVarargs public static <T, E extends Throwable> ICheckedMatcher<T,E> cases(
                ICheckedMatcher<? extends T,? extends E>... matchers) {
            return term -> {
                for (ICheckedMatcher<? extends T,? extends E> matcher : matchers) {
                    Optional<? extends T> result = matcher.matchOrThrow(term);
                    if (result.isPresent()) {
                        return Optional.of(result.get());
                    }
                }
                return Optional.empty();
            };
        }

    }

    @FunctionalInterface
    public interface ICheckedMatcher<T, E extends Throwable> {

        Optional<T> matchOrThrow(ITerm term) throws E;

    }

    // util

    private static <T> Optional<T> empty(ITerm term) {
        return Optional.empty();
    }

}