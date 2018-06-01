package mb.nabl2.terms.matching;

import static mb.nabl2.terms.Terms.TUPLE_OP;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.Function2;
import org.metaborg.util.functions.Function3;
import org.metaborg.util.functions.Function4;
import org.metaborg.util.functions.Function5;
import org.metaborg.util.functions.Function6;
import org.metaborg.util.optionals.Optionals;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.IBlobTerm;
import mb.nabl2.terms.IConsTerm;
import mb.nabl2.terms.IIntTerm;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.INilTerm;
import mb.nabl2.terms.IStringTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;

public class TermMatch {

    public static final M M = new M();

    public static class M {

        // term

        public IMatcher<ITerm> term() {
            return (term, unifier) -> Optional.of(term);
        }

        public IMatcher<ITerm> instantiatedTerm() {
            return (term, unifier) -> Optional.of(unifier.findRecursive(term));
        }

        public <R> IMatcher<R> term(Function1<? super ITerm, R> f) {
            return (term, unifier) -> Optional.of(f.apply(term));
        }

        public <T, R> IMatcher<R> term(IMatcher<? extends T> m, Function2<? super ITerm, ? super T, R> f) {
            return (term, unifier) -> m.match(term, unifier).map(t -> f.apply(term, t));
        }

        // appl

        public IMatcher<IApplTerm> appl() {
            return (term, unifier) -> unifier.findTerm(term).match(Terms.<Optional<IApplTerm>>cases(Optional::of,
                    this::empty, this::empty, this::empty, this::empty, this::empty));
        }

        public <R> IMatcher<R> appl(String op, Function1<? super IApplTerm, R> f) {
            return flatten(appl(appl -> appl.getOp().equals(op) ? Optional.of(f.apply(appl)) : Optional.empty()));
        }

        public <R> IMatcher<R> appl(Function1<? super IApplTerm, R> f) {
            return (term, unifier) -> unifier.findTerm(term)
                    .match(Terms.<Optional<R>>cases(appl -> Optional.of(f.apply(appl)), this::empty, this::empty,
                            this::empty, this::empty, this::empty));
        }

        public IMatcher<IApplTerm> appl0(String op) {
            return appl0(op, (appl) -> appl);
        }

        public <R> IMatcher<R> appl0(String op, Function1<? super IApplTerm, R> f) {
            return (term, unifier) -> {
                return unifier.findTerm(term).match(Terms.<Optional<R>>cases(appl -> {
                    if(!(op.equals(appl.getOp()) && appl.getArity() == 0)) {
                        return Optional.empty();
                    }
                    return Optional.of(f.apply(appl));
                }, this::empty, this::empty, this::empty, this::empty, this::empty));
            };
        }

        public <T> IMatcher<IApplTerm> appl1(String op, IMatcher<? extends T> m) {
            return appl1(op, m, (appl, t) -> appl);
        }

        public <T, R> IMatcher<R> appl1(String op, IMatcher<? extends T> m,
                Function2<? super IApplTerm, ? super T, R> f) {
            return (term, unifier) -> {
                return unifier.findTerm(term).match(Terms.<Optional<R>>cases(appl -> {
                    if(!(op.equals(appl.getOp()) && appl.getArity() == 1)) {
                        return Optional.empty();
                    }
                    return m.match(appl.getArgs().get(0), unifier).map(t -> f.apply(appl, t));
                }, this::empty, this::empty, this::empty, this::empty, this::empty));
            };
        }

        public <T1, T2> IMatcher<IApplTerm> appl2(String op, IMatcher<? extends T1> m1, IMatcher<? extends T2> m2) {
            return appl2(op, m1, m2, (appl, t1, t2) -> appl);
        }

