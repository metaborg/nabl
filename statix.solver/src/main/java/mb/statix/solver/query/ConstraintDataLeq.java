package mb.statix.solver.query;

import java.util.List;
import java.util.Set;

import org.metaborg.util.log.Level;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.util.Tuple3;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.Solver;
import mb.statix.solver.State;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.Rule;

class ConstraintDataLeq implements DataLeq<ITerm> {

    private final Rule constraint;
    private final State state;
    private final IsComplete isComplete;
    private final IDebugContext debug;
    private volatile Boolean alwaysTrue;

    public ConstraintDataLeq(Rule constraint, State state, IsComplete isComplete, IDebugContext debug) {
        this.constraint = constraint;
        this.state = state;
        this.isComplete = isComplete;
        this.debug = debug;
    }

    @Override public boolean leq(ITerm datum1, ITerm datum2) throws ResolutionException, InterruptedException {
        try {
            final Tuple3<State, Set<ITermVar>, List<IConstraint>> result;
            if((result = constraint.apply(ImmutableList.of(datum1, datum2), state).orElse(null)) == null) {
                return false;
            }
            if(Solver.entails(result._1(), result._3(), isComplete, result._2(), debug).isPresent()) {
                if(debug.isEnabled(Level.Info)) {
                    debug.info("{} shadows {}", state.unifier().toString(datum1), state.unifier().toString(datum2));
                }
                return true;
            } else {
                if(debug.isEnabled(Level.Info)) {
                    debug.info("{} does not shadow {}", state.unifier().toString(datum1),
                            state.unifier().toString(datum2));
                }
                return false;
            }
        } catch(Delay d) {
            throw new ResolutionDelayException("Data order delayed.", d);
        }
    }

    @Override public boolean alwaysTrue() throws InterruptedException {
        if(alwaysTrue != null)
            return alwaysTrue.booleanValue();

        return alwaysTrue = constraint.isAlways(state.spec()).orElse(false);
    }

}