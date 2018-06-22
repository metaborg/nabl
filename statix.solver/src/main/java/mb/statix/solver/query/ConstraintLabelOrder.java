package mb.statix.solver.query;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.MatchException;
import mb.nabl2.terms.unification.UnificationException;
import mb.nabl2.util.Tuple2;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Completeness;
import mb.statix.solver.Config;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.Solver;
import mb.statix.solver.State;
import mb.statix.spec.Lambda;

public class ConstraintLabelOrder implements LabelOrder<ITerm> {

    private final Lambda constraint;
    private final State state;
    private final Completeness completeness;
    private final IDebugContext debug;

    public ConstraintLabelOrder(Lambda constraint, State state, Completeness completeness, IDebugContext debug) {
        this.constraint = constraint;
        this.state = state;
        this.completeness = completeness;
        this.debug = debug;
    }

    public boolean lt(ITerm l1, ITerm l2) throws ResolutionException, InterruptedException {
        debug.info("Check order {} < {}", state.unifier().toString(l1), state.unifier().toString(l2));
        try {
            final Tuple2<State, Lambda> result = constraint.apply(ImmutableList.of(l1, l2), state);
            final Config config = Config.of(result._1(), result._2().getBody(), completeness);
            if(Solver.entails(config, result._2().getBodyVars(), debug.subContext())
                    .orElseThrow(() -> new ResolutionException("Label order check delayed"))) {
                debug.info("Ordered {} < {}", state.unifier().toString(l1), state.unifier().toString(l2));
                return true;
            } else {
                debug.info("Unordered {} < {}", state.unifier().toString(l1), state.unifier().toString(l2));
                return false;
            }
        } catch(MatchException | UnificationException ex) {
            return false;
        }
    }

}