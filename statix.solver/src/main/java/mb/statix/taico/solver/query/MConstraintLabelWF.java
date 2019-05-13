package mb.statix.taico.solver.query;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.query.ResolutionDelayException;
import mb.statix.spec.IRule;
import mb.statix.taico.solver.ICompleteness;
import mb.statix.taico.solver.IMState;
import mb.statix.taico.solver.MSolverResult;
import mb.statix.taico.solver.ModuleSolver;
import mb.statix.taico.unifier.DistributedUnifier;

public class MConstraintLabelWF implements LabelWF<ITerm> {

    private final List<IConstraint> constraints;
    private final IMState state;
    private final ICompleteness isComplete;
    private final IDebugContext debug;

    private final IListTerm labels;
    private final IListTerm tail;

    private MConstraintLabelWF(Collection<IConstraint> constraints, IMState state,
            ICompleteness isComplete, IDebugContext debug, IListTerm labels, IListTerm tail) {
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
        IMState newState = state.delegate();
        final ITermVar tailVar = newState.freshRigidVar("lbls");
        final Result<IUnifier.Immutable> unifyResult;
        try {
            if((unifyResult = newState.unifier().unify(tail, B.newCons(l, tailVar)).orElse(null)) == null) {
                throw new ResolutionException("Instantiating tail failed unexpectedly.");
            }
        } catch(OccursException e) {
            throw new ResolutionException("Instantiating tail failed unexpectedly.");
        }
        final IUnifier.Immutable newUnifier = unifyResult.unifier();
        newState.setUnifier((DistributedUnifier.Immutable) newUnifier);
        final MSolverResult result = ModuleSolver.solveSeparately(newState, constraints, isComplete, debug.subContext());
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
                return Optional.of(new MConstraintLabelWF(newConstraints, newState, isComplete, debug, labels, tailVar));
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
        final DistributedUnifier.Immutable newUnifier = (DistributedUnifier.Immutable) unifyResult.unifier();
        final IMState newState = state.delegate();
        newState.setUnifier(newUnifier);
        final MSolverResult result = ModuleSolver.solveSeparately(newState, constraints, isComplete, debug.subContext());
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

    public static MConstraintLabelWF of(IRule constraint, IMState state, ICompleteness isComplete, IDebugContext debug) {
        // duplicate logic from entails, because we call solve directly in step()
        final IMState _state = state.delegate(new HashSet<>(), true);
        final ITermVar lbls = _state.freshVar("lbls");
        final Tuple2<Set<ITermVar>, Set<IConstraint>> inst;
        try {
            if((inst = constraint.apply(ImmutableList.of(lbls), state).orElse(null)) == null) {
                throw new IllegalArgumentException("Label well-formedness cannot be instantiated.");
            }
        } catch(Delay e) {
            throw new IllegalArgumentException("Label well-formedness cannot be instantiated.", e);
        }
        return new MConstraintLabelWF(inst._2(), _state, isComplete, debug, lbls, lbls);
    }

}