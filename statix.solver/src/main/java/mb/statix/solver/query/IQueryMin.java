package mb.statix.solver.query;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.scopegraph.reference.DataEquiv;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Completeness;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.State;

public interface IQueryMin {

    LabelOrder<ITerm> getLabelOrder(State state, Completeness completeness, IDebugContext debug)
            throws ResolutionException;

    DataEquiv<ITerm> getDataEquiv(State state, Completeness completeness, IDebugContext debug)
            throws ResolutionException;

    String toString(IUnifier unifier);

}