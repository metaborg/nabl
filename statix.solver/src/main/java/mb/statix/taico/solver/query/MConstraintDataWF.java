package mb.statix.taico.solver.query;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.Set;

import org.metaborg.util.log.Level;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.util.Tuple2;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.query.ResolutionDelayException;
import mb.statix.spec.IRule;
import mb.statix.taico.solver.ICompleteness;
import mb.statix.taico.solver.IMState;
import mb.statix.taico.solver.ModuleSolver;

public class MConstraintDataWF implements DataWF<ITerm> {

    private final IRule constraint;
    private final IMState state;
    private final ICompleteness isComplete;
    private final IDebugContext debug;

    public MConstraintDataWF(IRule constraint, IMState state, ICompleteness isComplete, IDebugContext debug) {
        this.constraint = constraint;
        this.state = state;
        this.isComplete = isComplete;
        this.debug = debug;
    }

    public boolean wf(List<ITerm> datum) throws ResolutionException, InterruptedException {
        //TODO IMPORTANT For the separate solvers/entails solvers, the completeness is sometimes obtained via modules, which will not be the completeness in the solver in question.
        try {
            IMState resultState = state.delegate();
            final Tuple2<Set<ITermVar>, Set<IConstraint>> result;
            if((result = constraint.apply(datum, resultState).orElse(null)) == null) {
                return false;
            }
            if(ModuleSolver.entails(resultState, result._2(), isComplete, result._1(), debug).isPresent()) {
                if(debug.isEnabled(Level.Info)) {
                    debug.info("Well-formed {}", resultState.unifier().toString(B.newTuple(datum)));
                }
                return true;
            } else {
                if(debug.isEnabled(Level.Info)) {
                    debug.info("Not well-formed {}", resultState.unifier().toString(B.newTuple(datum)));
                }
                return false;
            }
        } catch(Delay d) {
            throw new ResolutionDelayException("Data well-formedness delayed.", d);
        }
    }

}