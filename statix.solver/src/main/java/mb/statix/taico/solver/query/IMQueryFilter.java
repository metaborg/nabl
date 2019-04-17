package mb.statix.taico.solver.query;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.log.IDebugContext;
import mb.statix.taico.solver.ICompleteness;
import mb.statix.taico.solver.IMState;

/**
 * Interface to represent query filters.
 * 
 * <pre>filter &lt;pathConstraint&gt; and &lt;dataConstraint&gt;</pre>
 */
public interface IMQueryFilter {

    /**
     * Creates a copy of this query filter and then applies the given substitution to the copy.
     * 
     * @param subst
     *      the substitution to apply
     * 
     * @return
     *      the new query filter
     */
    IMQueryFilter apply(ISubstitution.Immutable subst);

    /**
     * @param state
     *      the state
     * @param isComplete
     *      the isComplete predicate
     * @param debug
     *      the debug
     * 
     * @return
     *      the label wellformedness
     * 
     * @throws ResolutionException
     */
    LabelWF<ITerm> getLabelWF(IMState state, ICompleteness isComplete, IDebugContext debug) throws ResolutionException;

    /**
     * @param state
     *      the state
     * @param isComplete
     *      the isComplete predicate
     * @param debug
     *      the debug
     * 
     * @return
     *      the data wellformedness
     * 
     * @throws ResolutionException
     */
    DataWF<ITerm> getDataWF(IMState state, ICompleteness isComplete, IDebugContext debug) throws ResolutionException;

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
