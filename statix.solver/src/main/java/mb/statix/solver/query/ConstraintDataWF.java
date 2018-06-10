package mb.statix.solver.query;

import java.util.List;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Completeness;
import mb.statix.solver.Config;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.Solver;
import mb.statix.solver.State;
import mb.statix.solver.constraint.CUser;

public class ConstraintDataWF implements DataWF<ITerm> {

    private final String constraint;
    private final State state;
    private final Completeness completeness;
    private final IDebugContext debug;

    public ConstraintDataWF(String constraint, State state, Completeness completeness, IDebugContext debug) {
        this.constraint = constraint;
        this.state = state;
        this.completeness = completeness;
        this.debug = debug;
    }

    public boolean wf(List<ITerm> datum) throws ResolutionException, InterruptedException {
        final IConstraint C = new CUser(constraint, datum);
        final Config config = Config.of(state, ImmutableList.of(C), completeness);
        return Solver.entails(config, debug)
                .orElseThrow(() -> new ResolutionException("Data well-formedness check delayed"));
    }

}