package mb.statix.taico.solver.query;

import java.util.Set;

import org.metaborg.util.log.Level;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.util.Tuple2;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.Solver;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.query.ResolutionDelayException;
import mb.statix.spec.IRule;
import mb.statix.taico.solver.ICompleteness;
import mb.statix.taico.solver.IMState;
import mb.statix.taico.solver.ModuleSolver;

/**
 * Class to represent a label order imposed in the form of a constraint (rule).
 */
public class MConstraintLabelOrder implements LabelOrder<ITerm> {

    private final IRule constraint;
    private final IMState state;
    private final ICompleteness isComplete;
    private final IDebugContext debug;

    public MConstraintLabelOrder(IRule constraint, IMState state, ICompleteness isComplete, IDebugContext debug) {
        this.constraint = constraint;
        this.state = state;
        this.isComplete = isComplete;
        this.debug = debug;
    }

    /**
     * @see LabelOrder#lt
     * 
     * @throws ResolutionDelayException
     *      If applying the labels to the constraint causes a delay.
     * @throws ResolutionDelayException
     *      If {@link Solver#entails(State, Iterable, Completeness, IDebugContext)} causes a delay.
     * @throws InterruptedException
     *      {@inheritDoc}
     */
    @Override
    public boolean lt(ITerm l1, ITerm l2) throws ResolutionDelayException, InterruptedException {
        if(debug.isEnabled(Level.Info)) {
            debug.info("Check order {} < {}", state.unifier().toString(l1), state.unifier().toString(l2));
        }
        try {
            IMState resultState = state.delegate();
            final Tuple2<Set<ITermVar>, Set<IConstraint>> result;
            if((result = constraint.apply(ImmutableList.of(l1, l2), resultState).orElse(null)) == null) {
                return false;
            }
            if(ModuleSolver.entails(resultState, result._2(), isComplete, result._1(), debug.subContext()).isPresent()) {
                if(debug.isEnabled(Level.Info)) {
                    debug.info("Ordered {} < {}", resultState.unifier().toString(l1), resultState.unifier().toString(l2));
                }
                return true;
            } else {
                if(debug.isEnabled(Level.Info)) {
                    debug.info("Unordered {} < {}", resultState.unifier().toString(l1), resultState.unifier().toString(l2));
                }
                return false;
            }
        } catch(Delay d) {
            throw new ResolutionDelayException("Label order delayed.", d);
        }
    }

}