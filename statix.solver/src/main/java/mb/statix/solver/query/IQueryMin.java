package mb.statix.solver.query;

import org.metaborg.util.functions.Predicate3;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.State;
import mb.statix.solver.log.IDebugContext;
import mb.statix.taico.solver.query.IMQueryMin;

/**
 * Interface to represent query min.
 * 
 * <pre>min &lt;pathConstraint&gt; and &lt;dataConstraint&gt;</pre>
 */
public interface IQueryMin {

    IQueryMin apply(ISubstitution.Immutable subst);

    LabelOrder<ITerm> getLabelOrder(State state, Predicate3<ITerm, ITerm, State> isComplete, IDebugContext debug)
            throws ResolutionException;

    DataLeq<ITerm> getDataEquiv(State state, Predicate3<ITerm, ITerm, State> isComplete, IDebugContext debug)
            throws ResolutionException;

    String toString(TermFormatter termToString);

    /**
     * Converts this query min to a mutable query min.
     * 
     * @return
     *      the mutable copy of this query min
     */
    IMQueryMin toMutable();
}