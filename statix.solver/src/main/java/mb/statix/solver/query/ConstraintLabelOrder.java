package mb.statix.solver.query;

import java.util.Set;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.MismatchException;
import mb.nabl2.util.Tuple3;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Completeness;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.Solver;
import mb.statix.solver.State;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.Rule;

public class ConstraintLabelOrder implements LabelOrder<ITerm> {

    private final Rule constraint;
    private final State state;
    private final Completeness completeness;
    private final IDebugContext debug;

    public ConstraintLabelOrder(Rule constraint, State state, Completeness completeness, IDebugContext debug) {
        this.constraint = constraint;
        this.state = state;
        this.completeness = completeness;
        this.debug = debug;
    }

    public boolean lt(ITerm l1, ITerm l2) throws ResolutionException, InterruptedException {
        debug.info("Check order {} < {}", state.unifier().toString(l1), state.unifier().toString(l2));
        try {
            final Tuple3<State, Set<ITermVar>, Set<IConstraint>> result =
                    constraint.apply(ImmutableList.of(l1, l2), state);
            if(Solver.entails(result._1(), result._3(), completeness, result._2(), debug.subContext()).isPresent()) {
                debug.info("Ordered {} < {}", state.unifier().toString(l1), state.unifier().toString(l2));
                return true;
            } else {
                debug.info("Unordered {} < {}", state.unifier().toString(l1), state.unifier().toString(l2));
                return false;
            }
        } catch(Delay d) {
            throw new ResolutionDelayException("Label order delayed.", d);
        } catch(MismatchException ex) {
            return false;
        }
    }

}