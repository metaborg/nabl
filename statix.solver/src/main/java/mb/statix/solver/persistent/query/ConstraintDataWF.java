package mb.statix.solver.persistent.query;

import static mb.nabl2.terms.build.TermBuild.B;

import org.metaborg.util.log.Level;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.query.ResolutionDelayException;
import mb.statix.spec.Rule;

class ConstraintDataWF implements DataWF<ITerm> {

    private final Rule constraint;
    private final IState.Immutable state;
    private final IsComplete isComplete;
    private final IDebugContext debug;

    public ConstraintDataWF(Rule constraint, IState.Immutable state, IsComplete isComplete, IDebugContext debug) {
        this.constraint = constraint;
        this.state = state;
        this.isComplete = isComplete;
        this.debug = debug;
    }

    @Override public boolean wf(ITerm datum) throws ResolutionException, InterruptedException {
        final IUnifier.Immutable unifier = state.unifier();
        try {
            final IConstraint result;
            if((result = constraint.apply(ImmutableList.of(datum), unifier).orElse(null)) == null) {
                return false;
            }
            if(Solver.entails(state, result, isComplete, debug)) {
                if(debug.isEnabled(Level.Info)) {
                    debug.info("Well-formed {}", unifier.toString(B.newTuple(datum)));
                }
                return true;
            } else {
                if(debug.isEnabled(Level.Info)) {
                    debug.info("Not well-formed {}", unifier.toString(B.newTuple(datum)));
                }
                return false;
            }
        } catch(Delay d) {
            throw new ResolutionDelayException("Data well-formedness delayed.", d);
        }
    }

}