        public <T1, T2, R> IMatcher<R> appl2(String op, IMatcher<? extends T1> m1, IMatcher<? extends T2> m2,
                Function3<? super IApplTerm, ? super T1, ? super T2, R> f) {
            return (term, unifier) -> {
                return unifier.findTerm(term).match(Terms.<Optional<R>>cases(appl -> {
                    if(!(op.equals(appl.getOp()) && appl.getArity() == 2)) {
                        return Optional.empty();
                    }
                    Optional<? extends T1> o1 = m1.match(appl.getArgs().get(0), unifier);
                    Optional<? extends T2> o2 = m2.match(appl.getArgs().get(1), unifier);
                    return Optionals.lift(o1, o2, (t1, t2) -> f.apply(appl, t1, t2));
                }, this::empty, this::empty, this::empty, this::empty, this::empty));
            };
        }

        public <T1, T2, T3> IMatcher<IApplTerm> appl3(String op, IMatcher<? extends T1> m1, IMatcher<? extends T2> m2,
                IMatcher<T3> m3) {
            return appl3(op, m1, m2, m3, (appl, t1, t2, t3) -> appl);
        }

        public <T1, T2, T3, R> IMatcher<R> appl3(String op, IMatcher<? extends T1> m1, IMatcher<? extends T2> m2,
                IMatcher<? extends T3> m3, Function4<? super IApplTerm, ? super T1, ? super T2, ? super T3, R> f) {
            return (term, unifier) -> {
                return unifier.findTerm(term).match(Terms.<Optional<R>>cases(appl -> {
                    if(!(op.equals(appl.getOp()) && appl.getArity() == 3)) {
                        return Optional.empty();
                    }
                    Optional<? extends T1> o1 = m1.match(appl.getArgs().get(0), unifier);
                    Optional<? extends T2> o2 = m2.match(appl.getArgs().get(1), unifier);
                    Optional<? extends T3> o3 = m3.match(appl.getArgs().get(2), unifier);
                    return Optionals.lift(o1, o2, o3, (t1, t2, t3) -> f.apply(appl, t1, t2, t3));
                }, this::empty, this::empty, this::empty, this::empty, this::empty));
            };
        }

        public <T1, T2, T3, T4> IMatcher<IApplTerm> appl4(String op, IMatcher<? extends T1> m1,
                IMatcher<? extends T2> m2, IMatcher<T3> m3, IMatcher<T4> m4) {
            return appl4(op, m1, m2, m3, m4, (appl, t1, t2, t3, t4) -> appl);
        }

        public <T1, T2, T3, T4, R> IMatcher<R> appl4(String op, IMatcher<? extends T1> m1, IMatcher<? extends T2> m2,
                IMatcher<? extends T3> m3, IMatcher<? extends T4> m4,
                Function5<? super IApplTerm, ? super T1, ? super T2, ? super T3, ? super T4, R> f) {
            return (term, unifier) -> {
                return unifier.findTerm(term).match(Terms.<Optional<R>>cases(appl -> {
                    if(!(op.equals(appl.getOp()) && appl.getArity() == 4)) {
                        return Optional.empty();
                    }
                    Optional<? extends T1> o1 = m1.match(appl.getArgs().get(0), unifier);
                    Optional<? extends T2> o2 = m2.match(appl.getArgs().get(1), unifier);
                    Optional<? extends T3> o3 = m3.match(appl.getArgs().get(2), unifier);
                    Optional<? extends T4> o4 = m4.match(appl.getArgs().get(3), unifier);
                    return Optionals.lift(o1, o2, o3, o4, (t1, t2, t3, t4) -> f.apply(appl, t1, t2, t3, t4));
                }, this::empty, this::empty, this::empty, this::empty, this::empty));
            };
        }

        public <T1, T2, T3, T4, T5> IMatcher<IApplTerm> appl5(String op, IMatcher<? extends T1> m1,
                IMatcher<? extends T2> m2, IMatcher<T3> m3, IMatcher<T4> m4, IMatcher<T5> m5) {
            return appl5(op, m1, m2, m3, m4, m5, (appl, t1, t2, t3, t4, t5) -> appl);
        }

