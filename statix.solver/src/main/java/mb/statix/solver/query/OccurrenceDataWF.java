package mb.statix.solver.query;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Completeness;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.State;
import mb.statix.terms.AOccurrence;
import mb.statix.terms.Occurrence;

public class OccurrenceDataWF implements DataWF<ITerm> {

    private final Occurrence ref;
    private final State state;
    @SuppressWarnings("unused") private final Completeness completeness;
    @SuppressWarnings("unused") private final IDebugContext debug;

    public OccurrenceDataWF(Occurrence ref, State state, Completeness completeness, IDebugContext debug) {
        this.ref = ref;
        this.state = state;
        this.completeness = completeness;
        this.debug = debug;
    }

    public boolean wf(List<ITerm> datum) throws ResolutionException, InterruptedException {
        final IUnifier unifier = state.unifier();
        if(datum.size() != 1) {
            throw new IllegalArgumentException("Argument count mismatch for decl.");
        }
        final ITerm declTerm = datum.get(0);
        final Occurrence decl = AOccurrence.matcher(M.term()).match(declTerm, unifier)
                .orElseThrow(() -> new ResolutionException("Declaration match delayed"));
        if(!ref.getNamespace().equals(decl.getNamespace())) {
            return false;
        }
        if(ref.getName().size() != decl.getName().size()) {
            throw new IllegalArgumentException("Argument count mismatch for ref and decl occurrence.");
        }
        for(int i = 0; i < ref.getName().size(); i++) {
            final ITerm refArg = ref.getName().get(i);
            final ITerm declArg = decl.getName().get(i);
            if(unifier.areUnequal(refArg, declArg)) {
                return false;
            }
            if(!unifier.areEqual(ref.getName().get(i), decl.getName().get(i))) {
                throw new ResolutionException("Matching argument " + i + " delayed: " + unifier.toString(refArg) + " ~ " + unifier.toString(declArg));
            }
        }
        return true;
    }

}