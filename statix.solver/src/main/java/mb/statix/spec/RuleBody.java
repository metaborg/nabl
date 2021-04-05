package mb.statix.spec;

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
import mb.statix.constraints.CFalse;
import mb.statix.constraints.Constraints;
import mb.statix.solver.IConstraint;
import mb.statix.solver.StateUtil;
import mb.statix.solver.completeness.Completeness;
import mb.statix.solver.completeness.ICompleteness;

/**
 * Rule body, essentially an exists constraint with precomputed unifier for the (dis)equalities in the constraints.
 */
public class RuleBody {

    private final Set.Immutable<ITermVar> vars;
    private final IUniDisunifier.Immutable unifier;
    private final IConstraint constraint;

    private final @Nullable IConstraint cause;
    private final @Nullable ICompleteness.Immutable bodyCriticalEdges;

    private volatile Set.Immutable<ITermVar> freeVars;

    private RuleBody(Iterable<ITermVar> vars, IUniDisunifier.Immutable unifier, IConstraint constraint,
            @Nullable IConstraint cause, @Nullable ICompleteness.Immutable bodyCriticalEdges,
            Set.Immutable<ITermVar> freeVars) {
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


    public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    public RuleBody withCause(@Nullable IConstraint cause) {
        return new RuleBody(vars, unifier, constraint, cause, bodyCriticalEdges, freeVars);
    }

    public Optional<ICompleteness.Immutable> bodyCriticalEdges() {
        return Optional.ofNullable(bodyCriticalEdges);
    }

    public RuleBody withBodyCriticalEdges(ICompleteness.Immutable criticalEdges) {
        return new RuleBody(vars, unifier, constraint, cause, criticalEdges, freeVars);
    }


    public Set.Immutable<ITermVar> freeVars() {
        Set.Immutable<ITermVar> result = freeVars;
        if(result == null) {
            final Set.Transient<ITermVar> _freeVars = CapsuleUtil.transientSet();
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

    // FIXME equals & hashCode?

    /**
     * Apply capture avoiding substitution.
     */
    public RuleBody apply(ISubstitution.Immutable subst) {
        ISubstitution.Immutable localSubst = subst.removeAll(vars).retainAll(freeVars());
        if(localSubst.isEmpty()) {
            return this;
        }

        IUniDisunifier.Immutable unifier = this.unifier;
        IConstraint constraint = this.constraint;
        @Nullable ICompleteness.Immutable bodyCriticalEdges = this.bodyCriticalEdges;
        Set.Immutable<ITermVar> freeVars = this.freeVars;

        if(freeVars != null) {
            // before renaming is included in localSubst
            freeVars = freeVars.__removeAll(localSubst.domainSet()).__insertAll(localSubst.rangeSet());
        }

        final FreshVars fresh = new FreshVars(localSubst.domainSet(), localSubst.rangeSet(), freeVars());
        final IRenaming ren = fresh.fresh(vars);
        final Set.Immutable<ITermVar> vars = fresh.fix();

        if(!ren.isEmpty()) {
            unifier = unifier.rename(ren);
            localSubst = ren.asSubstitution().compose(localSubst);
        }

        try {
            if((unifier = unifier.unify(localSubst.entrySet()).map(r -> r.unifier()).orElse(null)) == null) {
                return contradiction(cause);
            }
        } catch(OccursException e) {
            return contradiction(cause);
        }
        constraint = constraint.apply(localSubst);
        if(bodyCriticalEdges != null) {
            bodyCriticalEdges = bodyCriticalEdges.apply(localSubst);
        }

        return new RuleBody(vars, unifier, constraint, cause, bodyCriticalEdges, freeVars);
    }

    public RuleBody apply(IRenaming subst) {
        Set.Immutable<ITermVar> vars = this.vars;
        IUniDisunifier.Immutable unifier = this.unifier;
        IConstraint constraint = this.constraint;
        ICompleteness.Immutable bodyCriticalEdges = this.bodyCriticalEdges;
        Set.Immutable<ITermVar> freeVars = this.freeVars;

        vars = CapsuleUtil.toSet(subst.rename(vars));
        unifier = unifier.rename(subst);
        constraint = constraint.apply(subst);
        if(bodyCriticalEdges != null) {
            bodyCriticalEdges = bodyCriticalEdges.apply(subst);
        }
        if(freeVars != null) {
            freeVars = freeVars.__removeAll(subst.keySet()).__insertAll(subst.rename(freeVars));
        }

        return new RuleBody(vars, unifier, constraint, cause, bodyCriticalEdges, freeVars);
    }


    public Optional<Boolean> isAlways() throws InterruptedException {
        if(unifier.isEmpty()) {
            return Optional.empty();
        }
        return constraint.match(Constraints.<Optional<Boolean>>cases()._true(c -> Optional.of(true))
                ._false(c -> Optional.of(false)).otherwise(c -> Optional.empty()));
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


    public static RuleBody contradiction(final @Nullable IConstraint cause) {
        return new RuleBody(CapsuleUtil.immutableSet(), PersistentUniDisunifier.Immutable.of(), new CFalse(), cause,
                Completeness.Immutable.of(), null);
    }

    public static RuleBody of(IConstraint constraint) {
        final FreshVars fresh = new FreshVars(constraint.freeVars());
        final IUniDisunifier.Transient unifier = PersistentUniDisunifier.Immutable.of().melt();
        final List<IConstraint> constraints = new ArrayList<>();
        final ICompleteness.Transient bodyCriticalEdges = Completeness.Transient.of();
        if(!process(constraint, fresh, unifier, constraints, bodyCriticalEdges)) {
            return contradiction(constraint.cause().orElse(null));
        }
        return new RuleBody(fresh.fix(), unifier.freeze(), Constraints.conjoin(constraints),
                constraint.cause().orElse(null), bodyCriticalEdges.freeze(), null);
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


}