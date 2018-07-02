package mb.statix.solver.query;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Completeness;
import mb.statix.solver.State;
import mb.statix.solver.log.IDebugContext;

public interface IQueryFilter {

    IQueryFilter apply(ISubstitution.Immutable subst);

    LabelWF<ITerm> getLabelWF(State state, Completeness completeness, IDebugContext debug) throws ResolutionException;

    DataWF<ITerm> getDataWF(State state, Completeness completeness, IDebugContext debug) throws ResolutionException;

    String toString(IUnifier unifier);

}