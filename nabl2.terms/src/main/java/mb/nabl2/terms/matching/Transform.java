package mb.nabl2.terms.matching;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.metaborg.util.collection.ImList;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.PartialFunction1;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.unit.Unit;

import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.matching.TermMatch.IMatcher;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

public class Transform {

    public static final T T = new T();

    public static class T {

        public Function1<ITerm, ITerm> sometd(PartialFunction1<ITerm, ITerm> m) {
            // @formatter:off
            return term -> m.apply(term).orElseGet(() -> term.match(Terms.cases(
                (appl) -> {
                    final ImList.Immutable<ITerm> newArgs;
                    if((newArgs = Terms.applyLazy(appl.getArgs(), sometd(m)::apply)) == null) {
                        return appl;
                    }
                    return B.newAppl(appl.getOp(), newArgs, appl.getAttachments());
                },
                (list) -> list.match(ListTerms.<IListTerm> cases(
                    (cons) -> B.newCons(sometd(m).apply(cons.getHead()), (IListTerm) sometd(m).apply(cons.getTail()), cons.getAttachments()),
                    (nil) -> nil,
                    (var) -> var
                )),
                (string) -> string,
                (integer) -> integer,
                (blob) -> blob,
                (var) -> var
            )));
            // @formatter:on
        }

        public Function1<ITerm, ITerm> somebu(PartialFunction1<ITerm, ITerm> m) {
            return term -> {
                // @formatter:off
                ITerm next = term.match(Terms.<ITerm>cases(
                    (appl) -> {
                        final ImList.Immutable<ITerm> newArgs;
                        if((newArgs = Terms.applyLazy(appl.getArgs(), somebu(m)::apply)) == null) {
                            return appl;
                        }
                        return B.newAppl(appl.getOp(), newArgs, appl.getAttachments());
                    },
                    (list) -> list.match(ListTerms.<IListTerm> cases(
                        (cons) -> B.newCons(somebu(m).apply(cons.getHead()), (IListTerm) somebu(m).apply(cons.getTail()), cons.getAttachments()),
                        (nil) -> nil,
                        (var) -> var
                    )),
                    (string) -> string,
                    (integer) -> integer,
                    (blob) -> blob,
                    (var) -> var
                ));
                // @formatter:on
                return m.apply(next).orElse(next);
            };
        }

        public <R> Function1<ITerm, Collection<R>> collecttd(PartialFunction1<ITerm, ? extends R> m) {
            return term -> {
                List<R> results = new ArrayList<>();
                M.<Unit>casesFix(f -> Iterables2.<IMatcher<? extends Unit>>from(
                // @formatter:off
                    (t, u) -> m.apply(t).map(r -> {
                        results.add(r);
                        return Unit.unit;
                    }),
                    (t, u) -> Optional.of(t.match(Terms.<Unit>cases(
                        (appl) -> {
                            for(ITerm arg : appl.getArgs()) {
                                f.match(arg, u);
                            }
                            return Unit.unit;
                        },
                        (list) -> list.match(ListTerms.<Unit> cases(
                            (cons) -> {
                                f.match(cons.getHead(), u);
                                f.match(cons.getTail(), u);
                                return Unit.unit;
                            },
                            (nil) -> Unit.unit,
                            (var) -> Unit.unit
                        )),
                        (string) -> Unit.unit,
                        (integer) -> Unit.unit,
                        (blob) -> Unit.unit,
                        (var) -> Unit.unit
                    )))
                    // @formatter:on
                )).match(term);
                return results;
            };
        }

        public Function1<ITerm, ITerm> maybe(PartialFunction1<ITerm, ITerm> m) {
            return term -> m.apply(term).orElse(term);
        }

    }

}
