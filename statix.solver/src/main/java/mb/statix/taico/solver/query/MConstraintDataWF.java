package mb.statix.taico.solver.query;

import static mb.nabl2.terms.build.TermBuild.B;

import java.io.Serializable;

import org.metaborg.util.log.Level;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.Tuple2;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.query.ResolutionDelayException;
import mb.statix.spec.IRule;
import mb.statix.taico.solver.IMState;
import mb.statix.taico.solver.ModuleSolver;
import mb.statix.taico.util.TDebug;

public class MConstraintDataWF implements DataWF<ITerm>, Serializable {
    private static final long serialVersionUID = 1L;
    
    private final IRule constraint;
    private final IMState state;
    private final IsComplete isComplete;
    private transient IDebugContext debug;

    public MConstraintDataWF(IRule constraint, IMState state, IsComplete isComplete, IDebugContext debug) {
        this.constraint = constraint;
        this.state = state;
        this.isComplete = isComplete;
        this.debug = debug;
    }

    @Override public boolean wf(ITerm datum) throws ResolutionException, InterruptedException {
        final IUnifier unifier = state.unifier();
        try {
            final IConstraint result;
            if((result = constraint.apply(ImmutableList.of(datum), unifier).map(Tuple2::_2).orElse(null)) == null) {
                return false;
            }
            IMState resultState = state.delegate();
            if(ModuleSolver.entails(resultState, result, isComplete, debug).isPresent()) {
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
    
    //TODO Temporary workaround
    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
        System.err.println("Serializing unserializable DataWF!");
        out.defaultWriteObject();
    }
    
    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        debug = TDebug.DEV_OUT;
    }
    
}