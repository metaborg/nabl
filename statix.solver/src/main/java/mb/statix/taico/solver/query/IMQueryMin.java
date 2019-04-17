package mb.statix.taico.solver.query;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.log.IDebugContext;
import mb.statix.taico.solver.ICompleteness;
import mb.statix.taico.solver.IMState;

/**
 * Interface to represent query min.
 * 
 * <pre>min &lt;pathConstraint&gt; and &lt;dataConstraint&gt;</pre>
 */
public interface IMQueryMin {

    IMQueryMin apply(ISubstitution.Immutable subst);

    /**
     * @param state
     *      the state
     * @param isComplete
     *      the isComplete predicate
     * @param debug
     *      the debug context
     * 
     * @return
     *      the label ordering
     * 
     * @throws ResolutionException
     */
    LabelOrder<ITerm> getLabelOrder(IMState state, ICompleteness isComplete, IDebugContext debug)
            throws ResolutionException;

    /**
     * @param state
     *      the state
     * @param isComplete
     *      the isComplete predicate
     * @param debug
     *      the debug context
     * 
     * @return
     *      the data ordering
     * 
     * @throws ResolutionException
     */
    DataLeq<ITerm> getDataEquiv(IMState state, ICompleteness isComplete, IDebugContext debug)
            throws ResolutionException;

    String toString(TermFormatter termToString);

}