package mb.statix.solver.query;

import static mb.nabl2.terms.matching.TermMatch.M;

import javax.annotation.Nullable;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.scopegraph.reference.DataEquiv;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ResolutionException;
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

    public LabelWF<ITerm> getLabelWF(State state, IDebugContext debug) {
        if(pathFilterConstraint == null) {
            return LabelWF.ANY();
        } else {
            return LabelWF.ANY(); // FIXME Use pathFilterConstraint
        }
    }

    public DataWF<ITerm> getDataWF(Occurrence ref, State state, IDebugContext debug) {
        final IUnifier unifier = state.unifier();
        return new DataWF<ITerm>() {
            public boolean wf(ITerm datum) throws ResolutionException, InterruptedException {
                final Occurrence decl = AOccurrence.matcher(M.term()).match(datum, unifier)
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

    public LabelOrder<ITerm> getLabelOrder(State state, IDebugContext debug) {
        if(pathMinConstraint == null) {
            return LabelOrder.NONE();
        } else {
            return LabelOrder.NONE(); // FIXME User pathMinConstraint
        }
    }

    public DataEquiv<ITerm> getDataEquiv(State state, IDebugContext debug) {
        return DataEquiv.ALL();
    }

}