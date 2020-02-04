package mb.nabl2.terms.matching;

import java.util.Optional;

import org.metaborg.util.functions.CheckedFunction1;
import org.metaborg.util.functions.CheckedFunction2;
import org.metaborg.util.functions.CheckedFunction3;
import org.metaborg.util.functions.CheckedFunction4;

import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.IIntTerm;
import mb.nabl2.terms.IStringTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.unification.Unifiers;
import mb.nabl2.terms.unification.u.IUnifier;

public class CheckedTermMatch {

    public static final CM CM = new CM();

    public static class CM {

        // term

        public <E extends Throwable> ICheckedMatcher<ITerm, E> term() {
            return (term, unifier) -> Optional.of(term);
        }

        public <R, E extends Throwable> ICheckedMatcher<R, E> term(CheckedFunction1<? super ITerm, R, ? extends E> f) {
            return (term, unifier) -> Optional.of(f.apply(term));
        }

        public <R, E extends Throwable> ICheckedMatcher<R, E> term(ITerm.CheckedCases<Optional<R>, E> cases) {
            return (term, unifier) -> unifier.findTerm(term).matchOrThrow(cases);
        }

        // appl

        public <R, E extends Throwable> ICheckedMatcher<R, E>
                appl(CheckedFunction1<? super IApplTerm, R, ? extends E> f) {
            return (term, unifier) -> unifier.findTerm(term).matchOrThrow(Terms.<Optional<R>, E>checkedCases(
                    appl -> Optional.of(f.apply(appl)), this::empty, this::empty, this::empty, this::empty));
        }

        public <R, E extends Throwable> ICheckedMatcher<R, E> appl0(String op,
                CheckedFunction1<? super IApplTerm, R, ? extends E> f) {
            return (term, unifier) -> {
                return unifier.findTerm(term).matchOrThrow(Terms.<Optional<R>, E>checkedCases(appl -> {
                    if(!(appl.getArity() == 0 && op.equals(appl.getOp()))) {
                        return Optional.empty();
                    }
                    return Optional.of(f.apply(appl));
                }, this::empty, this::empty, this::empty, this::empty));
            };
        }

        public <T, R, E extends Throwable> ICheckedMatcher<R, E> appl1(String op,
                ICheckedMatcher<? extends T, ? extends E> m,
                CheckedFunction2<? super IApplTerm, ? super T, R, ? extends E> f) {
            return (term, unifier) -> {
                return unifier.findTerm(term).matchOrThrow(Terms.<Optional<R>, E>checkedCases(appl -> {
                    if(!(appl.getArity() == 1 && op.equals(appl.getOp()))) {
                        return Optional.empty();
                    }
                    Optional<? extends T> o1 = m.matchOrThrow(appl.getArgs().get(0), unifier);
                    if(!o1.isPresent()) {
                        return Optional.empty();
                    }
                    T t = o1.get();
                    return Optional.of(f.apply(appl, t));
                }, this::empty, this::empty, this::empty, this::empty));
            };
        }

        public <T1, T2, R, E extends Throwable> ICheckedMatcher<R, E> appl2(String op,
                ICheckedMatcher<? extends T1, ? extends E> m1, ICheckedMatcher<? extends T2, ? extends E> m2,
                CheckedFunction3<? super IApplTerm, ? super T1, ? super T2, R, ? extends E> f) {
            return (term, unifier) -> {
                return unifier.findTerm(term).matchOrThrow(Terms.<Optional<R>, E>checkedCases(appl -> {
                    if(!(appl.getArity() == 2 && op.equals(appl.getOp()))) {
                        return Optional.empty();
                    }
                    Optional<? extends T1> o1 = m1.matchOrThrow(appl.getArgs().get(0), unifier);
                    if(!o1.isPresent()) {
                        return Optional.empty();
                    }
                    T1 t1 = o1.get();
                    Optional<? extends T2> o2 = m2.matchOrThrow(appl.getArgs().get(1), unifier);
                    if(!o2.isPresent()) {
                        return Optional.empty();
                    }
                    T2 t2 = o2.get();
                    return Optional.of(f.apply(appl, t1, t2));
                }, this::empty, this::empty, this::empty, this::empty));
            };
        }

