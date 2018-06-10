package mb.statix.solver.query;

import javax.annotation.Nullable;

import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.reference.DataEquiv;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.solver.Completeness;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.State;
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
        return new OccurrenceDataWF(ref, state, completeness, debug);
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