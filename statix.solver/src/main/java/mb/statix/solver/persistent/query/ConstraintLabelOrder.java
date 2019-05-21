package mb.statix.solver.persistent.query;

import org.metaborg.util.log.Level;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.persistent.State;
import mb.statix.solver.query.ResolutionDelayException;
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
        final IUnifier unifier = state.unifier();
        if(debug.isEnabled(Level.Info)) {
            debug.info("Check order {} < {}", unifier.toString(l1), state.unifier().toString(l2));
        }
        try {
            final IConstraint result;
            if((result = constraint.apply(ImmutableList.of(l1, l2), unifier).orElse(null)) == null) {
                return false;
            }
            if(Solver.entails(state, result, isComplete, debug.subContext()).isPresent()) {
                if(debug.isEnabled(Level.Info)) {
                    debug.info("Ordered {} < {}", unifier.toString(l1), unifier.toString(l2));
                }
                return true;
            } else {
                if(debug.isEnabled(Level.Info)) {
                    debug.info("Unordered {} < {}", unifier.toString(l1), unifier.toString(l2));
                }
                return false;
            }
        } catch(Delay d) {
            throw new ResolutionDelayException("Label order delayed.", d);
        }
    }

}