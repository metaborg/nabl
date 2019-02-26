package mb.statix.taico.solver.query;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.log.IDebugContext;
import mb.statix.taico.solver.MCompleteness;
import mb.statix.taico.solver.MState;

/**
 * Interface to represent query min.
 * 
 * <pre>min &lt;pathConstraint&gt; and &lt;dataConstraint&gt;</pre>
 */
public interface IMQueryMin {

    IMQueryMin apply(ISubstitution.Immutable subst);

    LabelOrder<ITerm> getLabelOrder(MState state, MCompleteness iCompleteness, IDebugContext debug)
            throws ResolutionException;

    DataLeq<ITerm> getDataEquiv(MState state, MCompleteness iCompleteness, IDebugContext debug)
            throws ResolutionException;

    String toString(TermFormatter termToString);

}