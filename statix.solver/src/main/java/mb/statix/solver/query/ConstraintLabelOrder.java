package mb.statix.solver.query;

import java.util.List;
import java.util.Set;

import org.metaborg.util.Ref;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.log.Level;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.Tuple2;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.Solver;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.State;
import mb.statix.spec.Rule;

public class ConstraintLabelOrder implements LabelOrder<ITerm> {

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
        final Ref<State> state = new Ref<>(this.state);
        final Function1<String, ITermVar> freshVar = (base) -> {
            final Tuple2<ITermVar, State> stateAndVar = state.get().freshVar(base);
            state.set(stateAndVar._2());
            return stateAndVar._1();
        };
        try {
            final Tuple2<Set<ITermVar>, List<IConstraint>> result;
            if((result = constraint.apply(ImmutableList.of(l1, l2), unifier, freshVar).orElse(null)) == null) {
                return false;
            }
            if(Solver.entails(state.get(), result._2(), isComplete, result._1(), debug.subContext()).isPresent()) {
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