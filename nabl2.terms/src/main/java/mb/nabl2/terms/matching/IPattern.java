package mb.nabl2.terms.matching;

import java.util.Arrays;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;

public interface IPattern {

    ISubstitution.Immutable match(Iterable<ITerm> terms) throws MatchException;

    default ISubstitution.Immutable match(ITerm... terms) throws MatchException {
        return match(Arrays.asList(terms));
    }

}