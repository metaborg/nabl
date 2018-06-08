package mb.statix.solver.query;

import static mb.nabl2.terms.matching.TermMatch.M;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Completeness;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.State;
import mb.statix.terms.AOccurrence;
import mb.statix.terms.Occurrence;

public class ResolveFilter implements IQueryFilter {

    private final ITerm refTerm;

    public ResolveFilter(ITerm refTerm) {
        this.refTerm = refTerm;
    }

    public LabelWF<ITerm> getLabelWF(State state, Completeness completeness, IDebugContext debug)
            throws ResolutionException {
        return get(state).getLabelWF(state, completeness, debug);
    }

    public DataWF<ITerm> getDataWF(State state, Completeness completeness, IDebugContext debug)
            throws ResolutionException {
        final Occurrence ref = AOccurrence.matcher(M.term()).match(refTerm, state.unifier())
                .orElseThrow(() -> new ResolutionException("Reference match delayed"));
        return get(state).getDataWF(ref, state, completeness, debug);
    }

    private NamespaceQuery get(State state) throws ResolutionException {
        final Occurrence ref = AOccurrence.matcher(M.term()).match(refTerm, state.unifier())
                .orElseThrow(() -> new ResolutionException("Reference match delayed"));
        return state.spec().namespaceQueries().getOrDefault(ref.getNamespace(), NamespaceQuery.DEFAULT);
    }

    @Override public String toString(IUnifier unifier) {
        final StringBuilder sb = new StringBuilder();
        sb.append("filter of ");
        sb.append(unifier.toString(refTerm));
        return sb.toString();
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("filter of ");
        sb.append(refTerm);
        return sb.toString();
    }

}