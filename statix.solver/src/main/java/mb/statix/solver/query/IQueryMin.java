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

public interface IQueryMin {

    IQueryMin apply(ISubstitution.Immutable subst);

    LabelOrder<ITerm> getLabelOrder(State state, Predicate3<ITerm, ITerm, State> isComplete, IDebugContext debug)
            throws ResolutionException;

    DataLeq<ITerm> getDataEquiv(State state, Predicate3<ITerm, ITerm, State> isComplete, IDebugContext debug)
            throws ResolutionException;

    String toString(TermFormatter termToString);

}