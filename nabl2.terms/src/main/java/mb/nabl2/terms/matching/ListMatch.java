package mb.nabl2.terms.matching;

import static mb.nabl2.terms.build.ListBuild.LB;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.Function2;
import org.metaborg.util.functions.Function3;
import org.metaborg.util.optionals.Optionals;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.IConsList;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.IListVar;
import mb.nabl2.terms.INilList;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.unification.Unifiers;
import mb.nabl2.terms.unification.u.IUnifier;

public class ListMatch {

    public static final LM LM = new LM();

    public static class LM {

        // list

        public IMatcher<IListTerm> list() {
            return list(l -> l);
        }

        public <R> IMatcher<R> list(Function1<? super IListTerm, R> f) {
            return M.cases(cons(), nil(), var()).map(l -> f.apply(l));
        }

        public IMatcher<? extends List<? extends ITerm>> listElems() {
            return listElems(M.term());
        }

        public <T> IMatcher<List<T>> listElems(IMatcher<T> m) {
            return listElems(m, (t, ts) -> ts);
        }

        public <T, R> IMatcher<R> listElems(IMatcher<T> m,
                Function2<? super IListTerm, ? super ImmutableList<T>, R> f) {
            //            return (term, unifier) -> {
            //                return unifier.findTerm(term).match(Terms.<Optional<R>>cases(this::empty, list -> {
            //                    List<Optional<T>> os = Lists.newArrayList();
            //                    for(ITerm t : ListTerms.iterable(list)) {
            //                        os.add(m.match(t, unifier));
            //                    }
            //                    return Optionals.sequence(os).map(ts -> (R) f.apply(list, ImmutableList.copyOf(ts)));
            //                }, this::empty, this::empty, this::empty, this::empty));
            //            };
        }

        public IMatcher<IConsList> cons() {
            return cons(t -> t);
        }

        public <R> IMatcher<R> cons(Function1<? super IConsList, R> f) {
            return cons(M.term(), (t, u) -> Optional.of(t), (cons, hd, tl) -> f.apply(cons));
        }

        public <THd, TTl, R> IMatcher<R> cons(IMatcher<? extends THd> mhd, IListMatcher<? extends TTl> mtl,
                Function3<? super IConsList, ? super THd, ? super TTl, R> f) {
            return (term, unifier) -> {
                return M.appl2(ListTerms.CONS_OP, M.term(), list(), (cons, hd, tl) -> {
                    final Optional<? extends THd> ohd = mhd.match(hd, unifier);
                    final Optional<? extends TTl> otl = mtl.match(tl, unifier);
                    return Optionals.lift(ohd, otl,
                            (thd, ttl) -> (R) f.apply(LB.newCons(hd, tl, cons.getAttachments()), thd, ttl));
                }).match(term, unifier).flatMap(o -> o);
            };
        }

        public IMatcher<INilList> nil() {
            return nil(t -> t);
        }

        public <R> IMatcher<R> nil(Function1<? super INilList, R> f) {
            return M.appl0(ListTerms.NIL_OP, t -> f.apply(LB.newNil(t.getAttachments())));
        }

        public IMatcher<IListVar> var() {
            return var(t -> t);
        }

        public <R> IMatcher<R> var(Function1<? super IListVar, R> f) {
            return M.var(v -> f.apply(LB.newVar(v.getResource(), v.getName(), v.getAttachments())));
        }

    }

    @FunctionalInterface
    public interface IListMatcher<T> {

        Optional<T> match(IListTerm term, IUnifier unifier);

        default Optional<T> match(IListTerm term) {
            return match(term, Unifiers.Immutable.of());
        }

        default <R> IListMatcher<R> map(Function<T, R> fun) {
            return (term, unifier) -> this.match(term, unifier).<R>map(fun);
        }

        default IListMatcher<T> filter(Predicate<T> pred) {
            return (term, unifier) -> this.match(term, unifier).filter(pred);
        }

        default <R> IListMatcher<R> flatMap(Function<T, Optional<R>> fun) {
            return (term, unifier) -> this.match(term, unifier).<R>flatMap(fun);
        }

        static <T> IListMatcher<T> flatten(IListMatcher<Optional<T>> m) {
            return m.flatMap(o -> o)::match;
        }

    }

}