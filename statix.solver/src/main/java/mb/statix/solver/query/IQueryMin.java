package mb.statix.solver.query;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Completeness;
import mb.statix.solver.State;
import mb.statix.solver.log.IDebugContext;

/**
 * Interface to represent query min.
 * 
 * <pre>min &lt;pathConstraint&gt; and &lt;dataConstraint&gt;</pre>
 */
public interface IQueryMin {

    IQueryMin apply(ISubstitution.Immutable subst);

    LabelOrder<ITerm> getLabelOrder(State state, Completeness iCompleteness, IDebugContext debug)
            throws ResolutionException;

    DataLeq<ITerm> getDataEquiv(State state, Completeness iCompleteness, IDebugContext debug)
            throws ResolutionException;

    String toString(TermFormatter termToString);

}