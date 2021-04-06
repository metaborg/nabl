package mb.statix.spec;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.metaborg.util.functions.Action1;

import com.google.common.collect.Lists;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.FreshVars;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.nabl2.terms.unification.ud.PersistentUniDisunifier;
import mb.nabl2.util.CapsuleUtil;
import mb.nabl2.util.TermFormatter;
import mb.nabl2.util.Tuple2;
import mb.statix.constraints.CFalse;
import mb.statix.constraints.Constraints;
import mb.statix.solver.IConstraint;
import mb.statix.solver.StateUtil;
import mb.statix.solver.completeness.Completeness;
import mb.statix.solver.completeness.ICompleteness;

public class PreSolvedConstraint implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Set.Immutable<ITermVar> vars;
    private final IUniDisunifier.Immutable unifier;
    private final IConstraint constraint;

    private final @Nullable IConstraint cause;
    private final @Nullable ICompleteness.Immutable bodyCriticalEdges;

    private volatile Set.Immutable<ITermVar> freeVars;

    public PreSolvedConstraint(Iterable<ITermVar> vars, IConstraint constraint) {
        this(vars, PersistentUniDisunifier.Immutable.of(), constraint, null, null, null);
    }

    public PreSolvedConstraint(Iterable<ITermVar> vars, IUniDisunifier.Immutable unifier, IConstraint constraint) {
        this(vars, unifier, constraint, null, null, null);
    }

    public PreSolvedConstraint(Iterable<ITermVar> vars, IUniDisunifier.Immutable unifier, IConstraint constraint,
            @Nullable IConstraint cause) {
        this(vars, unifier, constraint, cause, null, null);
    }

    private PreSolvedConstraint(Iterable<ITermVar> vars, IUniDisunifier.Immutable unifier, IConstraint constraint,
            @Nullable IConstraint cause, @Nullable ICompleteness.Immutable bodyCriticalEdges,
            @Nullable Set.Immutable<ITermVar> freeVars) {
        this.vars = CapsuleUtil.toSet(vars);
        this.unifier = unifier;
        this.constraint = constraint;
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

    public IConstraint constraint() {
        return constraint;
    }

    public PreSolvedConstraint withConstraint(IConstraint constraint) {
        return new PreSolvedConstraint(vars, unifier, constraint, cause, bodyCriticalEdges, null);
    }


    public Set.Immutable<ITermVar> freeVars() {
        Set.Immutable<ITermVar> result = freeVars;
        if(freeVars == null) {
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
        constraint.visitFreeVars(v -> {
            if(!vars.contains(v)) {
                onFreeVar.apply(v);
            }
        });
    }

    public void visitVars(Action1<ITermVar> onVar) {
        vars.forEach(onVar::apply);
        unifier.varSet().forEach(onVar::apply);
        Constraints.vars(constraint, onVar);
    }


    public PreSolvedConstraint apply(IRenaming subst) {
        Set.Immutable<ITermVar> vars = this.vars;
        IUniDisunifier.Immutable unifier = this.unifier;
        IConstraint constraint = this.constraint;
        ICompleteness.Immutable bodyCriticalEdges = this.bodyCriticalEdges;

        vars = CapsuleUtil.toSet(subst.rename(vars));
        unifier = unifier.rename(subst);
        constraint = constraint.apply(subst);
        if(bodyCriticalEdges != null) {
            bodyCriticalEdges = bodyCriticalEdges.apply(subst);
        }

        return new PreSolvedConstraint(vars, unifier, constraint, cause, bodyCriticalEdges, null);
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
        IConstraint constraint = this.constraint;
        @Nullable ICompleteness.Immutable bodyCriticalEdges = this.bodyCriticalEdges;
        Set.Immutable<ITermVar> freeVars = this.freeVars;

        final IRenaming ren = fresh.fresh(vars);
        if(!ren.isEmpty()) {
            unifier = unifier.rename(ren);
            constraint = constraint.apply(ren.asSubstitution());
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

        return new PreSolvedConstraint(vars, unifier, constraint, cause, bodyCriticalEdges, freeVars);
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
        IConstraint constraint = this.constraint;
        @Nullable ICompleteness.Immutable bodyCriticalEdges = this.bodyCriticalEdges;
        Set.Immutable<ITermVar> freeVars = this.freeVars;

        final IRenaming ren = fresh.fresh(vars);
        if(!ren.isEmpty()) {
            unifier = unifier.rename(ren);
            constraint = constraint.apply(ren.asSubstitution());
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

        return new PreSolvedConstraint(vars, unifier, constraint, cause, bodyCriticalEdges, freeVars);
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

        return new PreSolvedConstraint(vars, unifier, constraint, cause, bodyCriticalEdges, freeVars);
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

        return new PreSolvedConstraint(vars, unifier, constraint, cause, bodyCriticalEdges, freeVars);
    }


    private PreSolvedConstraint contradiction() {
        return new PreSolvedConstraint(CapsuleUtil.immutableSet(), PersistentUniDisunifier.Immutable.of(), new CFalse(),
                cause, bodyCriticalEdges == null ? null : Completeness.Immutable.of(), CapsuleUtil.immutableSet());
    }


    /**
     * Externalize the given variables, returning a substitution for the variables and an updated rule body. The given
     * variables must be free in the rule body, or part of the existential variables. Existential variables of the rule
     * body may become free in the updated rule body.
     */
    public Tuple2<ISubstitution.Immutable, PreSolvedConstraint> extern(Iterable<ITermVar> vars) {
        throw new UnsupportedOperationException();
    }


    public PreSolvedConstraint optimize() {
        final Set.Immutable<ITermVar> freeVars = freeVars();
        final FreshVars fresh = new FreshVars(freeVars);
        final IUniDisunifier.Transient unifier = PersistentUniDisunifier.Immutable.of().melt();
        final List<IConstraint> constraints = new ArrayList<>();
        final ICompleteness.Transient bodyCriticalEdges = Completeness.Transient.of();
        if(!process(constraint, fresh, unifier, constraints, bodyCriticalEdges)) {
            return contradiction();
        }
        return new PreSolvedConstraint(fresh.fix(), unifier.freeze(), Constraints.conjoin(constraints), cause,
                bodyCriticalEdges.freeze(), freeVars);

    }

    private static boolean process(IConstraint constraint, FreshVars fresh, IUniDisunifier.Transient unifier,
            Collection<IConstraint> constraints, ICompleteness.Transient bodyCriticalEdges) {
        final Deque<IConstraint> worklist = Lists.newLinkedList();
        worklist.push(constraint);
        while(!worklist.isEmpty()) {
            final IConstraint c = worklist.removeLast();
            // @formatter:off
            final boolean okay = c.match(Constraints.<Boolean>cases(
                carith -> { constraints.add(c); return true; },
                conj   -> { worklist.addAll(Constraints.disjoin(conj)); return true; },
                cequal -> {
                    try {
                        final IUnifier.Immutable result;
                        if((result = unifier.unify(cequal.term1(), cequal.term2()).orElse(null)) == null) {
                            return false;
                        }
                        bodyCriticalEdges.updateAll(result.domainSet(), result);
                        return true;
                    } catch(OccursException e) { return false; }
                },
                cexists -> {
                    final IRenaming renaming = fresh.fresh(cexists.vars());
                    worklist.add(cexists.constraint().apply(renaming));
                    cexists.bodyCriticalEdges().ifPresent(bce -> {
                        bodyCriticalEdges.addAll(bce.apply(renaming), unifier);
                    });
                    return true;
                },
                cfalse    -> { return false; },
                cinequal  -> {
                    return unifier.disunify(cinequal.universals(), cinequal.term1(), cinequal.term2()).isPresent();
                },
                cnew      -> { constraints.add(c); return true; },
                cquery    -> { constraints.add(c); return true; },
                ctelledge -> { constraints.add(c); return true; },
                castid    -> { constraints.add(c); return true; },
                castprop  -> { constraints.add(c); return true; },
                ctrue     -> { return true; },
                ctry      -> { constraints.add(c); return true; },
                cuser     -> { constraints.add(c); return true; }
            ));
            // @formatter:on
            if(!okay) {
                return false;
            }
        }

        return true;
    }

    public PreSolvedConstraint deoptimize() {
        return new PreSolvedConstraint(vars, PersistentUniDisunifier.Immutable.of(),
                Constraints.conjoin(StateUtil.asConstraint(unifier), constraint), cause, bodyCriticalEdges, freeVars);

    }


    public Optional<Boolean> isAlways() {
        if(!unifier.isEmpty()) {
            return Optional.empty();
        }
        return Constraints.trivial(constraint);
    }


    public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("{").append(termToString.format(vars)).append("} ");
        if(!unifier.isEmpty()) {
            sb.append(StateUtil.asConstraint(unifier).stream().map(c -> c.toString(termToString))
                    .collect(Collectors.joining(", ", "", " | ")));
        }
        sb.append(constraint.toString(termToString));
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }


}