        public <T1, T2, T3, T4, T5, R> IMatcher<R> appl5(String op, IMatcher<? extends T1> m1,
                IMatcher<? extends T2> m2, IMatcher<? extends T3> m3, IMatcher<? extends T4> m4,
                IMatcher<? extends T5> m5,
                Function6<? super IApplTerm, ? super T1, ? super T2, ? super T3, ? super T4, ? super T5, R> f) {
            return (term, unifier) -> {
                return unifier.findTerm(term).match(Terms.<Optional<R>>cases(appl -> {
                    if(!(op.equals(appl.getOp()) && appl.getArity() == 5)) {
                        return Optional.empty();
                    }
                    Optional<? extends T1> o1 = m1.match(appl.getArgs().get(0), unifier);
                    Optional<? extends T2> o2 = m2.match(appl.getArgs().get(1), unifier);
                    Optional<? extends T3> o3 = m3.match(appl.getArgs().get(2), unifier);
                    Optional<? extends T4> o4 = m4.match(appl.getArgs().get(3), unifier);
                    Optional<? extends T5> o5 = m5.match(appl.getArgs().get(4), unifier);
                    return Optionals.lift(o1, o2, o3, o4, o5,
                            (t1, t2, t3, t4, t5) -> f.apply(appl, t1, t2, t3, t4, t5));
                }, this::empty, this::empty, this::empty, this::empty, this::empty));
            };
        }

        // tuple

        public <R> IMatcher<R> tuple(Function1<? super IApplTerm, R> f) {
            return M.appl(TUPLE_OP, f);
        }

        public <R> IMatcher<R> tuple0(Function1<? super IApplTerm, R> f) {
            return M.appl0(TUPLE_OP, f);
        }

        public <T> IMatcher<IApplTerm> tuple1(IMatcher<? extends T> m) {
            return M.appl1(TUPLE_OP, m);
        }

        public <T, R> IMatcher<R> tuple1(IMatcher<? extends T> m, Function2<? super IApplTerm, ? super T, R> f) {
            return M.appl1(TUPLE_OP, m, f);
        }

        public <T1, T2> IMatcher<IApplTerm> tuple2(IMatcher<? extends T1> m1, IMatcher<? extends T2> m2) {
            return M.appl2(TUPLE_OP, m1, m2);
        }

        public <T1, T2, R> IMatcher<R> tuple2(IMatcher<? extends T1> m1, IMatcher<? extends T2> m2,
                Function3<? super IApplTerm, ? super T1, ? super T2, R> f) {
            return M.appl2(TUPLE_OP, m1, m2, f);
        }

        public <T1, T2, T3> IMatcher<IApplTerm> tuple3(IMatcher<? extends T1> m1, IMatcher<? extends T2> m2,
                IMatcher<? extends T3> m3) {
            return M.appl3(TUPLE_OP, m1, m2, m3);
        }

        public <T1, T2, T3, R> IMatcher<R> tuple3(IMatcher<? extends T1> m1, IMatcher<? extends T2> m2,
                IMatcher<? extends T3> m3, Function4<? super IApplTerm, ? super T1, ? super T2, ? super T3, R> f) {
            return M.appl3(TUPLE_OP, m1, m2, m3, f);
        }

        public <T1, T2, T3, T4> IMatcher<IApplTerm> tuple4(IMatcher<? extends T1> m1, IMatcher<? extends T2> m2,
                IMatcher<? extends T3> m3, IMatcher<? extends T4> m4) {
            return M.appl4(TUPLE_OP, m1, m2, m3, m4);
        }

        public <T1, T2, T3, T4, R> IMatcher<R> tuple4(IMatcher<? extends T1> m1, IMatcher<? extends T2> m2,
                IMatcher<? extends T3> m3, IMatcher<? extends T4> m4,
                Function5<? super IApplTerm, ? super T1, ? super T2, ? super T3, ? super T4, R> f) {
            return M.appl4(TUPLE_OP, m1, m2, m3, m4, f);
        }

        public <T1, T2, T3, T4, T5> IMatcher<IApplTerm> tuple5(IMatcher<? extends T1> m1, IMatcher<? extends T2> m2,
                IMatcher<? extends T3> m3, IMatcher<? extends T4> m4, IMatcher<? extends T5> m5) {
            return M.appl5(TUPLE_OP, m1, m2, m3, m4, m5);
        }

