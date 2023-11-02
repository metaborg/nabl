package mb.statix.spec;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.ImList;
import org.metaborg.util.functions.Action1;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.Predicate1;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.tuple.Tuple2;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.FreshVars;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.PersistentSubstitution;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.RigidException;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.nabl2.terms.unification.ud.PersistentUniDisunifier;
import mb.nabl2.util.TermFormatter;
import mb.statix.constraints.CExists;
import mb.statix.constraints.CFalse;
import mb.statix.constraints.Constraints;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.StateUtil;
import mb.statix.solver.completeness.Completeness;
import mb.statix.solver.completeness.ICompleteness;

public class PreSolvedConstraint implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Set.Immutable<ITermVar> vars;
    private final IUniDisunifier.Immutable unifier;
    private final List<IConstraint> constraints;

    private final @Nullable IConstraint cause;
    private final @Nullable ICompleteness.Immutable bodyCriticalEdges;

    private volatile Set.Immutable<ITermVar> freeVars;

    private PreSolvedConstraint(Iterable<ITermVar> vars, IUniDisunifier.Immutable unifier,
            List<IConstraint> constraints, @Nullable IConstraint cause,
            @Nullable ICompleteness.Immutable bodyCriticalEdges, @Nullable Set.Immutable<ITermVar> freeVars) {
        this.vars = CapsuleUtil.toSet(vars);
        this.unifier = unifier;
        this.constraints = constraints;
        this.cause = cause;
        this.bodyCriticalEdges = bodyCriticalEdges;
        this.freeVars = freeVars;
    }


    public Set.Immutable<ITermVar> vars() {
        return vars;
    }

    public IUniDisunifier.Immutable unifier() {
        return unifier;
    }

    public List<IConstraint> constraints() {
        return constraints;
    }


    public Set.Immutable<ITermVar> freeVars() {
        Set.Immutable<ITermVar> result = freeVars;
        if(result == null) {
            Set.Transient<ITermVar> _freeVars = CapsuleUtil.transientSet();
            doVisitFreeVars(_freeVars::__insert);
            result = _freeVars.freeze();
            freeVars = result;
        }
        return result;
    }

    public void visitFreeVars(Action1<ITermVar> onFreeVar) {
        freeVars().forEach(onFreeVar::apply);
    }

    private void doVisitFreeVars(Action1<ITermVar> onFreeVar) {
        unifier.varSet().forEach(v -> {
            if(!vars.contains(v)) {
                onFreeVar.apply(v);
            }
        });
        for(IConstraint constraint : constraints) {
            constraint.visitFreeVars(v -> {
                if(!vars.contains(v)) {
                    onFreeVar.apply(v);
                }
            });
        }
    }

    public void visitVars(Action1<ITermVar> onVar) {
        vars.forEach(onVar::apply);
        unifier.varSet().forEach(onVar::apply);
        for(IConstraint constraint : constraints) {
            Constraints.vars(constraint, onVar);
        }
    }


    public PreSolvedConstraint apply(IRenaming subst) {
        Set.Immutable<ITermVar> vars = this.vars;
        IUniDisunifier.Immutable unifier = this.unifier;
        List<IConstraint> constraints = this.constraints;
        ICompleteness.Immutable bodyCriticalEdges = this.bodyCriticalEdges;

        vars = CapsuleUtil.toSet(subst.rename(vars));
        unifier = unifier.rename(subst);
        constraints = constraints.stream().map(c -> c.apply(subst)).collect(ImList.Immutable.toImmutableList());
        if(bodyCriticalEdges != null) {
            bodyCriticalEdges = bodyCriticalEdges.apply(subst);
        }

        return new PreSolvedConstraint(vars, unifier, constraints, cause, bodyCriticalEdges, null);
    }


    public IConstraint toConstraint() {
        IConstraint newConstraint = Constraints.conjoin(
            Iterables2.fromConcat(StateUtil.asConstraint(unifier), constraints));
        if(!vars.isEmpty() || (bodyCriticalEdges != null && !bodyCriticalEdges.isEmpty())) {
            newConstraint = new CExists(vars, newConstraint, cause, bodyCriticalEdges).withCause(cause);
        }
        return newConstraint;
    }


    /**
     * Internalize the given unifier, avoiding capture.
     */
    public PreSolvedConstraint intern(Set.Immutable<ITermVar> otherExistentials,
            IUniDisunifier.Immutable otherUnifier) {
        if(otherUnifier.isEmpty()) {
            return this;
        }

        final Set.Immutable<ITermVar> otherFreeVars = otherUnifier.varSet().__removeAll(otherExistentials);

        final FreshVars fresh = new FreshVars(otherFreeVars, freeVars());

        final IRenaming otherRen = fresh.fresh(otherExistentials);
        if(!otherRen.isEmpty()) {
            otherUnifier = otherUnifier.rename(otherRen);
        }

        IUniDisunifier.Immutable unifier = this.unifier;
        List<IConstraint> constraints = this.constraints;
        @Nullable ICompleteness.Immutable bodyCriticalEdges = this.bodyCriticalEdges;
        Set.Immutable<ITermVar> freeVars = this.freeVars;

        final IRenaming ren = fresh.fresh(vars);
        if(!ren.isEmpty()) {
            unifier = unifier.rename(ren);
            constraints = constraints.stream().map(c -> c.apply(ren.asSubstitution()))
                    .collect(ImList.Immutable.toImmutableList());
            if(bodyCriticalEdges != null) {
                bodyCriticalEdges = bodyCriticalEdges.apply(ren);
            }
        }

        final Set.Immutable<ITermVar> vars = fresh.fix();

        final IUniDisunifier.Result<IUnifier.Immutable> result;
        try {
            if((result = unifier.uniDisunify(otherUnifier).orElse(null)) == null) {
                return contradiction();
            }
        } catch(OccursException e) {
            return contradiction();
        }
        unifier = result.unifier();
        if(bodyCriticalEdges != null) {
            bodyCriticalEdges = bodyCriticalEdges.updateAll(result.result().domainSet(), result.result());
        }
        if(freeVars != null) {
            freeVars = freeVars.__insertAll(otherFreeVars);
        }

        return new PreSolvedConstraint(vars, unifier, constraints, cause, bodyCriticalEdges, freeVars);
    }

    /**
     * Internalize the given unifier, avoiding capture.
     */
    public PreSolvedConstraint intern(Set.Immutable<ITermVar> otherExistentials, IUnifier.Immutable otherUnifier) {
        if(otherUnifier.isEmpty()) {
            return this;
        }

        final Set.Immutable<ITermVar> otherFreeVars = otherUnifier.varSet().__removeAll(otherExistentials);

        final FreshVars fresh = new FreshVars(otherFreeVars, freeVars());

        final IRenaming otherRen = fresh.fresh(otherExistentials);
        if(!otherRen.isEmpty()) {
            otherUnifier = otherUnifier.rename(otherRen);
        }

        IUniDisunifier.Immutable unifier = this.unifier;
        List<IConstraint> constraints = this.constraints;
        @Nullable ICompleteness.Immutable bodyCriticalEdges = this.bodyCriticalEdges;
        Set.Immutable<ITermVar> freeVars = this.freeVars;

        final IRenaming ren = fresh.fresh(vars);
        if(!ren.isEmpty()) {
            unifier = unifier.rename(ren);
            constraints = constraints.stream().map(c -> c.apply(ren.asSubstitution()))
                    .collect(ImList.Immutable.toImmutableList());
            if(bodyCriticalEdges != null) {
                bodyCriticalEdges = bodyCriticalEdges.apply(ren);
            }
        }

        final Set.Immutable<ITermVar> vars = fresh.fix();

        final IUniDisunifier.Result<IUnifier.Immutable> result;
        try {
            if((result = unifier.unify(otherUnifier).orElse(null)) == null) {
                return contradiction();
            }
        } catch(OccursException e) {
            return contradiction();
        }
        unifier = result.unifier();
        if(bodyCriticalEdges != null) {
            bodyCriticalEdges = bodyCriticalEdges.updateAll(result.result().domainSet(), result.result());
        }
        if(freeVars != null) {
            freeVars = freeVars.__insertAll(otherFreeVars);
        }

        return new PreSolvedConstraint(vars, unifier, constraints, cause, bodyCriticalEdges, freeVars);
    }

    /**
     * Internalize the given unifier unguarded, which may result in capture.
     */
    public PreSolvedConstraint unsafeIntern(Set.Immutable<ITermVar> otherExistentials, IUniDisunifier otherUnifier) {
        if(otherUnifier.isEmpty()) {
            return this;
        }

        Set.Immutable<ITermVar> vars = this.vars;
        IUniDisunifier.Immutable unifier = this.unifier;
        @Nullable ICompleteness.Immutable bodyCriticalEdges = this.bodyCriticalEdges;
        Set.Immutable<ITermVar> freeVars = this.freeVars;


        final IUniDisunifier.Result<IUnifier.Immutable> result;
        try {
            if((result = unifier.uniDisunify(otherUnifier).orElse(null)) == null) {
                return contradiction();
            }
        } catch(OccursException e) {
            return contradiction();
        }
        vars = vars.__insertAll(otherExistentials);
        unifier = result.unifier();
        if(bodyCriticalEdges != null) {
            bodyCriticalEdges = bodyCriticalEdges.updateAll(result.result().domainSet(), result.result());
        }
        if(freeVars != null) {
            freeVars = freeVars.__insertAll(unifier.varSet().__removeAll(vars));
        }

        return new PreSolvedConstraint(vars, unifier, constraints, cause, bodyCriticalEdges, freeVars);
    }

    /**
     * Internalize the given unifier unguarded, which may result in capture.
     */
    public PreSolvedConstraint unsafeIntern(Set.Immutable<ITermVar> otherExistentials, IUnifier otherUnifier) {
        if(otherUnifier.isEmpty()) {
            return this;
        }

        Set.Immutable<ITermVar> vars = this.vars;
        IUniDisunifier.Immutable unifier = this.unifier;
        @Nullable ICompleteness.Immutable bodyCriticalEdges = this.bodyCriticalEdges;
        Set.Immutable<ITermVar> freeVars = this.freeVars;


        final IUniDisunifier.Result<IUnifier.Immutable> result;
        try {
            if((result = unifier.unify(otherUnifier).orElse(null)) == null) {
                return contradiction();
            }
        } catch(OccursException e) {
            return contradiction();
        }
        vars = vars.__insertAll(otherExistentials);
        unifier = result.unifier();
        if(bodyCriticalEdges != null) {
            bodyCriticalEdges = bodyCriticalEdges.updateAll(result.result().domainSet(), result.result());
        }
        if(freeVars != null) {
            freeVars = freeVars.__insertAll(unifier.varSet().__removeAll(vars));
        }

        return new PreSolvedConstraint(vars, unifier, constraints, cause, bodyCriticalEdges, freeVars);
    }


    private PreSolvedConstraint contradiction() {
        return new PreSolvedConstraint(CapsuleUtil.immutableSet(), PersistentUniDisunifier.Immutable.of(),
                ImList.Immutable.of(new CFalse()), cause, bodyCriticalEdges == null ? null : Completeness.Immutable.of(),
                CapsuleUtil.immutableSet());
    }

    private static PreSolvedConstraint contradiction(IConstraint constraint) {
        return new PreSolvedConstraint(CapsuleUtil.immutableSet(), PersistentUniDisunifier.Immutable.of(),
                ImList.Immutable.of(new CFalse()), constraint.cause().orElse(null),
                constraint.bodyCriticalEdges().map(bce -> Completeness.Immutable.of()).orElse(null),
                CapsuleUtil.immutableSet());
    }


    /**
     * Externalize the given variables, returning a substitution for the variables and an updated rule body. The given
     * variables must be free in the rule body, or part of the existential variables. Existential variables of the rule
     * body may become free in the updated rule body.
     *
     * This method works under the assumption that the provided variables were bound, and that these binders are
     * replaced by the terms provided in the substitution, causing the variables that appear there to be bound instead.
     */
    public Tuple2<ISubstitution.Immutable, PreSolvedConstraint> extern(Iterable<ITermVar> externVars) {
        final Set.Immutable<ITermVar> freeVars = freeVars();
        final Set.Immutable<ITermVar> externVarSet = CapsuleUtil.toSet(externVars).__retainAll(freeVars);

        Set.Immutable<ITermVar> vars = this.vars;
        final IUniDisunifier.Transient _unifier = this.unifier.melt();

        // extract the terms for the variables, and adapt existential variables
        ISubstitution.Immutable subst = PersistentSubstitution.Immutable.of();
        for(ITermVar var : externVarSet) {
            subst = subst.put(var, _unifier.findRecursive(var));
        }
        final java.util.Set<ITermVar> externTermVars = subst.rangeSet();
        vars = vars.__removeAll(externTermVars);

        // reintroduce externalized variables that appear free in the body
        final Set.Immutable<ITermVar> newFreeVars =
                Constraints.freeVars(constraints).__insertAll(_unifier.varSet()).__removeAll(vars);
        final Set.Immutable<ITermVar> lostVars = externVarSet.__removeAll(externTermVars).__retainAll(newFreeVars);
        vars = vars.__insertAll(lostVars);

        // fix free variables captured in new terms
        final Set.Immutable<ITermVar> capturedVars = freeVars.__removeAll(externVarSet).__retainAll(externTermVars);
        if(!capturedVars.isEmpty()) {
            final FreshVars fresh = new FreshVars(freeVars, externTermVars, vars);
            final IRenaming ren = fresh.fresh(capturedVars);
            subst = subst.compose(ren.asSubstitution());
            try {
                if(!_unifier.unify(ren.entrySet()).isPresent()) {
                    throw new IllegalStateException();
                }
            } catch(OccursException ex) {
                throw new IllegalStateException();
            }
        }

        return Tuple2.of(subst,
                new PreSolvedConstraint(vars, _unifier.freeze(), constraints, cause, bodyCriticalEdges, null));
    }

    /**
     * Reduce the unifier to only include variables that are free or used in other constraints.
     */
    public PreSolvedConstraint cleanup() {
        final Set.Immutable<ITermVar> constraintFreeVars = Constraints.freeVars(constraints);
        final Set.Immutable<ITermVar> relevantVars = freeVars().__insertAll(constraintFreeVars);
        final Set.Immutable<ITermVar> irrelevantVars = this.unifier.varSet().__removeAll(relevantVars);
        final IUniDisunifier.Immutable unifier = this.unifier.removeAll(irrelevantVars).unifier();
        final Set.Immutable<ITermVar> vars = this.vars.__retainAll(constraintFreeVars.__insertAll(unifier.varSet()));
        return new PreSolvedConstraint(vars, unifier, constraints, cause, bodyCriticalEdges, null);
    }

    public static PreSolvedConstraint of(IConstraint constraint) {
        final Set.Immutable<ITermVar> freeVars = constraint.freeVars();
        final FreshVars fresh = new FreshVars(freeVars);
        final IUniDisunifier.Transient unifier = PersistentUniDisunifier.Immutable.of().melt();
        final List<IConstraint> constraints = new ArrayList<>();
        final ICompleteness.Transient bodyCriticalEdges = Completeness.Transient.of();
        final List<IConstraint> failures = new ArrayList<>();
        final Map<IConstraint, Delay> delays = new HashMap<>();
        preSolve(constraint, fresh::fresh, unifier, Predicate1.never(), new HashSet<>(), constraints, bodyCriticalEdges,
                new HashMap<>(), failures, delays, null, false);
        if(!failures.isEmpty()) {
            return contradiction(constraint);
        }
        if(!delays.isEmpty()) {
            throw new IllegalArgumentException("Unexpected delays: " + delays);
        }
        final Set.Immutable<ITermVar> vars = fresh.fix();
        return new PreSolvedConstraint(vars, unifier.freeze(), constraints, constraint.cause().orElse(null),
                bodyCriticalEdges.freeze(), freeVars);

    }

    /**
     * Pre-solve the constraint into the given data structures. A list of failed constraints is returned.
     */
    public static void preSolve(IConstraint constraint, Function1<java.util.Set<ITermVar>, IRenaming> fresh,
            IUniDisunifier.Transient unifier, Predicate1<ITermVar> isRigid, java.util.Set<ITermVar> updatedVars,
            Collection<IConstraint> constraints, ICompleteness.Transient bodyCriticalEdges,
            Map<ITermVar, ITermVar> existentials, Collection<IConstraint> failures, Map<IConstraint, Delay> delays,
            @Nullable IConstraint cause, boolean returnOnFirstErrorOrDelay) {
        final Deque<IConstraint> worklist = new LinkedList<>();
        worklist.push(constraint);
        AtomicBoolean first = new AtomicBoolean(true);
        while(!worklist.isEmpty()) {
            final IConstraint c = worklist.removeLast();
            // @formatter:off
            final boolean okay = c.match(Constraints.<Boolean>cases(
                carith -> { constraints.add(c.withCause(cause)); return true; },
                conj   -> { worklist.addAll(Constraints.disjoin(conj)); return true; },
                cequal -> {
                    try {
                        final IUnifier.Immutable result;
                        if((result = unifier.unify(cequal.term1(), cequal.term2(), isRigid).orElse(null)) == null) {
                            failures.add(cequal.withCause(cause));
                            return false;
                        }
                        updatedVars.addAll(result.domainSet());
                        bodyCriticalEdges.updateAll(result.domainSet(), result);
                        return true;
                    } catch(OccursException e) {
                        failures.add(cequal.withCause(cause));
                        return false;
                    } catch(RigidException e) {
                        delays.put(cequal, Delay.ofVars(e.vars()));
                        return false;
                    }
                },
                cexists -> {
                    final IRenaming renaming = fresh.apply(cexists.vars()/*FIXME possible opt: .__retainAll(cexists.constraint().freeVars())*/);
                    if(first.get()) {
                        existentials.putAll(renaming.asMap());
                    }
                    worklist.add(cexists.constraint().apply(renaming));
                    cexists.bodyCriticalEdges().ifPresent(bce -> {
                        bodyCriticalEdges.addAll(bce.apply(renaming), unifier);
                    });
                    return true;
                },
                cfalse -> {
                    failures.add(cfalse.withCause(cause));
                    return false;
                },
                cinequal  -> {
                    try {
                        if(!unifier.disunify(cinequal.universals(), cinequal.term1(), cinequal.term2(), isRigid).isPresent()) {
                            failures.add(cinequal.withCause(cause));
                            return false;
                        }
                    } catch (RigidException e) {
                        delays.put(cinequal, Delay.ofVars(e.vars()));
                        return false;
                    }
                    return true;
                },
                cnew      -> { constraints.add(c.withCause(cause)); return true; },
                cquery    -> { constraints.add(c.withCause(cause)); return true; },
                ctelledge -> { constraints.add(c.withCause(cause)); return true; },
                castid    -> { constraints.add(c.withCause(cause)); return true; },
                castprop  -> { constraints.add(c.withCause(cause)); return true; },
                ctrue     -> { return true; },
                ctry      -> { constraints.add(c.withCause(cause)); return true; },
                cuser     -> { constraints.add(c.withCause(cause)); return true; }
            ));
            first.set(false);
            // @formatter:on
            if(!okay && returnOnFirstErrorOrDelay) {
                return;
            }
        }
    }

    public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("{").append(termToString.format(vars)).append("} ");
        if(!unifier.isEmpty()) {
            sb.append(StateUtil.asConstraint(unifier).stream().map(c -> c.toString(termToString))
                    .collect(Collectors.joining(", ", "", " | ")));
        }
        sb.append(Constraints.toString(constraints, termToString));
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }


}
