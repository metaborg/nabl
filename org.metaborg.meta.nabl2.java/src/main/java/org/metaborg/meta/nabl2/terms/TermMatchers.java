package org.metaborg.meta.nabl2.terms;

import java.util.Optional;

import org.metaborg.meta.nabl2.functions.*;

public class TermMatchers {

    public static <R> PartialFunction1<ITerm,R> appl(String op, Function0<R> f) {
        return new PartialFunction1<ITerm,R>() {

            @Override public Optional<R> apply(ITerm term) {
                return term.match(Terms.<Optional<R>> cases()
                    // @formatter:off
                    .appl(appl -> {
                        if (appl.getOp().equals(op) && appl.getArity() == 0) {
                            return Optional.of(f.apply());
                        }
                        return Optional.empty();
                    })
                    .otherwise(() -> Optional.empty()));
                    // @formatter:on
            }

        };
    }

    public static <T, R> PartialFunction1<ITerm,R> appl(String op, PartialFunction1<ITerm,T> m, Function1<T,R> f) {
        return new PartialFunction1<ITerm,R>() {

            @Override public Optional<R> apply(ITerm term) {
                return term.match(Terms.<Optional<R>> cases()
                    // @formatter:off
                    .appl(appl -> {
                        if (appl.getOp().equals(op) && appl.getArity() == 1) {
                        Optional<T> t = m.apply(appl.getArgs().get(0));
                            if(t.isPresent()) {
                                return Optional.of(f.apply(t.get()));
                            }
                        }
                        return Optional.empty();
                    })
                    .otherwise(() -> Optional.empty()));
                    // @formatter:on
            }

        };
    }

    public static <T1, T2, R> PartialFunction1<ITerm,R> appl(String op, PartialFunction1<ITerm,T1> m1,
            PartialFunction1<ITerm,T2> m2, Function2<T1,T2,R> f) {
        return new PartialFunction1<ITerm,R>() {

            @Override public Optional<R> apply(ITerm term) {
                return term.match(Terms.<Optional<R>> cases()
                    // @formatter:off
                    .appl(appl -> {
                        if (appl.getOp().equals(op) && appl.getArity() == 2) {
                        Optional<T1> t1 = m1.apply(appl.getArgs().get(0));
                        Optional<T2> t2 = m2.apply(appl.getArgs().get(1));
                            if(t1.isPresent() && t2.isPresent()) {
                                return Optional.of(f.apply(t1.get(), t2.get()));
                            }
                        }
                        return Optional.empty();
                    })
                    .otherwise(() -> Optional.empty()));
                    // @formatter:on
            }

        };
    }

    public static <T1, T2, T3, R> PartialFunction1<ITerm,R> appl(String op, PartialFunction1<ITerm,T1> m1,
            PartialFunction1<ITerm,T2> m2, PartialFunction1<ITerm,T3> m3, Function3<T1,T2,T3,R> f) {
        return new PartialFunction1<ITerm,R>() {

            @Override public Optional<R> apply(ITerm term) {
                return term.match(Terms.<Optional<R>> cases()
                    // @formatter:off
                    .appl(appl -> {
                        if (appl.getOp().equals(op) && appl.getArity() == 3) {
                        Optional<T1> t1 = m1.apply(appl.getArgs().get(0));
                        Optional<T2> t2 = m2.apply(appl.getArgs().get(1));
                        Optional<T3> t3 = m3.apply(appl.getArgs().get(2));
                            if(t1.isPresent() && t2.isPresent()) {
                                return Optional.of(f.apply(t1.get(), t2.get(), t3.get()));
                            }
                        }
                        return Optional.empty();
                    })
                    .otherwise(() -> Optional.empty()));
                    // @formatter:on
            }

        };
    }

    public static PartialFunction1<ITerm,String> string() {
        return new PartialFunction1<ITerm,String>() {

            @Override public Optional<String> apply(ITerm term) {
                return term.match(Terms.<Optional<String>> cases()
                    // @formatter:off
                    .string(string -> Optional.of(string.getValue()))
                    .otherwise(() -> Optional.empty()));
                    // @formatter:off
            }

        };
    }

    public static PartialFunction1<ITerm,Integer> integer() {
        return new PartialFunction1<ITerm,Integer>() {

            @Override public Optional<Integer> apply(ITerm term) {
                return term.match(Terms.<Optional<Integer>> cases()
                    // @formatter:off
                    .integer(integer -> Optional.of(integer.getValue()))
                    .otherwise(() -> Optional.empty()));
                    // @formatter:off
            }

        };
    }

    public static PartialFunction1<ITerm,ITerm> any() {
        return new PartialFunction1<ITerm,ITerm>() {

            @Override public Optional<ITerm> apply(ITerm term) {
                return Optional.of(term);
            }

        };
    }


    
}