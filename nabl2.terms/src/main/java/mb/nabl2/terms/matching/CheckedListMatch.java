package mb.nabl2.terms.matching;

import static mb.nabl2.terms.build.ListBuild.LB;
import static mb.nabl2.terms.matching.CheckedTermMatch.CM;

import java.util.List;
import java.util.Optional;

import org.metaborg.util.functions.CheckedFunction1;
import org.metaborg.util.functions.CheckedFunction2;
import org.metaborg.util.functions.CheckedFunction3;
import org.metaborg.util.optionals.Optionals;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import mb.nabl2.terms.IConsList;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.IListVar;
import mb.nabl2.terms.INilList;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.matching.CheckedTermMatch.ICheckedMatcher;
import mb.nabl2.terms.unification.Unifiers;
import mb.nabl2.terms.unification.u.IUnifier;

public class CheckedListMatch {

    public static final CLM CLM = new CLM();

    public static class CLM {

        // list

        public <E extends Throwable> ICheckedMatcher<IListTerm, E> list() {
            return list(l -> l);
        }

        public <R, E extends Throwable> ICheckedMatcher<R, E>
                list(CheckedFunction1<? super IListTerm, R, ? extends E> f) {
            return CM.<IListTerm, E>cases(cons(), nil(), var()).map(l -> f.apply(l));
        }

        public <E extends Throwable> ICheckedMatcher<? extends List<? extends ITerm>, ? extends E> listElems() {
            return listElems(CM.term());
        }

        public <T, E extends Throwable> ICheckedMatcher<List<T>, E> listElems(ICheckedMatcher<T, ? extends E> m) {
            return listElems(m, (t, ts) -> ts);
        }

        public <T, R, E extends Throwable> ICheckedMatcher<R, E> listElems(ICheckedMatcher<T, ? extends E> m,
                CheckedFunction2<? super IListTerm, ? super ImmutableList<T>, R, ? extends E> f) {
            return (term, unifier) -> {
                return this.<E>list().flatMap(list -> {
                    List<Optional<T>> os = Lists.newArrayList();
                    for(ITerm t : ListTerms.iterable(list)) {
                        os.add(m.matchOrThrow(t, unifier));
                    }
                    Optional<List<T>> o = Optionals.sequence(os);
                    return o.isPresent() ? Optional.of(f.apply(list, ImmutableList.copyOf(o.get()))) : Optional.empty();
                }).matchOrThrow(term, unifier);
            };
        }

        public <E extends Throwable> ICheckedMatcher<IConsList, E> cons() {
            return cons(t -> t);
        }

        public <R, E extends Throwable> ICheckedMatcher<R, E>
                cons(CheckedFunction1<? super IConsList, R, ? extends E> f) {
            return cons(CM.term(), (t, u) -> Optional.of(t), (cons, hd, tl) -> f.apply(cons));
        }

        public <THd, TTl, R, E extends Throwable> ICheckedMatcher<R, E> cons(
                ICheckedMatcher<? extends THd, ? extends E> mhd, ICheckedListMatcher<? extends TTl, ? extends E> mtl,
                CheckedFunction3<? super IConsList, ? super THd, ? super TTl, R, ? extends E> f) {
            return (term, unifier) -> {
                return CM.<ITerm, IListTerm, Optional<R>, E>appl2(ListTerms.CONS_OP, CM.term(), list(),
                        (cons, hd, tl) -> {
                            final Optional<? extends THd> ohd = mhd.matchOrThrow(hd, unifier);
                            final Optional<? extends TTl> otl = mtl.matchOrThrow(tl, unifier);
                            final Optional<IConsList> o =
                                    Optionals.lift(ohd, otl, (thd, ttl) -> LB.newCons(hd, tl, cons.getAttachments()));
                            return o.isPresent() ? Optional.of(f.apply(o.get(), ohd.get(), otl.get()))
                                    : Optional.empty();
                        }).matchOrThrow(term, unifier).flatMap(o -> o);
            };
        }

        public <E extends Throwable> ICheckedMatcher<INilList, E> nil() {
            return nil(t -> t);
        }

        public <R, E extends Throwable> ICheckedMatcher<R, E>
                nil(CheckedFunction1<? super INilList, R, ? extends E> f) {
            return CM.appl0(ListTerms.NIL_OP, t -> f.apply(LB.newNil(t.getAttachments())));
        }

        public <E extends Throwable> ICheckedMatcher<IListVar, E> var() {
            return var(t -> t);
        }

        public <R, E extends Throwable> ICheckedMatcher<R, E>
                var(CheckedFunction1<? super IListVar, R, ? extends E> f) {
            return CM.var(v -> f.apply(LB.newVar(v.getResource(), v.getName(), v.getAttachments())));
        }

    }

    @FunctionalInterface
    public interface ICheckedListMatcher<T, E extends Throwable> {

        Optional<T> matchOrThrow(IListTerm term, IUnifier unifier) throws E;

        default Optional<T> matchOrThrow(IListTerm term) throws E {
            return matchOrThrow(term, Unifiers.Immutable.of());
        }

        default <R> ICheckedListMatcher<R, E> map(CheckedFunction1<T, R, E> fun) {
            return (term, unifier) -> {
                final Optional<T> o = this.matchOrThrow(term, unifier);
                return o.isPresent() ? Optional.of(fun.apply(o.get())) : Optional.empty();
            };
        }

        default <R> ICheckedListMatcher<R, E> flatMap(CheckedFunction1<T, Optional<R>, E> fun) {
            return (term, unifier) -> {
                final Optional<T> o = this.matchOrThrow(term, unifier);
                return o.isPresent() ? fun.apply(o.get()) : Optional.empty();
            };
        }

    }

}