        public <T1, T2, T3, R, E extends Throwable> ICheckedMatcher<R, E> appl3(String op,
                ICheckedMatcher<? extends T1, ? extends E> m1, ICheckedMatcher<? extends T2, ? extends E> m2,
                ICheckedMatcher<? extends T3, ? extends E> m3,
                CheckedFunction4<? super IApplTerm, ? super T1, ? super T2, ? super T3, R, ? extends E> f) {
            return (term, unifier) -> {
                return unifier.findTerm(term).matchOrThrow(Terms.<Optional<R>, E>checkedCases(appl -> {
                    if(!(appl.getArity() == 3 && op.equals(appl.getOp()))) {
                        return Optional.empty();
                    }
                    Optional<? extends T1> o1 = m1.matchOrThrow(appl.getArgs().get(0), unifier);
                    if(!o1.isPresent()) {
                        return Optional.empty();
                    }
                    T1 t1 = o1.get();
                    Optional<? extends T2> o2 = m2.matchOrThrow(appl.getArgs().get(1), unifier);
                    if(!o2.isPresent()) {
                        return Optional.empty();
                    }
                    T2 t2 = o2.get();
                    Optional<? extends T3> o3 = m3.matchOrThrow(appl.getArgs().get(2), unifier);
                    if(!o3.isPresent()) {
                        return Optional.empty();
                    }
                    T3 t3 = o3.get();
                    return Optional.of(f.apply(appl, t1, t2, t3));
                }, this::empty, this::empty, this::empty, this::empty));
            };
        }

        // integer

        public <R, E extends Throwable> ICheckedMatcher<R, E> integer(CheckedFunction1<IIntTerm, R, ? extends E> f) {
            return (term, unifier) -> unifier.findTerm(term).matchOrThrow(Terms.<Optional<R>, E>checkedCases(
                    this::empty, this::empty, string -> Optional.of(f.apply(string)), this::empty, this::empty));
        }

        // string

        public <R, E extends Throwable> ICheckedMatcher<R, E> string(CheckedFunction1<IStringTerm, R, ? extends E> f) {
            return (term, unifier) -> unifier.findTerm(term).matchOrThrow(Terms.<Optional<R>, E>checkedCases(
                    this::empty, string -> Optional.of(f.apply(string)), this::empty, this::empty, this::empty));
        }

        // var

        public <R, E extends Throwable> ICheckedMatcher<R, E>
                var(CheckedFunction1<? super ITermVar, R, ? extends E> f) {
            return (term, unifier) -> unifier.findTerm(term).matchOrThrow(Terms.<Optional<R>, E>checkedCases(
                    this::empty, this::empty, this::empty, this::empty, var -> Optional.of(f.apply(var))));
        }

        // cases

        @SafeVarargs public final <T, E extends Throwable> ICheckedMatcher<T, E>
                cases(ICheckedMatcher<? extends T, ? extends E>... matchers) {
            return (term, unifier) -> {
                for(ICheckedMatcher<? extends T, ? extends E> matcher : matchers) {
                    Optional<? extends T> result = matcher.matchOrThrow(term, unifier);
                    if(result.isPresent()) {
                        return Optional.of(result.get());
                    }
                }
                return Optional.empty();
            };
        }

        // util

        private <T> Optional<T> empty(@SuppressWarnings("unused") ITerm term) {
            return Optional.empty();
        }

    }

    @FunctionalInterface
    public interface ICheckedMatcher<T, E extends Throwable> {

        Optional<T> matchOrThrow(ITerm term, IUnifier unifier) throws E;

        default Optional<T> matchOrThrow(ITerm term) throws E {
            return matchOrThrow(term, Unifiers.Immutable.of());
        }

        default <R> ICheckedMatcher<R, E> map(CheckedFunction1<T, R, E> fun) {
            return (term, unifier) -> {
                final Optional<T> o = this.matchOrThrow(term, unifier);
                return o.isPresent() ? Optional.of(fun.apply(o.get())) : Optional.empty();
            };
        }

        default <R> ICheckedMatcher<R, E> flatMap(CheckedFunction1<T, Optional<R>, E> fun) {
            return (term, unifier) -> {
                final Optional<T> o = this.matchOrThrow(term, unifier);
                return o.isPresent() ? fun.apply(o.get()) : Optional.empty();
            };
        }

    }

}