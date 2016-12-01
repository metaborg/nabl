package org.metaborg.meta.nabl2.stratego;

import java.util.LinkedList;
import java.util.Optional;

import org.metaborg.meta.nabl2.functions.CheckedFunction0;
import org.metaborg.meta.nabl2.functions.CheckedFunction1;
import org.metaborg.meta.nabl2.functions.CheckedFunction2;
import org.metaborg.meta.nabl2.functions.Function0;
import org.metaborg.meta.nabl2.functions.Function1;
import org.metaborg.meta.nabl2.functions.Function2;
import org.metaborg.meta.nabl2.functions.Function3;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.common.collect.Lists;

public class StrategoMatchers {

    public static <T> MatchBuilder<T> patterns() {
        return new MatchBuilder<>();
    }

    public static class MatchBuilder<T> {

        private LinkedList<IMatcher<Optional<T>>> matchers = Lists.newLinkedList();

        public MatchBuilder<T> appl0(String op, Function0<T> f) {
            matchers.add(new IMatcher<Optional<T>>() {

                @Override public Optional<T> match(IStrategoTerm term) {
                    if (Tools.isTermAppl(term) && Tools.hasConstructor((IStrategoAppl) term, op, 0)) {
                        return Optional.of(f.apply());
                    } else {
                        return Optional.empty();
                    }
                }

            });
            return this;
        }

        public MatchBuilder<T> appl1(String op, Function1<IStrategoTerm,T> f) {
            matchers.add(new IMatcher<Optional<T>>() {

                @Override public Optional<T> match(IStrategoTerm term) {
                    if (Tools.isTermAppl(term) && Tools.hasConstructor((IStrategoAppl) term, op, 1)) {
                        return Optional.of(f.apply(term.getSubterm(0)));
                    } else {
                        return Optional.empty();
                    }
                }

            });
            return this;
        }

        public MatchBuilder<T> appl2(String op, Function2<IStrategoTerm,IStrategoTerm,T> f) {
            matchers.add(new IMatcher<Optional<T>>() {

                @Override public Optional<T> match(IStrategoTerm term) {
                    if (Tools.isTermAppl(term) && Tools.hasConstructor((IStrategoAppl) term, op, 2)) {
                        return Optional.of(f.apply(term.getSubterm(0), term.getSubterm(1)));
                    } else {
                        return Optional.empty();
                    }
                }

            });
            return this;
        }

        public MatchBuilder<T> appl3(String op, Function3<IStrategoTerm,IStrategoTerm,IStrategoTerm,T> f) {
            matchers.add(new IMatcher<Optional<T>>() {

                @Override public Optional<T> match(IStrategoTerm term) {
                    if (Tools.isTermAppl(term) && Tools.hasConstructor((IStrategoAppl) term, op, 3)) {
                        return Optional.of(f.apply(term.getSubterm(0), term.getSubterm(1), term.getSubterm(2)));
                    } else {
                        return Optional.empty();
                    }
                }

            });
            return this;
        }

        public IMatcher<T> otherwise(Function0<T> f) {
            return new IMatcher<T>() {

                @Override public T match(IStrategoTerm term) {
                    for (IMatcher<Optional<T>> matcher : matchers) {
                        Optional<T> result = matcher.match(term);
                        if (result.isPresent()) {
                            return result.get();
                        }
                    }
                    return f.apply();
                }

            };
        }

    }

    public interface IMatcher<T> {

        T match(IStrategoTerm term);

    }


    public static <T, E extends Throwable> CheckedMatchBuilder<T,E> patternsThrows() {
        return new CheckedMatchBuilder<>();
    }

    public static class CheckedMatchBuilder<T, E extends Throwable> {

        private LinkedList<ICheckedMatcher<Optional<T>,E>> matchers = Lists.newLinkedList();

        public CheckedMatchBuilder<T,E> appl0(String op, CheckedFunction0<T,E> f) {
            matchers.add(new ICheckedMatcher<Optional<T>,E>() {

                @Override public Optional<T> match(IStrategoTerm term) throws E {
                    if (Tools.isTermAppl(term) && Tools.hasConstructor((IStrategoAppl) term, op, 0)) {
                        return Optional.of(f.apply());
                    } else {
                        return Optional.empty();
                    }
                }

            });
            return this;
        }

        public CheckedMatchBuilder<T,E> appl1(String op, CheckedFunction1<IStrategoTerm,T,E> f) {
            matchers.add(new ICheckedMatcher<Optional<T>,E>() {

                @Override public Optional<T> match(IStrategoTerm term) throws E {
                    if (Tools.isTermAppl(term) && Tools.hasConstructor((IStrategoAppl) term, op, 1)) {
                        return Optional.of(f.apply(term.getSubterm(0)));
                    } else {
                        return Optional.empty();
                    }
                }

            });
            return this;
        }

        public CheckedMatchBuilder<T,E> appl2(String op, CheckedFunction2<IStrategoTerm,IStrategoTerm,T,E> f) {
            matchers.add(new ICheckedMatcher<Optional<T>,E>() {

                @Override public Optional<T> match(IStrategoTerm term) throws E {
                    if (Tools.isTermAppl(term) && Tools.hasConstructor((IStrategoAppl) term, op, 2)) {
                        return Optional.of(f.apply(term.getSubterm(0), term.getSubterm(1)));
                    } else {
                        return Optional.empty();
                    }
                }

            });
            return this;
        }

        public ICheckedMatcher<T,E> otherwise(CheckedFunction0<T,E> f) {
            return new ICheckedMatcher<T,E>() {

                @Override public T match(IStrategoTerm term) throws E {
                    for (ICheckedMatcher<Optional<T>,E> matcher : matchers) {
                        Optional<T> result = matcher.match(term);
                        if (result.isPresent()) {
                            return result.get();
                        }
                    }
                    return f.apply();
                }

            };
        }

    }

    public interface ICheckedMatcher<T, E extends Throwable> {

        T match(IStrategoTerm term) throws E;

    }

}