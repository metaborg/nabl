package mb.nabl2.terms.matching;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.PartialFunction1;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.unit.Unit;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.IConsTerm;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.Terms;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

public class Transform {

    public static final T T = new T();

    public static class T {

        public static Function1<ITerm, ITerm> sometd(PartialFunction1<ITerm, ITerm> m) {
            return term -> m.apply(term).orElseGet(() -> {
                switch(term.termTag()) {
                    case IApplTerm: {
                        IApplTerm appl = (IApplTerm) term;
                        final ImmutableList<ITerm> newArgs;
                        if((newArgs = Terms.applyLazy(appl.getArgs(), sometd(m))) == null) {
                            return appl;
                        }
                        return B.newAppl(appl.getOp(), newArgs, appl.getAttachments());
                    }

                    case IConsTerm: {
                        IConsTerm cons = (IConsTerm) term;
                        return B.newCons(sometd(m).apply(cons.getHead()),
                            (IListTerm) sometd(m).apply(cons.getTail()), cons.getAttachments());
                    }

                    case INilTerm:
                    case ITermVar:
                    case IBlobTerm:
                    case IIntTerm:
                    case IStringTerm: {
                        return term;
                    }
                }
                // N.B. don't use this in default case branch, instead use IDE to catch non-exhaustive switch statements
                throw new RuntimeException("Missing case for ITerm subclass/tag");
            });
        }

        public static Function1<ITerm, ITerm> somebu(PartialFunction1<ITerm, ITerm> m) {
            return term -> {
                ITerm next = null;
                switch(term.termTag()) {
                    case IApplTerm: {
                        IApplTerm appl = (IApplTerm) term;
                        final ImmutableList<ITerm> newArgs;
                        if((newArgs = Terms.applyLazy(appl.getArgs(), somebu(m)))
                            == null) {
                            next = appl;
                            break;
                        }
                        next = B.newAppl(appl.getOp(), newArgs, appl.getAttachments());
                        break;
                    }

                    case IConsTerm: {
                        IConsTerm cons = (IConsTerm) term;
                        next = B.newCons(somebu(m).apply(cons.getHead()),
                            (IListTerm) somebu(m).apply(cons.getTail()),
                            cons.getAttachments());
                        break;
                    }

                    case INilTerm:
                    case ITermVar:
                    case IBlobTerm:
                    case IIntTerm:
                    case IStringTerm: {
                        next = term;
                        break;
                    }
                }
                return m.apply(next).orElse(next);
            };
        }

        public static <R> Function1<ITerm, Collection<R>> collecttd(
            PartialFunction1<ITerm, ? extends R> m) {
            return term -> {
                List<R> results = new ArrayList<>();
                M.<Unit>casesFix(
                    f -> Iterables2.from((t, u) -> m.apply(t).map(r -> {
                        results.add(r);
                        return Unit.unit;
                    }), (t, u) -> {
                        switch(t.termTag()) {
                            case IApplTerm: {
                                IApplTerm appl = (IApplTerm) t;
                                for(ITerm arg : appl.getArgs()) {
                                    f.match(arg, u);
                                }
                                return Optional.of(Unit.unit);
                            }

                            case IConsTerm: {
                                IConsTerm cons = (IConsTerm) t;
                                f.match(cons.getHead(), u);
                                f.match(cons.getTail(), u);
                                return Optional.of(Unit.unit);
                            }

                            case INilTerm:
                            case IStringTerm:
                            case IIntTerm:
                            case IBlobTerm:
                            case ITermVar: {
                                return Optional.of(Unit.unit);
                            }
                        }
                        // N.B. don't use this in default case branch, instead use IDE to catch non-exhaustive switch statements
                        throw new RuntimeException("Missing case for ITerm subclass/tag");
                    })).match(term);
                return results;
            };
        }

        public static Function1<ITerm, ITerm> maybe(PartialFunction1<ITerm, ITerm> m) {
            return term -> m.apply(term).orElse(term);
        }

    }

}