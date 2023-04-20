package mb.nabl2.terms.matching;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;

import org.metaborg.util.collection.ImList;
import org.metaborg.util.functions.Predicate1;

import mb.nabl2.terms.IAttachments;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.build.Attachments;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.u.IUnifier;

import static mb.nabl2.terms.build.TermBuild.B;

public class TermPattern {

    public static P P = new P();

    public static class P {

        public Pattern newAppl(String op, Pattern... args) {
            return newAppl(op, Arrays.asList(args), Attachments.empty());
        }

        public Pattern newAppl(String op, Iterable<? extends Pattern> args, IAttachments attachments) {
            if(op.equals("")) {
                throw new IllegalArgumentException();
            }
            return new ApplPattern(op, args, attachments);
        }

        public Pattern newTuple(Pattern... args) {
            return newTuple(Arrays.asList(args), Attachments.empty());
        }

        public Pattern newTuple(Iterable<? extends Pattern> args) {
            return newTuple(args, Attachments.empty());
        }

        public Pattern newTuple(Iterable<? extends Pattern> args, IAttachments attachments) {
            final List<Pattern> argList = ImList.Immutable.copyOf(args);
            if(argList.size() == 1) {
                return argList.get(0);
            } else {
                return new ApplPattern(Terms.TUPLE_OP, argList, attachments);
            }
        }

        public Pattern newList(List<? extends Pattern> args) {
            return newListTail(args, newNil(), Attachments.empty());
        }

        public Pattern newList(List<? extends Pattern> args, IAttachments attachments) {
            return newListTail(args, newNil(attachments), attachments);
        }

        public Pattern newListTail(List<? extends Pattern> args, Pattern tail) {
            return newListTail(args, tail, Attachments.empty());
        }

        public Pattern newListTail(List<? extends Pattern> args, Pattern tail, IAttachments attachments) {
            Pattern list = tail;
            for(ListIterator<? extends Pattern> iter =
                   args.listIterator(args.size()); iter.hasPrevious(); ) {
                Pattern elem = iter.previous();
                list = newCons(elem, list, attachments);
            }
            return list;
        }

        public Pattern newCons(Pattern head, Pattern tail, IAttachments attachments) {
            return new ConsPattern(head, tail, attachments);
        }

        public Pattern newNil() {
            return new NilPattern(Attachments.empty());
        }

        public Pattern newNil(IAttachments attachments) {
            return new NilPattern(attachments);
        }

        public Pattern newString(String value) {
            return new StringPattern(value, Attachments.empty());
        }

        public Pattern newString(String value, IAttachments attachments) {
            return new StringPattern(value, attachments);
        }

        public Pattern newInt(int value) {
            return new IntPattern(value, Attachments.empty());
        }

        public Pattern newInt(int value, IAttachments attachments) {
            return new IntPattern(value, attachments);
        }

        public Pattern newWld() {
            return new PatternVar();
        }

        public Pattern newVar(String name) {
            return new PatternVar(name);
        }

        public Pattern newVar(ITermVar var) {
            return new PatternVar(var);
        }

        public Pattern newAs(String name, Pattern pattern) {
            return new PatternAs(name, pattern);
        }

        public Pattern newAs(ITermVar var, Pattern pattern) {
            return new PatternAs(var, pattern);
        }

        public Pattern newAs(Pattern pattern) {
            return new PatternAs(pattern);
        }

        public Pattern fromTerm(ITerm term) {
            return fromTerm(term, v -> false);
        }

        public Pattern fromTerm(ITerm term, Predicate1<ITermVar> isWildcard) {
            // @formatter:off
            return term.match(Terms.cases(
                appl -> {
                    final List<ITerm> args = appl.getArgs();
                    final ImList.Mutable<Pattern> newArgs = new ImList.Mutable<>(args.size());
                    for(ITerm arg : args) {
                        newArgs.add(fromTerm(arg, isWildcard));
                    }
                    return new ApplPattern(appl.getOp(), newArgs.freeze(), appl.getAttachments());
                },
                list -> list.match(ListTerms.cases(
                    cons -> new ConsPattern(fromTerm(cons.getHead(), isWildcard), fromTerm(cons.getTail(), isWildcard), cons.getAttachments()),
                    nil -> new NilPattern(nil.getAttachments()),
                    var -> isWildcard.test(var) ? new PatternVar() : new PatternVar(var)
                )),
                string -> new StringPattern(string.getValue(), string.getAttachments()),
                integer -> new IntPattern(integer.getValue(), integer.getAttachments()),
                blob -> {
                    throw new IllegalArgumentException("Cannot create blob patterns.");
                },
                var -> isWildcard.test(var) ? new PatternVar() : new PatternVar(var)
            ));
            // @formatter:on
        }

        public Optional<ISubstitution.Immutable> match(final Iterable<Pattern> patterns, final Collection<ITerm> terms) {
            return TermPattern.P.newTuple(patterns).match(B.newTuple(terms));
        }

        public MaybeNotInstantiated<Optional<ISubstitution.Immutable>> match(final Iterable<Pattern> patterns,
                final Collection<? extends ITerm> terms, IUnifier.Immutable unifier) {
            return TermPattern.P.newTuple(patterns).match(B.newTuple(terms), unifier);
        }

        public Optional<MatchResult> matchWithEqs(final Iterable<Pattern> patterns,
                final Collection<? extends ITerm> terms, IUnifier.Immutable unifier, VarProvider fresh) {
            return TermPattern.P.newTuple(patterns).matchWithEqs(B.newTuple(terms), unifier, fresh);
        }

    }

}