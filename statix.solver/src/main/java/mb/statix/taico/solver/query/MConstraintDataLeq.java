package mb.statix.taico.solver.query;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.Set;

import org.metaborg.util.log.Level;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.util.Tuple2;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.query.ResolutionDelayException;
import mb.statix.spec.IRule;
import mb.statix.taico.solver.ICompleteness;
import mb.statix.taico.solver.IMState;
import mb.statix.taico.solver.ModuleSolver;

public class MConstraintDataLeq implements DataLeq<ITerm> {

    private final IRule constraint;
    private final IMState state;
    private final ICompleteness isComplete;
    private final IDebugContext debug;
    private volatile Boolean alwaysTrue;

    public MConstraintDataLeq(IRule constraint, IMState state, ICompleteness isComplete, IDebugContext debug) {
        this.constraint = constraint;
        this.state = state;
        this.isComplete = isComplete;
        this.debug = debug;
    }

    @Override public boolean leq(List<ITerm> datum1, List<ITerm> datum2)
            throws ResolutionException, InterruptedException {
        final ITerm term1 = B.newTuple(datum1);
        final ITerm term2 = B.newTuple(datum2);
        try {
            IMState resultState = state.delegate();
            final Tuple2<Set<ITermVar>, Set<IConstraint>> result;
            if((result = constraint.apply(ImmutableList.of(term1, term2), resultState).orElse(null)) == null) {
                return false;
            }
            if(ModuleSolver.entails(resultState, result._2(), isComplete, result._1(), debug).isPresent()) {
                if(debug.isEnabled(Level.Info)) {
                    debug.info("{} shadows {}", resultState.unifier().toString(term1), resultState.unifier().toString(term2));
                }
                return true;
            } else {
                if(debug.isEnabled(Level.Info)) {
                    debug.info("{} does not shadow {}",
                            resultState.unifier().toString(term1),
                            resultState.unifier().toString(term2));
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