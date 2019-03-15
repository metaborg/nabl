package mb.statix.solver.query;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.Set;

import org.metaborg.util.log.Level;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.util.Tuple3;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Completeness;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.Solver;
import mb.statix.solver.State;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.Rule;

public class ConstraintDataLeq implements DataLeq<ITerm> {

    private final Rule constraint;
    private final State state;
    private final Completeness completeness;
    private final IDebugContext debug;
    private volatile Boolean alwaysTrue;

    public ConstraintDataLeq(Rule constraint, State state, Completeness completeness, IDebugContext debug) {
        this.constraint = constraint;
        this.state = state;
        this.completeness = completeness;
        this.debug = debug;
    }

    @Override public boolean leq(List<ITerm> datum1, List<ITerm> datum2)
            throws ResolutionException, InterruptedException {
        final ITerm term1 = B.newTuple(datum1);
        final ITerm term2 = B.newTuple(datum2);
        try {
            final Tuple3<State, Set<ITermVar>, Set<IConstraint>> result;
            if((result = constraint.apply(ImmutableList.of(term1, term2), state).orElse(null)) == null) {
                return false;
            }
            if(Solver.entails(result._1(), result._3(), completeness, result._2(), debug).isPresent()) {
                if(debug.isEnabled(Level.Info)) {
                    debug.info("{} shadows {}", state.unifier().toString(term1), state.unifier().toString(term2));
                }
                return true;
            } else {
                if(debug.isEnabled(Level.Info)) {
                    debug.info("{} does not shadow {}", state.unifier().toString(term1),
                            state.unifier().toString(term2));
                }
                return false;
            }
        } catch(Delay d) {
            throw new ResolutionDelayException("Data order delayed.", d);
        }
    }

    @Override public boolean alwaysTrue() throws InterruptedException {
        if (alwaysTrue != null) return alwaysTrue.booleanValue();
        
        return alwaysTrue = constraint.isAlways(state.spec()).orElse(false);
    }

}