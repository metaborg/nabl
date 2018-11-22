package mb.statix.solver.query;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.Set;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.util.Tuple3;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Completeness;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.Solver;
import mb.statix.solver.State;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.Rule;

public class ConstraintDataWF implements DataWF<ITerm> {

    private final Rule constraint;
    private final State state;
    private final Completeness completeness;
    private final IDebugContext debug;

    public ConstraintDataWF(Rule constraint, State state, Completeness completeness, IDebugContext debug) {
        this.constraint = constraint;
        this.state = state;
        this.completeness = completeness;
        this.debug = debug;
    }

    public boolean wf(List<ITerm> datum) throws ResolutionException, InterruptedException {
        try {
            final Tuple3<State, Set<ITermVar>, Set<IConstraint>> result;
            if((result = constraint.apply(datum, state).orElse(null)) == null) {
                return false;
            }
            if(Solver.entails(result._1(), result._3(), completeness, result._2(), debug).isPresent()) {
                debug.info("Well-formed {}", state.unifier().toString(B.newTuple(datum)));
                return true;
            } else {
                debug.info("Not well-formed {}", state.unifier().toString(B.newTuple(datum)));
                return false;
            }
        } catch(Delay d) {
            throw new ResolutionDelayException("Data well-formedness delayed.", d);
        }
    }

}