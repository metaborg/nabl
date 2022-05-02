package mb.nabl2.terms.matching;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.metaborg.util.functions.Predicate1;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.IAttachments;
import mb.nabl2.terms.IConsTerm;
import mb.nabl2.terms.IIntTerm;
import mb.nabl2.terms.INilTerm;
import mb.nabl2.terms.IStringTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
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
            if(op.isEmpty()) {
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
            final List<Pattern> argList = ImmutableList.copyOf(args);
            if(argList.size() == 1) {
                return argList.get(0);
            } else {
                return new ApplPattern(Terms.TUPLE_OP, argList, attachments);
            }
        }

        public Pattern newList(Iterable<? extends Pattern> args) {
            return newListTail(args, newNil(), Attachments.empty());
        }

        public Pattern newList(Iterable<? extends Pattern> args, IAttachments attachments) {
            return newListTail(args, newNil(attachments), attachments);
        }

        public Pattern newListTail(Iterable<? extends Pattern> args, Pattern tail) {
            return newListTail(args, tail, Attachments.empty());
        }

        public Pattern newListTail(Iterable<? extends Pattern> args, Pattern tail, IAttachments attachments) {
            Pattern list = tail;
            for(Pattern elem : ImmutableList.copyOf(args).reverse()) {
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
            switch(term.termTag()) {
                case IApplTerm: {
                    IApplTerm appl = (IApplTerm) term;
                    final List<ITerm> args = appl.getArgs();
                    final ImmutableList.Builder<Pattern> newArgs =
                        ImmutableList.builderWithExpectedSize(args.size());
                    for(ITerm arg : args) {
                        newArgs.add(fromTerm(arg, isWildcard));
                    }
                    return new ApplPattern(appl.getOp(), newArgs.build(), appl.getAttachments());
                }

                case IConsTerm: {
                    IConsTerm cons = (IConsTerm) term;
                    return new ConsPattern(fromTerm(cons.getHead(), isWildcard),
                        fromTerm(cons.getTail(), isWildcard), cons.getAttachments());
                }

                case INilTerm: {
                    INilTerm nil = (INilTerm) term;
                    return new NilPattern(nil.getAttachments());
                }

                case IStringTerm: {
                    IStringTerm string = (IStringTerm) term;
                    return new StringPattern(string.getValue(), string.getAttachments());
                }

                case IIntTerm: {
                    IIntTerm integer = (IIntTerm) term;
                    return new IntPattern(integer.getValue(), integer.getAttachments());
                }

                case IBlobTerm: {
                    throw new IllegalArgumentException("Cannot create blob patterns.");
                }

                case ITermVar: {
                    ITermVar var = (ITermVar) term;
                    return isWildcard.test(var) ? new PatternVar() : new PatternVar(var);
                }
            }
            // N.B. don't use this in default case branch, instead use IDE to catch non-exhaustive switch statements
            throw new RuntimeException("Missing case for ITerm subclass/tag");
        }

        public Optional<ISubstitution.Immutable> match(final Iterable<Pattern> patterns, final Iterable<ITerm> terms) {
            return TermPattern.P.newTuple(patterns).match(B.newTuple(terms));
        }

        public MaybeNotInstantiated<Optional<ISubstitution.Immutable>> match(final Iterable<Pattern> patterns,
                final Iterable<? extends ITerm> terms, IUnifier.Immutable unifier) {
            return TermPattern.P.newTuple(patterns).match(B.newTuple(terms), unifier);
        }

        public Optional<MatchResult> matchWithEqs(final Iterable<Pattern> patterns,
                final Iterable<? extends ITerm> terms, IUnifier.Immutable unifier, VarProvider fresh) {
            return TermPattern.P.newTuple(patterns).matchWithEqs(B.newTuple(terms), unifier, fresh);
        }

    }

}