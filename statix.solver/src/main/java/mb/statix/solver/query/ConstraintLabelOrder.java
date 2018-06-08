package mb.statix.solver.query;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Completeness;
import mb.statix.solver.Config;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.Solver;
import mb.statix.solver.State;
import mb.statix.solver.constraint.CUser;

public class ConstraintLabelOrder implements LabelOrder<ITerm> {

    private final String constraint;
    private final State state;
    private final Completeness completeness;
    private final IDebugContext debug;

    public ConstraintLabelOrder(String constraint, State state, Completeness completeness, IDebugContext debug) {
        this.constraint = constraint;
        this.state = state;
        this.completeness = completeness;
        this.debug = debug;
    }

    public boolean lt(ITerm l1, ITerm l2) throws ResolutionException, InterruptedException {
        debug.info("Check {} < {}", state.unifier().toString(l1), state.unifier().toString(l2));
        final IConstraint C = new CUser(constraint, ImmutableList.of(l1, l2));
        final Config config = Config.of(state, ImmutableList.of(C), completeness);
        return Solver.entails(config, debug.subContext()).orElseThrow(() -> new ResolutionException());
    }

}