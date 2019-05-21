package mb.statix.solver.persistent.query;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.Optional;

import org.metaborg.util.log.Level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.IUnifier.Immutable.Result;
import mb.nabl2.terms.unification.OccursException;
import mb.statix.constraints.CConj;
import mb.statix.constraints.CEqual;
import mb.statix.constraints.CExists;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.solver.persistent.State;
import mb.statix.solver.query.ResolutionDelayException;
import mb.statix.spec.Rule;

class ConstraintLabelWF implements LabelWF<ITerm> {

    private final IConstraint constraint;
    private final State state;
    private final IsComplete isComplete;
    private final IDebugContext debug;

    private final IListTerm labels;
    private final IListTerm tail;

    private ConstraintLabelWF(IConstraint constraint, State state, IsComplete isComplete, IDebugContext debug,
            IListTerm labels, IListTerm tail) {
        this.constraint = constraint;
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
        ITermVar tailVar = B.newVar("", "lbls");
        final IConstraint c =
                new CExists(ImmutableSet.of(tailVar), new CConj(new CEqual(tail, B.newCons(l, tailVar)), constraint));
        final SolverResult result = Solver.solve(state, c, isComplete, debug.subContext());
        tailVar = result.existentials().get(tailVar);
        if(result.hasErrors()) {
            if(debug.isEnabled(Level.Info)) {
                debug.info("Cannot step {} after {}", state.unifier().toString(l), state.unifier().toString(labels));
            }
            return Optional.empty();
        } else {
            final Delay d = result.delay();
            if(result.delays().isEmpty() || d.vars().equals(ImmutableSet.of(tailVar))) { // stuck on the tail
                if(debug.isEnabled(Level.Info)) {
                    debug.info("Stepped {} after {}", state.unifier().toString(l), state.unifier().toString(labels));
                }
                final IConstraint newConstraint = result.delayed();
                return Optional
                        .of(new ConstraintLabelWF(newConstraint, result.state(), isComplete, debug, labels, tailVar));
            } else { // stuck on the context
                if(debug.isEnabled(Level.Info)) {
                    debug.info("Stepping {} after {} delayed", state.unifier().toString(l),
                            state.unifier().toString(labels));
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


        final SolverResult result = Solver.solve(newState, constraint, isComplete, debug.subContext());
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

    public static ConstraintLabelWF of(Rule constraint, State state, IsComplete isComplete, IDebugContext debug)
            throws InterruptedException {
        // duplicate logic from entails, because we call solve directly in step()
        ITermVar var = B.newVar("", "lbls");
        final IConstraint inst;
        try {
            if((inst = constraint.apply(ImmutableList.of(var), state.unifier()).orElse(null)) == null) {
                throw new IllegalArgumentException("Label well-formedness cannot be instantiated.");
            }
        } catch(Delay e) {
            throw new IllegalArgumentException("Label well-formedness cannot be instantiated.", e);
        }
        final IConstraint c = new CExists(ImmutableSet.of(var), inst);
        final SolverResult result = Solver.solve(state.clearVarsAndScopes(), c, isComplete, debug.subContext());
        state = result.state();
        var = result.existentials().get(var);
        return new ConstraintLabelWF(result.delayed(), state, isComplete, debug, var, var);
    }

}