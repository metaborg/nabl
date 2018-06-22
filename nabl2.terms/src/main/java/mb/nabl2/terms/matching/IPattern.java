package mb.nabl2.terms.matching;

import java.util.Arrays;

import org.immutables.value.Value;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;

public interface IPattern {

    MatchResult match(Iterable<ITerm> terms) throws MatchException;

    default MatchResult match(ITerm... terms) throws MatchException {
        return match(Arrays.asList(terms));
    }

    @Value.Immutable
    static abstract class MatchResult {

        @Value.Parameter public abstract ISubstitution.Immutable substitution();

        @Value.Parameter public abstract IUnifier.Immutable unifier();

        public ITerm apply(ITerm term) {
            return unifier().findRecursive(substitution().apply(term));
        }

    }

}