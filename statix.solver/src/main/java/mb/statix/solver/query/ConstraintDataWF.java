package mb.statix.solver.query;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.Set;

import org.metaborg.util.functions.Predicate3;
import org.metaborg.util.log.Level;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.util.Tuple3;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.Solver;
import mb.statix.solver.State;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.IRule;

public class ConstraintDataWF implements DataWF<ITerm> {

    private final IRule constraint;
    private final State state;
    private final Predicate3<ITerm, ITerm, State> isComplete;
    private final IDebugContext debug;

    public ConstraintDataWF(IRule constraint, State state, Predicate3<ITerm, ITerm, State> isComplete, IDebugContext debug) {
        this.constraint = constraint;
        this.state = state;
        this.isComplete = isComplete;
        this.debug = debug;
    }

    @Override public boolean wf(List<ITerm> datum) throws ResolutionException, InterruptedException {
        try {
            final Tuple3<State, Set<ITermVar>, Set<IConstraint>> result;
            if((result = constraint.apply(datum, state).orElse(null)) == null) {
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