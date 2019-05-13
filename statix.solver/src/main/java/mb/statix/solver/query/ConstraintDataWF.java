package mb.statix.solver.query;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.Set;

import org.metaborg.util.log.Level;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.util.Tuple3;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.Solver;
import mb.statix.solver.State;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.Rule;

class ConstraintDataWF implements DataWF<ITerm> {

    private final Rule constraint;
    private final State state;
    private final IsComplete isComplete;
    private final IDebugContext debug;

    public ConstraintDataWF(Rule constraint, State state, IsComplete isComplete, IDebugContext debug) {
        this.constraint = constraint;
        this.state = state;
        this.isComplete = isComplete;
        this.debug = debug;
    }

    @Override public boolean wf(ITerm datum) throws ResolutionException, InterruptedException {
        try {
            final Tuple3<State, Set<ITermVar>, List<IConstraint>> result;
            if((result = constraint.apply(ImmutableList.of(datum), state).orElse(null)) == null) {
                return false;
            }
            if(Solver.entails(result._1(), result._3(), isComplete, result._2(), debug).isPresent()) {
                if(debug.isEnabled(Level.Info)) {
                    debug.info("Well-formed {}", state.unifier().toString(B.newTuple(datum)));
                }
                return true;
            } else {
                if(debug.isEnabled(Level.Info)) {
                    debug.info("Not well-formed {}", state.unifier().toString(B.newTuple(datum)));
                }
                return false;
            }
        } catch(Delay d) {
            throw new ResolutionDelayException("Data well-formedness delayed.", d);
        }
    }

}