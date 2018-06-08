package mb.statix.solver.query;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.scopegraph.reference.DataEquiv;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Completeness;
import mb.statix.solver.Config;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.Solver;
import mb.statix.solver.State;
import mb.statix.solver.constraint.CUser;

public class QueryMin implements IQueryMin {

    private final String pathConstraint;
    private final String dataConstraint;

    public QueryMin(String pathConstraint, String dataConstraint) {
        this.pathConstraint = pathConstraint;
        this.dataConstraint = dataConstraint;
    }

    public LabelOrder<ITerm> getLabelOrder(State state, Completeness completeness, IDebugContext debug) {
        return new ConstraintLabelOrder(pathConstraint, state, completeness, debug);
    }

    public DataEquiv<ITerm> getDataEquiv(State state, Completeness completeness, IDebugContext debug) {
        return new DataEquiv<ITerm>() {

            public boolean eq(List<ITerm> datum1, List<ITerm> datum2) throws ResolutionException, InterruptedException {
                final IConstraint constraint =
                        new CUser(dataConstraint, ImmutableList.of(B.newTuple(datum1), B.newTuple(datum2)));
                final Config config = Config.of(state, ImmutableList.of(constraint), completeness);
                return Solver.entails(config, debug).orElseThrow(() -> new ResolutionException("Data equivalence check delayed"));
            }

            public boolean alwaysTrue() {
                return false;
            }

        };
    }

    @Override public String toString(IUnifier unifier) {
        return toString();
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("min ");
        sb.append(pathConstraint);
        sb.append(" and ");
        sb.append(dataConstraint);
        return sb.toString();
    }

}