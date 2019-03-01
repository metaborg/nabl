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
import mb.nabl2.util.Tuple2;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.query.ResolutionDelayException;
import mb.statix.spec.Rule;
import mb.statix.taico.module.IModule;
import mb.statix.taico.scopegraph.OwnableScope;
import mb.statix.taico.solver.MCompleteness;
import mb.statix.taico.solver.MSolverResult;
import mb.statix.taico.solver.MState;
import mb.statix.taico.solver.ModuleSolver;
import mb.statix.taico.util.IOwnable;

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
        final Predicate1<ITermVar> isRigid = v -> {
            if (rigidVars.contains(v) || newTail.equals(v)) return true;
            
            IModule owner = state.manager().getModule(v.getResource());
            System.err.println("[2] isRigid in MConstraintLabelWF matcher with new owner " + owner);
            return owner != state.owner();
        };
        //TODO TAICO This might need to be done differently, as we cannot determine closed scopes any more?
        final Predicate1<ITerm> isClosed = s -> {
            if (closedScopes.contains(s)) return true;
            
            IModule owner;
            if (s instanceof IOwnable) {
                System.err.println("isClosed in MConstraintLabelWF, s is ownable");
                owner = ((IOwnable) s).getOwner();
            } else {
                System.err.println("isClosed in MConstraintLabelWF, s is NOT ownable");
                OwnableScope scope = OwnableScope.ownableMatcher(state.manager()::getModule).match(s, state.unifier()).orElse(null);
                if (scope == null) {
                    System.err.println("Unable to convert scope term to scope in isClosed predicate in MConstraintLabelWF");
                    return false;
                }
                owner = scope.getOwner();
            }
            return owner != state.owner();
        };
        
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
        
        final Predicate1<ITermVar> isRigid = v -> {
            if (rigidVars.contains(v)) return true;
            
            IModule owner = state.manager().getModule(v.getResource());
            System.err.println("[3] isRigid in MConstraintLabelWF (accepting) matcher with new owner " + owner);
            return owner != state.owner();
        };
        //TODO TAICO This might need to be done differently, as we cannot determine closed scopes any more?
        final Predicate1<ITerm> isClosed = s -> {
            if (closedScopes.contains(s)) return true;
            
            IModule owner;
            if (s instanceof IOwnable) {
                System.err.println("isClosed in MConstraintLabelWF (accepting), s is ownable");
                owner = ((IOwnable) s).getOwner();
            } else {
                System.err.println("isClosed in MConstraintLabelWF (accepting), s is NOT ownable");
                OwnableScope scope = OwnableScope.ownableMatcher(state.manager()::getModule).match(s, state.unifier()).orElse(null);
                if (scope == null) {
                    System.err.println("Unable to convert scope term to scope in isClosed predicate in MConstraintLabelWF (accepting)");
                    return false;
                }
                owner = scope.getOwner();
            }
            return owner != state.owner();
        };
        
//        final Predicate1<ITermVar> isRigid = v -> rigidVars.contains(v);
//        final Predicate1<ITerm> isClosed = s -> closedScopes.contains(s);
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
        final Tuple2<Set<ITermVar>, Set<IConstraint>> inst;
        try {
            if((inst = constraint.apply(ImmutableList.of(lbls), newState).orElse(null)) == null) {
                throw new IllegalArgumentException("Label well-formedness cannot be instantiated.");
            }
        } catch(Delay e) {
            throw new IllegalArgumentException("Label well-formedness cannot be instantiated.", e);
        }
        return new MConstraintLabelWF(inst._2(), newState, state.vars(), state.scopes(), completeness, debug, lbls, lbls);
    }

}