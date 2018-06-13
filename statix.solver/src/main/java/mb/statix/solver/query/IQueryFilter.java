package mb.statix.solver.query;

import org.metaborg.util.functions.Function1;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Completeness;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.State;

public interface IQueryFilter {

    IQueryFilter apply(Function1<ITerm, ITerm> map);

    LabelWF<ITerm> getLabelWF(State state, Completeness completeness, IDebugContext debug) throws ResolutionException;

    DataWF<ITerm> getDataWF(State state, Completeness completeness, IDebugContext debug) throws ResolutionException;

    String toString(IUnifier unifier);

}