        public <T1, T2, T3, T4, T5, R> IMatcher<R> tuple5(IMatcher<? extends T1> m1, IMatcher<? extends T2> m2,
                IMatcher<? extends T3> m3, IMatcher<? extends T4> m4, IMatcher<? extends T5> m5,
                Function6<? super IApplTerm, ? super T1, ? super T2, ? super T3, ? super T4, ? super T5, R> f) {
            return M.appl5(TUPLE_OP, m1, m2, m3, m4, m5, f);
        }

        // list

        public IMatcher<IListTerm> list() {
            return list((l) -> l);
        }

        public <R> IMatcher<R> list(Function1<? super IListTerm, R> f) {
            return (term, unifier) -> unifier.findTerm(term).match(Terms.<Optional<R>>cases(this::empty,
                    list -> Optional.of(f.apply(list)), this::empty, this::empty, this::empty, this::empty));
        }

        public IMatcher<? extends List<? extends ITerm>> listElems() {
            return listElems(M.term());
        }

        public <T> IMatcher<ImmutableList<T>> listElems(IMatcher<T> m) {
            return listElems(m, (t, ts) -> ts);
        }

        public <T, R> IMatcher<R> listElems(IMatcher<T> m,
                Function2<? super IListTerm, ? super ImmutableList<T>, R> f) {
            return (term, unifier) -> {
                return unifier.findTerm(term).match(Terms.<Optional<R>>cases(this::empty, list -> {
                    List<Optional<T>> os = Lists.newArrayList();
                    for(ITerm t : ListTerms.iterable(list)) {
                        os.add(m.match(t, unifier));
                    }
                    return Optionals.sequence(os).map(ts -> (R) f.apply(list, ImmutableList.copyOf(ts)));
                }, this::empty, this::empty, this::empty, this::empty));
            };
        }

        public <R> IMatcher<R> cons(Function1<? super IConsTerm, R> f) {
            return (term, unifier) -> unifier.findTerm(term).match(Terms.<Optional<R>>cases(this::empty, list -> {
                return list.match(ListTerms.<Optional<R>>cases(cons -> Optional.of(f.apply(cons)),
                        nil -> Optional.empty(), var -> Optional.empty()));
            }, this::empty, this::empty, this::empty, this::empty));

        }

        public <THd, TTl, R> IMatcher<R> cons(IMatcher<? extends THd> mhd, IMatcher<? extends TTl> mtl,
                Function3<? super IConsTerm, ? super THd, ? super TTl, R> f) {
            return (term, unifier) -> unifier.findTerm(term).match(Terms.<Optional<R>>cases(this::empty, list -> {
                return list.match(ListTerms.<Optional<R>>cases(cons -> {
                    Optional<? extends THd> ohd = mhd.match(cons.getHead(), unifier);
                    Optional<? extends TTl> otl = mtl.match(cons.getTail(), unifier);
                    return Optionals.lift(ohd, otl, (thd, ttl) -> f.apply(cons, thd, ttl));
                }, this::empty, this::empty));
            }, this::empty, this::empty, this::empty, this::empty));

        }

        public <R> IMatcher<R> nil(Function1<? super INilTerm, R> f) {
            return (term, unifier) -> unifier.findTerm(term).match(Terms.<Optional<R>>cases(this::empty, list -> {
                return list.match(ListTerms.<Optional<R>>cases(cons -> Optional.empty(),
                        nil -> Optional.of(f.apply(nil)), var -> Optional.empty()));
            }, this::empty, this::empty, this::empty, this::empty));

        }

        // string

        public IMatcher<IStringTerm> string() {
            return string(s -> s);
        }

        public <R> IMatcher<R> string(Function1<? super IStringTerm, R> f) {
            return (term, unifier) -> unifier.findTerm(term).match(Terms.<Optional<R>>cases(this::empty, this::empty,
                    string -> Optional.of(f.apply(string)), this::empty, this::empty, this::empty));
        }

        public IMatcher<String> stringValue() {
            return string(s -> s.getValue());
        }

        // integer

        public IMatcher<IIntTerm> integer() {
            return integer(i -> i);
        }

