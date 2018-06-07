package mb.statix.solver.query;

import java.util.List;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Completeness;
import mb.statix.solver.Config;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.Solver;
import mb.statix.solver.State;
import mb.statix.solver.constraint.CUser;

public class QueryFilter implements IQueryFilter {

    private final String pathConstraint;
    private final String dataConstraint;

    public QueryFilter(String pathConstraint, String dataConstraint) {
        this.pathConstraint = pathConstraint;
        this.dataConstraint = dataConstraint;
    }

    public LabelWF<ITerm> getLabelWF(State state, Completeness completeness, IDebugContext debug) {
        return new ConstraintLabelWF(pathConstraint, state, completeness, debug);
    }

    public DataWF<ITerm> getDataWF(State state, Completeness completeness, IDebugContext debug) {
        return new DataWF<ITerm>() {
            public boolean wf(List<ITerm> datum) throws ResolutionException, InterruptedException {
                final IConstraint constraint = new CUser(dataConstraint, datum);
                final Config config = Config.of(state, ImmutableList.of(constraint), completeness);
                return Solver.entails(config, debug).orElseThrow(() -> new ResolutionException());
            }
        };
    }

    @Override public String toString(IUnifier unifier) {
        return toString();
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("filter ");
        sb.append(pathConstraint);
        sb.append(" and ");
        sb.append(dataConstraint);
        return sb.toString();
    }

}