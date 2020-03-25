package mb.nabl2.terms.matching;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.Predicate1;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.u.IUnifier;

public class TermPattern {

    public static P P = new P();

    public static class P {

        public Pattern newAppl(String op, Pattern... args) {
            return newAppl(op, Arrays.asList(args), ImmutableClassToInstanceMap.of());
        }

        public Pattern newAppl(String op, Iterable<? extends Pattern> args,
                ImmutableClassToInstanceMap<Object> attachments) {
            if(op.equals("")) {
                throw new IllegalArgumentException();
            }
            return new ApplPattern(op, args, attachments);
        }

        public Pattern newTuple(Pattern... args) {
            return newTuple(Arrays.asList(args), ImmutableClassToInstanceMap.of());
        }

        public Pattern newTuple(Iterable<? extends Pattern> args) {
            return newTuple(args, ImmutableClassToInstanceMap.of());
        }

        public Pattern newTuple(Iterable<? extends Pattern> args, ImmutableClassToInstanceMap<Object> attachments) {
            final List<Pattern> argList = ImmutableList.copyOf(args);
            if(argList.size() == 1) {
                return argList.get(0);
            } else {
                return new ApplPattern(Terms.TUPLE_OP, argList, attachments);
            }
        }

        public Pattern newString(String value) {
            return new StringPattern(value, ImmutableClassToInstanceMap.of());
        }

        public Pattern newString(String value, ImmutableClassToInstanceMap<Object> attachments) {
            return new StringPattern(value, attachments);
        }

        public Pattern newInt(int value) {
            return new IntPattern(value, ImmutableClassToInstanceMap.of());
        }

        public Pattern newInt(int value, ImmutableClassToInstanceMap<Object> attachments) {
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
                    final List<Pattern> args = appl.getArgs().stream().map(a -> fromTerm(a, isWildcard)).collect(ImmutableList.toImmutableList());
                    return new ApplPattern(appl.getOp(), args, appl.getAttachments());
                },
                string -> new StringPattern(string.getValue(), string.getAttachments()),
                integer -> new IntPattern(integer.getValue(), integer.getAttachments()),
                blob -> {
                    throw new IllegalArgumentException("Cannot create blob patterns.");
                },
                var -> isWildcard.test(var) ? new PatternVar() : new PatternVar(var)
            ));
            // @formatter:on
        }

        public Optional<ISubstitution.Immutable> match(final Iterable<Pattern> patterns, final Iterable<ITerm> terms) {
            return TermPattern.P.newTuple(patterns).match(B.newTuple(terms));
        }

        public MaybeNotInstantiated<Optional<ISubstitution.Immutable>> match(final Iterable<Pattern> patterns,
                final Iterable<? extends ITerm> terms, IUnifier.Immutable unifier) {
            return TermPattern.P.newTuple(patterns).match(B.newTuple(terms), unifier);
        }

        public Optional<MatchResult> matchWithEqs(final Iterable<Pattern> patterns,
                final Iterable<? extends ITerm> terms, IUnifier.Immutable unifier,
                Function1<Optional<ITermVar>, ITermVar> fresh) {
            return TermPattern.P.newTuple(patterns).matchWithEqs(B.newTuple(terms), unifier, fresh);
        }

    }

}
