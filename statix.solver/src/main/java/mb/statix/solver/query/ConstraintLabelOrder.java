package mb.statix.solver.query;

import java.util.List;
import java.util.Set;

import org.metaborg.util.log.Level;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.util.Tuple3;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.Solver;
import mb.statix.solver.State;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.Rule;

class ConstraintLabelOrder implements LabelOrder<ITerm> {

    private final Rule constraint;
    private final State state;
    private final IsComplete isComplete;
    private final IDebugContext debug;

    public ConstraintLabelOrder(Rule constraint, State state, IsComplete isComplete, IDebugContext debug) {
        this.constraint = constraint;
        this.state = state;
        this.isComplete = isComplete;
        this.debug = debug;
    }

    @Override public boolean lt(ITerm l1, ITerm l2) throws ResolutionException, InterruptedException {
        if(debug.isEnabled(Level.Info)) {
            debug.info("Check order {} < {}", state.unifier().toString(l1), state.unifier().toString(l2));
        }
        try {
            final Tuple3<State, Set<ITermVar>, List<IConstraint>> result;
            if((result = constraint.apply(ImmutableList.of(l1, l2), state).orElse(null)) == null) {
                return false;
            }
            if(Solver.entails(result._1(), result._3(), isComplete, result._2(), debug.subContext()).isPresent()) {
                if(debug.isEnabled(Level.Info)) {
                    debug.info("Ordered {} < {}", state.unifier().toString(l1), state.unifier().toString(l2));
                }
                return true;
            } else {
                if(debug.isEnabled(Level.Info)) {
                    debug.info("Unordered {} < {}", state.unifier().toString(l1), state.unifier().toString(l2));
                }
                return false;
            }
        } catch(Delay d) {
            throw new ResolutionDelayException("Label order delayed.", d);
        }
    }

}