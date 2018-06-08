package mb.statix.solver.query;

import static mb.nabl2.terms.matching.TermMatch.M;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.scopegraph.reference.DataEquiv;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Completeness;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.State;
import mb.statix.terms.AOccurrence;
import mb.statix.terms.Occurrence;

public class ResolveMin implements IQueryMin {

    private final ITerm refTerm;

    public ResolveMin(ITerm refTerm) {
        this.refTerm = refTerm;
    }

    public LabelOrder<ITerm> getLabelOrder(State state, Completeness completeness, IDebugContext debug)
            throws ResolutionException {
        return get(state).getLabelOrder(state, completeness, debug);
    }

    public DataEquiv<ITerm> getDataEquiv(State state, Completeness completeness, IDebugContext debug)
            throws ResolutionException {
        return get(state).getDataEquiv(state, completeness, debug);
    }

    private NamespaceQuery get(State state) throws ResolutionException {
        final Occurrence ref = AOccurrence.matcher(M.term()).match(refTerm, state.unifier())
                .orElseThrow(() -> new ResolutionException("Reference match delayed"));
        return state.spec().namespaceQueries().getOrDefault(ref.getNamespace(), NamespaceQuery.DEFAULT);
    }

    @Override public String toString(IUnifier unifier) {
        final StringBuilder sb = new StringBuilder();
        sb.append("min of ");
        sb.append(unifier.toString(refTerm));
        return sb.toString();
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("min of ");
        sb.append(refTerm);
        return sb.toString();
    }

}