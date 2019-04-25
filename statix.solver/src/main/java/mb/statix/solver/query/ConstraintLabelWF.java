package mb.statix.solver.query;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.metaborg.util.Ref;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.log.Level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.IUnifier.Immutable.Result;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.util.Tuple2;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.Solver;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.SolverResult;
import mb.statix.solver.State;
import mb.statix.spec.Rule;

public class ConstraintLabelWF implements LabelWF<ITerm> {

    private final List<IConstraint> constraints;
    private final State state;
    private final IsComplete isComplete;
    private final IDebugContext debug;

    private final IListTerm labels;
    private final IListTerm tail;

    private ConstraintLabelWF(Collection<IConstraint> constraints, State state, IsComplete isComplete,
            IDebugContext debug, IListTerm labels, IListTerm tail) {
        this.constraints = ImmutableList.copyOf(constraints);
        this.state = state;
        this.isComplete = isComplete;
        this.debug = debug;
        this.labels = labels;
        this.tail = tail;
    }

    @Override public Optional<LabelWF<ITerm>> step(ITerm l) throws ResolutionException, InterruptedException {
        if(debug.isEnabled(Level.Info)) {
            debug.info("Try step {} after {}", state.unifier().toString(l), state.unifier().toString(labels));
        }
        final Tuple2<ITermVar, State> newTail = state.freshRigidVar("lbls");
        final Result<IUnifier.Immutable> unifyResult;
        final ITermVar tailVar = newTail._1();
        try {
            if((unifyResult = newTail._2().unifier().unify(tail, B.newCons(l, tailVar)).orElse(null)) == null) {
                throw new ResolutionException("Instantiating tail failed unexpectedly.");
            }
        } catch(OccursException e) {
            throw new ResolutionException("Instantiating tail failed unexpectedly.");
        }
        final IUnifier.Immutable newUnifier = unifyResult.unifier();
        State newState = newTail._2().withUnifier(newUnifier);
        final SolverResult result = Solver.solve(newState, constraints, isComplete, debug.subContext());
        if(result.hasErrors()) {
            if(debug.isEnabled(Level.Info)) {
                debug.info("Cannot step {} after {}", newUnifier.toString(l), newUnifier.toString(labels));
            }
            return Optional.empty();
        } else {
            final Delay d = result.delay();
            if(result.delays().isEmpty() || d.vars().equals(ImmutableSet.of(tailVar))) { // stuck on the tail
                if(debug.isEnabled(Level.Info)) {
                    debug.info("Stepped {} after {}", newUnifier.toString(l), newUnifier.toString(labels));
                }
                final Set<IConstraint> newConstraints = result.delays().keySet();
                return Optional
                        .of(new ConstraintLabelWF(newConstraints, result.state(), isComplete, debug, labels, tailVar));
            } else { // stuck on the context
                if(debug.isEnabled(Level.Info)) {
                    debug.info("Stepping {} after {} delayed", newUnifier.toString(l), newUnifier.toString(labels));
                }
                throw new ResolutionDelayException("Well-formedness step delayed.", d); // FIXME Remove local vars and scopes
            }
        }
    }

    @Override public boolean accepting() throws ResolutionException, InterruptedException {
        if(debug.isEnabled(Level.Info)) {
            debug.info("Check well-formedness of {}", state.unifier().toString(labels));
        }
        final Result<IUnifier.Immutable> unifyResult;
        try {
            if((unifyResult = state.unifier().unify(tail, B.newNil()).orElse(null)) == null) {
                throw new ResolutionException("Instantiation tail failed unexpectedly.");
            }
        } catch(OccursException e) {
            throw new ResolutionException("Instantiation tail failed unexpectedly.");
        }
        final IUnifier.Immutable newUnifier = unifyResult.unifier();
        final State newState = state.withUnifier(newUnifier);
        final SolverResult result = Solver.solve(newState, constraints, isComplete, debug.subContext());
        if(result.hasErrors()) {
            if(debug.isEnabled(Level.Info)) {
                debug.info("Not well-formed {}", newUnifier.toString(labels));
            }
            return false;
        } else if(result.delays().isEmpty()) {
            if(debug.isEnabled(Level.Info)) {
                debug.info("Well-formed {}", newUnifier.toString(labels));
            }
            return true;
        } else {
            throw new ResolutionDelayException("Label well-formedness delayed.", result.delay()); // FIXME Remove local vars and scopes
        }
    }

    public static ConstraintLabelWF of(Rule constraint, State state, IsComplete isComplete, IDebugContext debug) {
        // duplicate logic from entails, because we call solve directly in step()
        final Ref<State> _state = new Ref<>(state.clearVarsAndScopes());
        final Function1<String, ITermVar> freshVar = (base) -> {
            final Tuple2<ITermVar, State> stateAndVar = _state.get().freshVar(base);
            _state.set(stateAndVar._2());
            return stateAndVar._1();
        };
        final ITermVar lbls = freshVar.apply("lbls");
        final Tuple2<Set<ITermVar>, List<IConstraint>> inst;
        try {
            if((inst = constraint.apply(ImmutableList.of(lbls), state.unifier(), freshVar).orElse(null)) == null) {
                throw new IllegalArgumentException("Label well-formedness cannot be instantiated.");
            }
        } catch(Delay e) {
            throw new IllegalArgumentException("Label well-formedness cannot be instantiated.", e);
        }
        return new ConstraintLabelWF(inst._2(), _state.get(), isComplete, debug, lbls, lbls);
    }

}