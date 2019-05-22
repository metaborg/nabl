package mb.statix.solver.query;

import mb.nabl2.regexp.IRegExpMatcher;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.spec.IRule;

/**
 * Interface to represent query filters.
 * 
 * <pre>filter &lt;pathConstraint&gt; and &lt;dataConstraint&gt;</pre>
 */
public interface IQueryFilter {

    IRegExpMatcher<ITerm> getLabelWF();

    IRule getDataWF();

    /**
     * Creates a copy of this query filter and then applies the given substitution to the copy.
     * 
     * @param subst
     *      the substitution to apply
     * 
     * @return
     *      the new query filter
     */
    IQueryFilter apply(ISubstitution.Immutable subst);

    /**
     * Converts this query filter into string representation, where terms are formatted with the
     * given term formatter.
     * 
     * <pre>filter pathFilter and dataFilter</pre>
     * 
     * @param termToString
     *      the term formatter
     * 
     * @return
     *      the string
     */
    String toString(TermFormatter termToString);
}