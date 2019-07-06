package mb.statix.taico.solver.query;

import org.metaborg.util.log.Level;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.Tuple2;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.query.ResolutionDelayException;
import mb.statix.spec.IRule;
import mb.statix.taico.solver.ModuleSolver;
import mb.statix.taico.solver.state.IMState;

public class MConstraintDataLeq implements DataLeq<ITerm> {

    private final IRule constraint;
    private final IMState state;
    private final IsComplete isComplete;
    private final IDebugContext debug;
    private volatile Boolean alwaysTrue;

    public MConstraintDataLeq(IRule constraint, IMState state, IsComplete isComplete, IDebugContext debug) {
        this.constraint = constraint;
        this.state = state;
        this.isComplete = isComplete;
        this.debug = debug;
    }

    @Override public boolean leq(ITerm datum1, ITerm datum2)
            throws ResolutionException, InterruptedException {
        final IUnifier unifier = state.unifier();
        try {
            final IConstraint result;
            if((result = constraint.apply(ImmutableList.of(datum1, datum2), unifier).map(Tuple2::_2).orElse(null)) == null) {
                return false;
            }
            IMState resultState = state.delegate();
            if(ModuleSolver.entails(resultState, result, isComplete, debug).isPresent()) {
                if(debug.isEnabled(Level.Info)) {
                    debug.info("{} shadows {}", unifier.toString(datum1), unifier.toString(datum2));
                }
                return true;
            } else {
                if(debug.isEnabled(Level.Info)) {
                    debug.info("{} does not shadow {}", unifier.toString(datum1), unifier.toString(datum2));
                }
                return false;
            }
        } catch(Delay d) {
            throw new ResolutionDelayException("Data order delayed.", d);
        }
    }

    @Override public boolean alwaysTrue() throws InterruptedException {
        if (alwaysTrue != null) return alwaysTrue;
        
        return alwaysTrue = constraint.isAlways(state.spec()).orElse(false);
    }

}