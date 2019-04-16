package mb.statix.solver.query;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Completeness;
import mb.statix.solver.State;
import mb.statix.solver.log.IDebugContext;
import mb.statix.taico.solver.query.IMQueryFilter;

/**
 * Interface to represent query filters.
 * 
 * <pre>filter &lt;pathConstraint&gt; and &lt;dataConstraint&gt;</pre>
 */
public interface IQueryFilter {

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

    LabelWF<ITerm> getLabelWF(State state, Completeness completeness, IDebugContext debug) throws ResolutionException;

    DataWF<ITerm> getDataWF(State state, Completeness completeness, IDebugContext debug) throws ResolutionException;

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

    /**
     * Converts this query filter to a query filter for mutable state.
     * This method creates a copy of this filter.
     * 
     * @return
     *      the mutable query filter
     */
    IMQueryFilter toMutable();
}