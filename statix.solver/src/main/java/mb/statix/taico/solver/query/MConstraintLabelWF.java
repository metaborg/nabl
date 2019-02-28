package mb.statix.taico.solver.query;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.metaborg.util.functions.Predicate1;
import org.metaborg.util.log.Level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.IUnifier.Immutable.Result;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.util.Tuple3;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.query.ResolutionDelayException;
import mb.statix.spec.Rule;
import mb.statix.taico.solver.MCompleteness;
import mb.statix.taico.solver.MSolverResult;
import mb.statix.taico.solver.MState;
import mb.statix.taico.solver.ModuleSolver;

public class MConstraintLabelWF implements LabelWF<ITerm> {

    private final List<IConstraint> constraints;
    private final MState state;
    private final Set<ITermVar> rigidVars;
    private final Set<ITerm> closedScopes;
    private final MCompleteness completeness;
    private final IDebugContext debug;

    private final IListTerm labels;
    private final IListTerm tail;

    private MConstraintLabelWF(Collection<IConstraint> constraints, MState state, Set<ITermVar> rigidVars,
            Set<ITerm> closedScopes, MCompleteness completeness, IDebugContext debug, IListTerm labels, IListTerm tail) {
        this.constraints = ImmutableList.copyOf(constraints);
        this.state = state;
        this.rigidVars = rigidVars;
        this.closedScopes = closedScopes;
        this.completeness = completeness;
        this.debug = debug;
        this.labels = labels;
        this.tail = tail;
    }

    @Override public Optional<LabelWF<ITerm>> step(ITerm l) throws ResolutionException, InterruptedException {
        if(debug.isEnabled(Level.Info)) {
            debug.info("Try step {} after {}", state.unifier().toString(l), state.unifier().toString(labels));
        }
        MState newState = state.copy();
        final ITermVar newTail = newState.freshVar("lbls");
        final Result<IUnifier.Immutable> unifyResult;
        try {
            if((unifyResult = newState.unifier().unify(tail, B.newCons(l, newTail)).orElse(null)) == null) {
                throw new ResolutionException("Instantiation tail failed unexpectedly.");
            }
        } catch(OccursException e) {
            throw new ResolutionException("Instantiation tail failed unexpectedly.");
        }
        final IUnifier.Immutable newUnifier = unifyResult.unifier();
        newState.setUnifier(newUnifier);
        final Predicate1<ITermVar> isRigid = v -> rigidVars.contains(v) || newTail.equals(v);
        //TODO TAICO This might need to be done differently, as we cannot determine closed scopes any more?
        final Predicate1<ITerm> isClosed = s -> closedScopes.contains(s);
        
        //TODO IMPORTANT TAICO redirect this to the correct solvers?
        MCompleteness ncompleteness = completeness.copy();
        final MSolverResult result = ModuleSolver.solveSeparately(newState, constraints, ncompleteness, isRigid, isClosed, debug.subContext());
        if(result.hasErrors()) {
            if(debug.isEnabled(Level.Info)) {
                debug.info("Cannot step {} after {}", newUnifier.toString(l), newUnifier.toString(labels));
            }
            return Optional.empty();
        } else {
            final Delay d = result.delay();
            if(result.delays().isEmpty() || d.vars().equals(ImmutableSet.of(newTail))) { // stuck on the tail
                if(debug.isEnabled(Level.Info)) {
                    debug.info("Stepped {} after {}", newUnifier.toString(l), newUnifier.toString(labels));
                }
                final Set<IConstraint> newConstraints = result.delays().keySet();
                return Optional.of(new MConstraintLabelWF(newConstraints, newState, rigidVars, closedScopes,
                        ncompleteness, debug, labels, newTail));
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
        final MState newState = state.copy();
        newState.setUnifier(newUnifier);
        
        final Predicate1<ITermVar> isRigid = v -> rigidVars.contains(v);
        final Predicate1<ITerm> isClosed = s -> closedScopes.contains(s);
        //TODO Fix solver result
        final MSolverResult result =
                ModuleSolver.solveSeparately(newState, constraints, completeness.copy(), isRigid, isClosed, debug.subContext());
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

    public static MConstraintLabelWF of(Rule constraint, MState state, MCompleteness completeness, IDebugContext debug) {
        MState newState = state.copy();
        final ITermVar lbls = newState.freshVar("lbls");
        final Tuple3<MState, Set<ITermVar>, Set<IConstraint>> inst;
        try {
            if((inst = constraint.apply(ImmutableList.of(lbls), newState).orElse(null)) == null) {
                throw new IllegalArgumentException("Label well-formedness cannot be instantiated.");
            }
        } catch(Delay e) {
            throw new IllegalArgumentException("Label well-formedness cannot be instantiated.", e);
        }
        return new MConstraintLabelWF(inst._3(), inst._1(), state.vars(), state.scopes(), completeness, debug, lbls, lbls);
    }

}