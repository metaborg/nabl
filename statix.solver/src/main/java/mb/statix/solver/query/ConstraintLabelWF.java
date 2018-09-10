package mb.statix.solver.query;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.metaborg.util.functions.Predicate1;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.MatchException;
import mb.nabl2.terms.unification.CannotUnifyException;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.util.Tuple2;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Completeness;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.Solver;
import mb.statix.solver.SolverResult;
import mb.statix.solver.State;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.Lambda;

public class ConstraintLabelWF implements LabelWF<ITerm> {

    private final List<IConstraint> constraints;
    private final State state;
    private final Set<ITermVar> rigidVars;
    private final Set<ITerm> closedScopes;
    private final Completeness completeness;
    private final IDebugContext debug;

    private final IListTerm labels;
    private final IListTerm tail;

    private ConstraintLabelWF(Collection<IConstraint> constraints, State state, Set<ITermVar> rigidVars,
            Set<ITerm> closedScopes, Completeness completeness, IDebugContext debug, IListTerm labels, IListTerm tail) {
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
        debug.info("Try step {} after {}", state.unifier().toString(l), state.unifier().toString(labels));
        final Tuple2<ITermVar, State> newTail = state.freshVar("lbls");
        IUnifier.Immutable newUnifier;
        try {
            newUnifier = newTail._2().unifier().unify(tail, B.newCons(l, newTail._1())).unifier();
        } catch(CannotUnifyException | OccursException e) {
            throw new ResolutionException("Intantiation tail failed unexpectedly.");
        }
        final State newState = newTail._2().withUnifier(newUnifier);
        final Predicate1<ITermVar> isRigid = v -> rigidVars.contains(v) || newTail._1().equals(v);
        final Predicate1<ITerm> isClosed = s -> closedScopes.contains(s);
        final SolverResult result =
                Solver.solve(newState, constraints, completeness, isRigid, isClosed, debug.subContext());
        if(result.hasErrors()) {
            debug.info("Cannot step {} after {}", newUnifier.toString(l), newUnifier.toString(labels));
            return Optional.empty();
        } else {
            final Delay d = result.delay();
            if(result.delays().isEmpty() || d.vars().equals(ImmutableSet.of(newTail._1()))) { // stuck on the tail
                debug.info("Stepped {} after {}", newUnifier.toString(l), newUnifier.toString(labels));
                final Set<IConstraint> newConstraints = result.delays().keySet();
                return Optional.of(new ConstraintLabelWF(newConstraints, result.state(), rigidVars, closedScopes,
                        result.completeness(), debug, labels, newTail._1()));
            } else { // stuck on the context
                debug.info("Stepping {} after {} delayed", newUnifier.toString(l), newUnifier.toString(labels));
                throw new ResolutionDelayException("Well-formedness step delayed.", d); // FIXME Remove local vars and scopes
            }
        }
    }

    @Override public boolean accepting() throws ResolutionException, InterruptedException {
        debug.info("Check well-formedness of {}", state.unifier().toString(labels));
        IUnifier.Immutable newUnifier;
        try {
            newUnifier = state.unifier().unify(tail, B.newNil()).unifier();
        } catch(CannotUnifyException | OccursException e) {
            throw new ResolutionException("Intantiation tail failed unexpectedly.");
        }
        final State newState = state.withUnifier(newUnifier);
        final Predicate1<ITermVar> isRigid = v -> rigidVars.contains(v);
        final Predicate1<ITerm> isClosed = s -> closedScopes.contains(s);
        final SolverResult result =
                Solver.solve(newState, constraints, completeness, isRigid, isClosed, debug.subContext());
        if(result.hasErrors()) {
            debug.info("Not well-formed {}", newUnifier.toString(labels));
            return false;
        } else if(result.delays().isEmpty()) {
            debug.info("Well-formed {}", newUnifier.toString(labels));
            return true;
        } else {
            throw new ResolutionDelayException("Label well-formedness delayed.", result.delay()); // FIXME Remove local vars and scopes
        }
    }

    public static ConstraintLabelWF of(Lambda constraint, State state, Completeness completeness, IDebugContext debug) {
        final Tuple2<ITermVar, State> lbls = state.freshVar("lbls");
        final Tuple2<State, Lambda> inst;
        try {
            inst = constraint.apply(ImmutableList.of(lbls._1()), lbls._2());
        } catch(MatchException | CannotUnifyException e) {
            throw new IllegalArgumentException("Label well-formedness cannot be instantiated.", e);
        }
        return new ConstraintLabelWF(inst._2().body(), inst._1(), state.vars(), state.scopes(), completeness, debug,
                lbls._1(), lbls._1());
    }

}