        public <R> IMatcher<R> integer(Function1<? super IIntTerm, R> f) {
            return (term, unifier) -> unifier.findTerm(term).match(Terms.<Optional<R>>cases(this::empty, this::empty,
                    this::empty, integer -> Optional.of(f.apply(integer)), this::empty, this::empty));
        }

        public IMatcher<Integer> integerValue() {
            return integer(i -> i.getValue());
        }

        // blob

        public IMatcher<IBlobTerm> blob() {
            return blob(i -> i);
        }

        public <R> IMatcher<R> blob(Function1<? super IBlobTerm, R> f) {
            return (term, unifier) -> unifier.findTerm(term).match(Terms.<Optional<R>>cases(this::empty, this::empty,
                    this::empty, this::empty, blob -> Optional.of(f.apply(blob)), this::empty));
        }

        @SuppressWarnings("unchecked") public <T> IMatcher<T> blobValue(Class<T> blobClass) {
            return (term, unifier) -> blob().match(term, unifier).flatMap(b -> {
                if(blobClass.isInstance(b.getValue())) {
                    return Optional.of((T) b.getValue());
                } else {
                    return Optional.empty();
                }
            });
        }

        // var

        public IMatcher<ITermVar> var() {
            return var(v -> v);
        }

        public <R> IMatcher<R> var(Function1<? super ITermVar, R> f) {
            return (term, unifier) -> unifier.findTerm(term).match(Terms.<Optional<R>>cases(this::empty, this::empty,
                    this::empty, this::empty, this::empty, var -> Optional.of(f.apply(var))));
        }

        // optionals

        public <R> IMatcher<R> flatten(IMatcher<Optional<R>> m) {
            return (term, unifier) -> m.match(term, unifier).flatMap(o -> o);
        }

        // cases

        @SafeVarargs public final <T> IMatcher<T> cases(IMatcher<? extends T>... matchers) {
            return (term, unifier) -> {
                for(IMatcher<? extends T> matcher : matchers) {
                    Optional<? extends T> result = matcher.match(term, unifier);
                    if(result.isPresent()) {
                        return Optional.of(result.get());
                    }
                }
                return Optional.empty();
            };
        }

        public <T> IMatcher<T> casesFix(Function1<IMatcher<T>, Iterable<IMatcher<? extends T>>> f) {
            return (term, unifier) -> {
                for(IMatcher<? extends T> matcher : f.apply(casesFix(f))) {
                    Optional<? extends T> result = matcher.match(term, unifier);
                    if(result.isPresent()) {
                        return Optional.of(result.get());
                    }
                }
                return Optional.empty();
            };
        }

        // metadata

        public <R> IMatcher<R> req(IMatcher<R> matcher) {
            return req("Cannot match", matcher);
        }

        public <R> IMatcher<R> req(String msg, IMatcher<R> matcher) {
            return (term, unifier) -> {
                return matcher.match(term, unifier).map(Optional::of)
                        .orElseThrow(() -> new IllegalArgumentException(msg + ": " + term));
            };
        }

        @SuppressWarnings("unchecked") public <R extends ITerm> IMatcher<R> preserveAttachments(IMatcher<R> matcher) {
            return (term, unifier) -> matcher.match(term, unifier)
                    .map(r -> (R) r.withAttachments(term.getAttachments()));
        }

        // util

        private <T> Optional<T> empty(@SuppressWarnings("unused") ITerm term) {
            return Optional.empty();
        }
    }

    @FunctionalInterface
    public interface IMatcher<T> {

        Optional<T> match(ITerm term, IUnifier unifier);

        @Deprecated default Optional<T> match(ITerm term) {
            return match(term, PersistentUnifier.Immutable.of());
        }

        default <R> IMatcher<R> map(Function<T, R> fun) {
            return (term, unifier) -> this.match(term, unifier).<R>map(fun);
        }

        default <R> IMatcher<R> flatMap(Function<T, Optional<R>> fun) {
            return (term, unifier) -> this.match(term, unifier).<R>flatMap(fun);
        }

        static <T> IMatcher<T> flatten(IMatcher<Optional<T>> m) {
            return m.flatMap(o -> o)::match;
        }

    }

}