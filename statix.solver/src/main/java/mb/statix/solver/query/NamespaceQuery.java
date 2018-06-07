package mb.statix.solver.query;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;

import javax.annotation.Nullable;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.scopegraph.reference.DataEquiv;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Completeness;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.State;
import mb.statix.terms.AOccurrence;
import mb.statix.terms.Occurrence;

@SuppressWarnings("unused")
public class NamespaceQuery {

    public static final NamespaceQuery DEFAULT = new NamespaceQuery();

    private final @Nullable String pathFilterConstraint;
    private final @Nullable String pathMinConstraint;

    public NamespaceQuery() {
        this(null, null);
    }

    public NamespaceQuery(@Nullable String pathFilterConstraint, @Nullable String pathMinConstraint) {
        this.pathFilterConstraint = pathFilterConstraint;
        this.pathMinConstraint = pathMinConstraint;
    }

    public LabelWF<ITerm> getLabelWF(State state, Completeness completeness, IDebugContext debug) {
        if(pathFilterConstraint == null) {
            return LabelWF.ANY();
        } else {
            return new ConstraintLabelWF(pathFilterConstraint, state, completeness, debug);
        }
    }

    public DataWF<ITerm> getDataWF(Occurrence ref, State state, Completeness completeness, IDebugContext debug) {
        final IUnifier unifier = state.unifier();
        return new DataWF<ITerm>() {
            public boolean wf(List<ITerm> datum) throws ResolutionException, InterruptedException {
                if(datum.size() != 1) {
                    throw new IllegalArgumentException("Argument count mismatch for decl.");
                }
                final ITerm declTerm = datum.get(0);
                final Occurrence decl = AOccurrence.matcher(M.term()).match(declTerm, unifier)
                        .orElseThrow(() -> new ResolutionException());
                if(!ref.getNamespace().equals(decl.getNamespace())) {
                    return false;
                }
                if(ref.getName().size() != decl.getName().size()) {
                    throw new IllegalArgumentException("Argument count mismatch for ref and decl occurrence.");
                }
                for(int i = 0; i < ref.getName().size(); i++) {
                    if(!unifier.areEqual(ref.getName().get(i), decl.getName().get(i))) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    public LabelOrder<ITerm> getLabelOrder(State state, Completeness completeness, IDebugContext debug) {
        if(pathMinConstraint == null) {
            return LabelOrder.NONE();
        } else {
            return new ConstraintLabelOrder(pathMinConstraint, state, completeness, debug);
        }
    }

    public DataEquiv<ITerm> getDataEquiv(State state, Completeness completeness, IDebugContext debug) {
        return DataEquiv.ALL();
    }

}