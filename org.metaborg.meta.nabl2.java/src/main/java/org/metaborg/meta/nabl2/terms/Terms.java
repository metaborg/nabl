package org.metaborg.meta.nabl2.terms;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.meta.nabl2.util.Optionals;
import org.metaborg.meta.nabl2.util.Unit;
import org.metaborg.meta.nabl2.util.functions.CheckedFunction1;
import org.metaborg.meta.nabl2.util.functions.CheckedFunction2;
import org.metaborg.meta.nabl2.util.functions.CheckedFunction3;
import org.metaborg.meta.nabl2.util.functions.CheckedFunction4;
import org.metaborg.meta.nabl2.util.functions.Function1;
import org.metaborg.meta.nabl2.util.functions.Function2;
import org.metaborg.meta.nabl2.util.functions.Function3;
import org.metaborg.meta.nabl2.util.functions.Function4;
import org.metaborg.meta.nabl2.util.functions.Function5;
import org.metaborg.meta.nabl2.util.functions.Function6;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.Lists;

public class Terms {

    public static final String TUPLE_OP = "";

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

            @Override public T caseVar(ITermVar var) {
                return onVar.apply(var);
            }

        };
    }

    public static <T> ITerm.Cases<T> casesFix(
        // @formatter:off
        Function2<ITerm.Cases<T>, ? super IApplTerm, ? extends T> onAppl,
        Function2<ITerm.Cases<T>, ? super IListTerm, ? extends T> onList,
        Function2<ITerm.Cases<T>, ? super IStringTerm, ? extends T> onString,
        Function2<ITerm.Cases<T>, ? super IIntTerm, ? extends T> onInt,
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

            @Override public T caseVar(ITermVar var) {
                return onVar.apply(this, var);
            }

        };
    }

    public static class M {

        // term

        public static IMatcher<ITerm> term() {
            return term -> Optional.of(term);
        }

        public static <R> IMatcher<R> term(Function1<? super ITerm, R> f) {
            return term -> Optional.of(f.apply(term));
        }

        public static <T, R> IMatcher<R> term(IMatcher<? extends T> m, Function2<? super ITerm, ? super T, R> f) {
            return term -> m.match(term).map(t -> f.apply(term, t));
        }

        // appl

        public static IMatcher<IApplTerm> appl() {
            return term -> term.match(Terms.<Optional<IApplTerm>>cases(Optional::of, Terms::empty, Terms::empty,
                    Terms::empty, Terms::empty));
        }

        public static <R> IMatcher<R> appl(String op, Function1<? super IApplTerm, R> f) {
            return flatten(appl(appl -> appl.getOp().equals(op) ? Optional.of(f.apply(appl)) : Optional.empty()));
        }

        public static <R> IMatcher<R> appl(Function1<? super IApplTerm, R> f) {
            return term -> term.match(Terms.<Optional<R>>cases(appl -> Optional.of(f.apply(appl)), Terms::empty,
                    Terms::empty, Terms::empty, Terms::empty));
        }

        public static IMatcher<IApplTerm> appl0(String op) {
            return appl0(op, (appl) -> appl);
        }

        public static <T, R> IMatcher<R> appl0(String op, Function1<? super IApplTerm, R> f) {
            return term -> {
                return term.match(Terms.<Optional<R>>cases(appl -> {
                    if(!(op.equals(appl.getOp()) && appl.getArity() == 0)) {
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
                Function2<? super IApplTerm, ? super T, R> f) {
            return term -> {
                return term.match(Terms.<Optional<R>>cases(appl -> {
                    if(!(op.equals(appl.getOp()) && appl.getArity() == 1)) {
                        return Optional.empty();
                    }
                    return m.match(appl.getArgs().get(0)).map(t -> f.apply(appl, t));
                }, Terms::empty, Terms::empty, Terms::empty, Terms::empty));
            };
        }

        public static <T1, T2> IMatcher<IApplTerm> appl2(String op, IMatcher<? extends T1> m1,
                IMatcher<? extends T2> m2) {
            return appl2(op, m1, m2, (appl, t1, t2) -> appl);
        }

        public static <T1, T2, R> IMatcher<R> appl2(String op, IMatcher<? extends T1> m1, IMatcher<? extends T2> m2,
                Function3<? super IApplTerm, ? super T1, ? super T2, R> f) {
            return term -> {
                return term.match(Terms.<Optional<R>>cases(appl -> {
                    if(!(op.equals(appl.getOp()) && appl.getArity() == 2)) {
                        return Optional.empty();
                    }
                    Optional<? extends T1> o1 = m1.match(appl.getArgs().get(0));
                    Optional<? extends T2> o2 = m2.match(appl.getArgs().get(1));
                    return Optionals.lift(o1, o2, (t1, t2) -> f.apply(appl, t1, t2));
                }, Terms::empty, Terms::empty, Terms::empty, Terms::empty));
            };
        }

        public static <T1, T2, T3> IMatcher<IApplTerm> appl3(String op, IMatcher<? extends T1> m1,
                IMatcher<? extends T2> m2, IMatcher<T3> m3) {
            return appl3(op, m1, m2, m3, (appl, t1, t2, t3) -> appl);
        }

        public static <T1, T2, T3, R> IMatcher<R> appl3(String op, IMatcher<? extends T1> m1, IMatcher<? extends T2> m2,
                IMatcher<? extends T3> m3, Function4<? super IApplTerm, ? super T1, ? super T2, ? super T3, R> f) {
            return term -> {
                return term.match(Terms.<Optional<R>>cases(appl -> {
                    if(!(op.equals(appl.getOp()) && appl.getArity() == 3)) {
                        return Optional.empty();
                    }
                    Optional<? extends T1> o1 = m1.match(appl.getArgs().get(0));
                    Optional<? extends T2> o2 = m2.match(appl.getArgs().get(1));
                    Optional<? extends T3> o3 = m3.match(appl.getArgs().get(2));
                    return Optionals.lift(o1, o2, o3, (t1, t2, t3) -> f.apply(appl, t1, t2, t3));
                }, Terms::empty, Terms::empty, Terms::empty, Terms::empty));
            };
        }

        public static <T1, T2, T3, T4> IMatcher<IApplTerm> appl4(String op, IMatcher<? extends T1> m1,
                IMatcher<? extends T2> m2, IMatcher<T3> m3, IMatcher<T4> m4) {
            return appl4(op, m1, m2, m3, m4, (appl, t1, t2, t3, t4) -> appl);
        }

        public static <T1, T2, T3, T4, R> IMatcher<R> appl4(String op, IMatcher<? extends T1> m1,
                IMatcher<? extends T2> m2, IMatcher<? extends T3> m3, IMatcher<? extends T4> m4,
                Function5<? super IApplTerm, ? super T1, ? super T2, ? super T3, ? super T4, R> f) {
            return term -> {
                return term.match(Terms.<Optional<R>>cases(appl -> {
                    if(!(op.equals(appl.getOp()) && appl.getArity() == 4)) {
                        return Optional.empty();
                    }
                    Optional<? extends T1> o1 = m1.match(appl.getArgs().get(0));
                    Optional<? extends T2> o2 = m2.match(appl.getArgs().get(1));
                    Optional<? extends T3> o3 = m3.match(appl.getArgs().get(2));
                    Optional<? extends T4> o4 = m4.match(appl.getArgs().get(3));
                    return Optionals.lift(o1, o2, o3, o4, (t1, t2, t3, t4) -> f.apply(appl, t1, t2, t3, t4));
                }, Terms::empty, Terms::empty, Terms::empty, Terms::empty));
            };
        }

        public static <T1, T2, T3, T4, T5> IMatcher<IApplTerm> appl5(String op, IMatcher<? extends T1> m1,
                IMatcher<? extends T2> m2, IMatcher<T3> m3, IMatcher<T4> m4, IMatcher<T5> m5) {
            return appl5(op, m1, m2, m3, m4, m5, (appl, t1, t2, t3, t4, t5) -> appl);
        }

        public static <T1, T2, T3, T4, T5, R> IMatcher<R> appl5(String op, IMatcher<? extends T1> m1,
                IMatcher<? extends T2> m2, IMatcher<? extends T3> m3, IMatcher<? extends T4> m4,
                IMatcher<? extends T5> m5,
                Function6<? super IApplTerm, ? super T1, ? super T2, ? super T3, ? super T4, ? super T5, R> f) {
            return term -> {
                return term.match(Terms.<Optional<R>>cases(appl -> {
                    if(!(op.equals(appl.getOp()) && appl.getArity() == 5)) {
                        return Optional.empty();
                    }
                    Optional<? extends T1> o1 = m1.match(appl.getArgs().get(0));
                    Optional<? extends T2> o2 = m2.match(appl.getArgs().get(1));
                    Optional<? extends T3> o3 = m3.match(appl.getArgs().get(2));
                    Optional<? extends T4> o4 = m4.match(appl.getArgs().get(3));
                    Optional<? extends T5> o5 = m5.match(appl.getArgs().get(4));
                    return Optionals.lift(o1, o2, o3, o4, o5,
                            (t1, t2, t3, t4, t5) -> f.apply(appl, t1, t2, t3, t4, t5));
                }, Terms::empty, Terms::empty, Terms::empty, Terms::empty));
            };
        }

        // tuple

        public static <R> IMatcher<R> tuple(Function1<? super IApplTerm, R> f) {
            return M.appl(TUPLE_OP, f);
        }

        public static <R> IMatcher<R> tuple0(Function1<? super IApplTerm, R> f) {
            return M.appl0(TUPLE_OP, f);
        }

        public static <T> IMatcher<IApplTerm> tuple1(IMatcher<? extends T> m) {
            return M.appl1(TUPLE_OP, m);
        }

        public static <T, R> IMatcher<R> tuple1(IMatcher<? extends T> m, Function2<? super IApplTerm, ? super T, R> f) {
            return M.appl1(TUPLE_OP, m, f);
        }

        public static <T1, T2> IMatcher<IApplTerm> tuple2(IMatcher<? extends T1> m1, IMatcher<? extends T2> m2) {
            return M.appl2(TUPLE_OP, m1, m2);
        }

        public static <T1, T2, R> IMatcher<R> tuple2(IMatcher<? extends T1> m1, IMatcher<? extends T2> m2,
                Function3<? super IApplTerm, ? super T1, ? super T2, R> f) {
            return M.appl2(TUPLE_OP, m1, m2, f);
        }

        public static <T1, T2, T3> IMatcher<IApplTerm> tuple3(IMatcher<? extends T1> m1, IMatcher<? extends T2> m2,
                IMatcher<? extends T3> m3) {
            return M.appl3(TUPLE_OP, m1, m2, m3);
        }

        public static <T1, T2, T3, R> IMatcher<R> tuple3(IMatcher<? extends T1> m1, IMatcher<? extends T2> m2,
                IMatcher<? extends T3> m3, Function4<? super IApplTerm, ? super T1, ? super T2, ? super T3, R> f) {
            return M.appl3(TUPLE_OP, m1, m2, m3, f);
        }

        public static <T1, T2, T3, T4> IMatcher<IApplTerm> tuple4(IMatcher<? extends T1> m1, IMatcher<? extends T2> m2,
                IMatcher<? extends T3> m3, IMatcher<? extends T4> m4) {
            return M.appl4(TUPLE_OP, m1, m2, m3, m4);
        }

        public static <T1, T2, T3, T4, R> IMatcher<R> tuple4(IMatcher<? extends T1> m1, IMatcher<? extends T2> m2,
                IMatcher<? extends T3> m3, IMatcher<? extends T4> m4,
                Function5<? super IApplTerm, ? super T1, ? super T2, ? super T3, ? super T4, R> f) {
            return M.appl4(TUPLE_OP, m1, m2, m3, m4, f);
        }

        // list

        public static IMatcher<IListTerm> list() {
            return list((l) -> l);
        }

        public static <R> IMatcher<R> list(Function1<? super IListTerm, R> f) {
            return term -> term.match(Terms.<Optional<R>>cases(Terms::empty, list -> Optional.of(f.apply(list)),
                    Terms::empty, Terms::empty, Terms::empty));
        }

        public static <T> IMatcher<? extends List<? extends ITerm>> listElems() {
            return listElems(M.term());
        }

        public static <T> IMatcher<List<T>> listElems(IMatcher<T> m) {
            return listElems(m, (t, ts) -> ts);
        }

        public static <T, R> IMatcher<R> listElems(IMatcher<T> m, Function2<? super IListTerm, ? super List<T>, R> f) {
            return term -> {
                return term.match(Terms.<Optional<R>>cases(Terms::empty, list -> {
                    List<Optional<T>> os = Lists.newArrayList();
                    for(ITerm t : list) {
                        os.add(m.match(t));
                    }
                    return Optionals.sequence(os).map(ts -> (R) f.apply(list, Lists.newArrayList(ts)));
                }, Terms::empty, Terms::empty, Terms::empty));
            };
        }

        public static <R> IMatcher<R> cons(Function1<? super IConsTerm, R> f) {
            return term -> term.match(Terms.<Optional<R>>cases(Terms::empty, list -> {
                return list.match(ListTerms.<Optional<R>>cases(cons -> Optional.of(f.apply(cons)),
                        nil -> Optional.empty(), var -> Optional.empty()));
            }, Terms::empty, Terms::empty, Terms::empty));

        }

        public static <R> IMatcher<R> nil(Function1<? super INilTerm, R> f) {
            return term -> term.match(Terms.<Optional<R>>cases(Terms::empty, list -> {
                return list.match(ListTerms.<Optional<R>>cases(cons -> Optional.empty(),
                        nil -> Optional.of(f.apply(nil)), var -> Optional.empty()));
            }, Terms::empty, Terms::empty, Terms::empty));

        }

        // string

        public static IMatcher<IStringTerm> string() {
            return string(s -> s);
        }

        public static <R> IMatcher<R> string(Function1<? super IStringTerm, R> f) {
            return term -> term.match(Terms.<Optional<R>>cases(Terms::empty, Terms::empty,
                    string -> Optional.of(f.apply(string)), Terms::empty, Terms::empty));
        }

        public static IMatcher<String> stringValue() {
            return string(s -> s.getValue());
        }

        // integer

        public static IMatcher<IIntTerm> integer() {
            return integer(i -> i);
        }

        public static <R> IMatcher<R> integer(Function1<? super IIntTerm, R> f) {
            return term -> term.match(Terms.<Optional<R>>cases(Terms::empty, Terms::empty, Terms::empty,
                    integer -> Optional.of(f.apply(integer)), Terms::empty));
        }

        public static IMatcher<Integer> integerValue() {
            return integer(i -> i.getValue());
        }

        // var

        public static IMatcher<ITermVar> var() {
            return var(v -> v);
        }

        public static <R> IMatcher<R> var(Function1<? super ITermVar, R> f) {
            return term -> term.match(Terms.<Optional<R>>cases(Terms::empty, Terms::empty, Terms::empty, Terms::empty,
                    var -> Optional.of(f.apply(var))));
        }

        // optionals

        public static <R> IMatcher<R> flatten(IMatcher<Optional<R>> m) {
            return term -> m.match(term).flatMap(o -> o);
        }

        // cases

        @SafeVarargs public static <T> IMatcher<T> cases(IMatcher<? extends T>... matchers) {
            return term -> {
                for(IMatcher<? extends T> matcher : matchers) {
                    Optional<? extends T> result = matcher.match(term);
                    if(result.isPresent()) {
                        return Optional.of(result.get());
                    }
                }
                return Optional.empty();
            };
        }

        public static <T> IMatcher<T> casesFix(Function1<IMatcher<T>, Iterable<IMatcher<? extends T>>> f) {
            return term -> {
                for(IMatcher<? extends T> matcher : f.apply(casesFix(f))) {
                    Optional<? extends T> result = matcher.match(term);
                    if(result.isPresent()) {
                        return Optional.of(result.get());
                    }
                }
                return Optional.empty();
            };
        }

        public static Function1<ITerm, ITerm> sometd(IMatcher<ITerm> m) {
            return term -> m.match(term)
                    .orElseGet(
                            () -> term
                                    .match(Terms
                                            .<ITerm>cases(
                // @formatter:off
                (appl) -> TB.newAppl(appl.getOp(), appl.getArgs().stream().map(arg -> sometd(m).apply(arg))::iterator, appl.getAttachments()),
                (list) -> list.match(ListTerms.<IListTerm> cases(
                    (cons) -> TB.newCons(sometd(m).apply(cons.getHead()), (IListTerm) sometd(m).apply(cons.getTail()), cons.getAttachments()),
                    (nil) -> nil,
                    (var) -> var
                )),
                (string) -> string,
                (integer) -> integer,
                (var) -> var
                // @formatter:on
                            )));
        }

        public static Function1<ITerm, ITerm> somebu(IMatcher<ITerm> m) {
            return term -> {
                ITerm next =
                        term.match(
                                Terms.<ITerm>cases(
                    // @formatter:off
                    (appl) -> TB.newAppl(appl.getOp(), appl.getArgs().stream().map(arg -> somebu(m).apply(arg))::iterator, appl.getAttachments()),
                    (list) -> list.match(ListTerms.<IListTerm> cases(
                        (cons) -> TB.newCons(somebu(m).apply(cons.getHead()), (IListTerm) somebu(m).apply(cons.getTail()), cons.getAttachments()),
                        (nil) -> nil,
                        (var) -> var
                    )),
                    (string) -> string,
                    (integer) -> integer,
                    (var) -> var
                    // @formatter:on
                ));
                return m.match(next).orElse(next);
            };
        }

        public static <R> Function1<ITerm, Collection<R>> collecttd(IMatcher<? extends R> m) {
            return term -> {
                List<R> results = Lists.newArrayList();
                M.<Unit>casesFix(f -> Iterables2.<IMatcher<? extends Unit>>from(
                    // @formatter:off
                    t -> m.match(t).map(r -> {
                        results.add(r);
                        return Unit.unit;
                    }),
                    t -> Optional.of(t.match(Terms.<Unit>cases(
                        (appl) -> {
                            for(ITerm arg : appl.getArgs()) {
                                f.match(arg);
                            }
                            return Unit.unit;
                        },
                        (list) -> list.match(ListTerms.<Unit> cases(
                            (cons) -> {
                                f.match(cons.getHead());
                                f.match(cons.getTail());
                                return Unit.unit;
                            },
                            (nil) -> Unit.unit,
                            (var) -> Unit.unit
                        )),
                        (string) -> Unit.unit,
                        (integer) -> Unit.unit,
                        (var) -> Unit.unit
                    )))
                    // @formatter:on
                )).match(term);
                return results;
            };
        }

    }

    @FunctionalInterface
    public interface IMatcher<T> {

        Optional<T> match(ITerm term);

    }

    // CHECKED

    public static <T, E extends Throwable> ITerm.CheckedCases<T, E> checkedCases(
            // @formatter:off
            CheckedFunction1<? super IApplTerm, T, E> onAppl, CheckedFunction1<? super IListTerm, T, E> onList,
            CheckedFunction1<? super IStringTerm, T, E> onString, CheckedFunction1<? super IIntTerm, T, E> onInt,
            CheckedFunction1<? super ITermVar, T, E> onVar
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

            @Override public T caseVar(ITermVar var) throws E {
                return onVar.apply(var);
            }

        };
    }

    public static class CM {

        // term

        public static <E extends Throwable> ICheckedMatcher<ITerm, E> term() {
            return term -> Optional.of(term);
        }

        public static <R, E extends Throwable> ICheckedMatcher<R, E>
                term(CheckedFunction1<? super ITerm, R, ? extends E> f) {
            return term -> Optional.of(f.apply(term));
        }

        // appl

        public static <R, E extends Throwable> ICheckedMatcher<R, E>
                appl(CheckedFunction1<? super IApplTerm, R, ? extends E> f) {
            return term -> term.matchOrThrow(Terms.<Optional<R>, E>checkedCases(appl -> Optional.of(f.apply(appl)),
                    Terms::empty, Terms::empty, Terms::empty, Terms::empty));
        }

        public static <T, R, E extends Throwable> ICheckedMatcher<R, E> appl0(String op,
                CheckedFunction1<? super IApplTerm, R, ? extends E> f) {
            return term -> {
                return term.matchOrThrow(Terms.<Optional<R>, E>checkedCases(appl -> {
                    if(!(op.equals(appl.getOp()) && appl.getArity() == 0)) {
                        return Optional.empty();
                    }
                    return Optional.of(f.apply(appl));
                }, Terms::empty, Terms::empty, Terms::empty, Terms::empty));
            };
        }

        public static <T, R, E extends Throwable> ICheckedMatcher<R, E> appl1(String op,
                ICheckedMatcher<? extends T, ? extends E> m,
                CheckedFunction2<? super IApplTerm, ? super T, R, ? extends E> f) {
            return term -> {
                return term.matchOrThrow(Terms.<Optional<R>, E>checkedCases(appl -> {
                    if(!(op.equals(appl.getOp()) && appl.getArity() == 1)) {
                        return Optional.empty();
                    }
                    Optional<? extends T> o1 = m.matchOrThrow(appl.getArgs().get(0));
                    if(!o1.isPresent()) {
                        return Optional.empty();
                    }
                    T t = o1.get();
                    return Optional.of(f.apply(appl, t));
                }, Terms::empty, Terms::empty, Terms::empty, Terms::empty));
            };
        }

        public static <T1, T2, R, E extends Throwable> ICheckedMatcher<R, E> appl2(String op,
                ICheckedMatcher<? extends T1, ? extends E> m1, ICheckedMatcher<? extends T2, ? extends E> m2,
                CheckedFunction3<? super IApplTerm, ? super T1, ? super T2, R, ? extends E> f) {
            return term -> {
                return term.matchOrThrow(Terms.<Optional<R>, E>checkedCases(appl -> {
                    if(!(op.equals(appl.getOp()) && appl.getArity() == 2)) {
                        return Optional.empty();
                    }
                    Optional<? extends T1> o1 = m1.matchOrThrow(appl.getArgs().get(0));
                    if(!o1.isPresent()) {
                        return Optional.empty();
                    }
                    T1 t1 = o1.get();
                    Optional<? extends T2> o2 = m2.matchOrThrow(appl.getArgs().get(1));
                    if(!o2.isPresent()) {
                        return Optional.empty();
                    }
                    T2 t2 = o2.get();
                    return Optional.of(f.apply(appl, t1, t2));
                }, Terms::empty, Terms::empty, Terms::empty, Terms::empty));
            };
        }

        public static <T1, T2, T3, R, E extends Throwable> ICheckedMatcher<R, E> appl3(String op,
                ICheckedMatcher<? extends T1, ? extends E> m1, ICheckedMatcher<? extends T2, ? extends E> m2,
                ICheckedMatcher<? extends T3, ? extends E> m3,
                CheckedFunction4<? super IApplTerm, ? super T1, ? super T2, ? super T3, R, ? extends E> f) {
            return term -> {
                return term.matchOrThrow(Terms.<Optional<R>, E>checkedCases(appl -> {
                    if(!(op.equals(appl.getOp()) && appl.getArity() == 3)) {
                        return Optional.empty();
                    }
                    Optional<? extends T1> o1 = m1.matchOrThrow(appl.getArgs().get(0));
                    if(!o1.isPresent()) {
                        return Optional.empty();
                    }
                    T1 t1 = o1.get();
                    Optional<? extends T2> o2 = m2.matchOrThrow(appl.getArgs().get(1));
                    if(!o2.isPresent()) {
                        return Optional.empty();
                    }
                    T2 t2 = o2.get();
                    Optional<? extends T3> o3 = m3.matchOrThrow(appl.getArgs().get(2));
                    if(!o3.isPresent()) {
                        return Optional.empty();
                    }
                    T3 t3 = o3.get();
                    return Optional.of(f.apply(appl, t1, t2, t3));
                }, Terms::empty, Terms::empty, Terms::empty, Terms::empty));
            };
        }

        // list

        public static <R, E extends Throwable> ICheckedMatcher<R, E>
                list(CheckedFunction1<? super IListTerm, R, ? extends E> f) {
            return term -> term.matchOrThrow(Terms.<Optional<R>, E>checkedCases(Terms::empty,
                    list -> Optional.of(f.apply(list)), Terms::empty, Terms::empty, Terms::empty));
        }

        public static <T, R, E extends Throwable> ICheckedMatcher<R, E> listElems(
                ICheckedMatcher<? extends T, ? extends E> m,
                CheckedFunction2<? super IListTerm, Iterable<T>, R, ? extends E> f) {
            return term -> {
                return term.matchOrThrow(Terms.<Optional<R>, E>checkedCases(Terms::empty, list -> {
                    List<T> ts = Lists.newArrayList();
                    for(ITerm t : list) {
                        Optional<? extends T> o = m.matchOrThrow(t);
                        if(!o.isPresent()) {
                            return Optional.empty();
                        }
                        ts.add(o.get());
                    }
                    return Optional.of(f.apply(list, ts));
                }, Terms::empty, Terms::empty, Terms::empty));
            };
        }

        public static <R, E extends Throwable> ICheckedMatcher<R, E>
                cons(CheckedFunction1<? super IConsTerm, R, ? extends E> f) {
            return term -> term.matchOrThrow(Terms.<Optional<R>, E>checkedCases(Terms::empty, list -> {
                return list.matchOrThrow(ListTerms.<Optional<R>, E>checkedCases(cons -> Optional.of(f.apply(cons)),
                        nil -> Optional.empty(), var -> Optional.empty()));
            }, Terms::empty, Terms::empty, Terms::empty));

        }

        public static <R, E extends Throwable> ICheckedMatcher<R, E>
                nil(CheckedFunction1<? super INilTerm, R, ? extends E> f) {
            return term -> term.matchOrThrow(Terms.<Optional<R>, E>checkedCases(Terms::empty, list -> {
                return list.matchOrThrow(ListTerms.<Optional<R>, E>checkedCases(cons -> Optional.empty(),
                        nil -> Optional.of(f.apply(nil)), var -> Optional.empty()));
            }, Terms::empty, Terms::empty, Terms::empty));

        }

        // integer

        public static <R, E extends Throwable> ICheckedMatcher<R, E>
                integer(CheckedFunction1<IIntTerm, R, ? extends E> f) {
            return term -> term.matchOrThrow(Terms.<Optional<R>, E>checkedCases(Terms::empty, Terms::empty,
                    Terms::empty, string -> Optional.of(f.apply(string)), Terms::empty));
        }

        // string

        public static <R, E extends Throwable> ICheckedMatcher<R, E>
                string(CheckedFunction1<IStringTerm, R, ? extends E> f) {
            return term -> term.matchOrThrow(Terms.<Optional<R>, E>checkedCases(Terms::empty, Terms::empty,
                    string -> Optional.of(f.apply(string)), Terms::empty, Terms::empty));
        }

        // var

        public static <R, E extends Throwable> ICheckedMatcher<R, E>
                var(CheckedFunction1<? super ITermVar, R, ? extends E> f) {
            return term -> term.matchOrThrow(Terms.<Optional<R>, E>checkedCases(Terms::empty, list -> {
                return list.matchOrThrow(ListTerms.<Optional<R>, E>checkedCases(cons -> Optional.empty(),
                        nil -> Optional.empty(), var -> Optional.of(f.apply(var))));
            }, Terms::empty, Terms::empty, var -> Optional.of(f.apply(var))));
        }

        // cases

        @SafeVarargs public static <T, E extends Throwable> ICheckedMatcher<T, E>
                cases(ICheckedMatcher<? extends T, ? extends E>... matchers) {
            return term -> {
                for(ICheckedMatcher<? extends T, ? extends E> matcher : matchers) {
                    Optional<? extends T> result = matcher.matchOrThrow(term);
                    if(result.isPresent()) {
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