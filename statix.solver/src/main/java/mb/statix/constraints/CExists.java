package mb.statix.constraints;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import jakarta.annotation.Nullable;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.Action1;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.FreshVars;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.IConstraint;
import mb.statix.solver.completeness.ICompleteness;

public final class CExists implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final Set.Immutable<ITermVar> vars;
    private final IConstraint constraint;

    private final @Nullable IConstraint cause;
    private final @Nullable CExists origin;
    private final @Nullable ICompleteness.Immutable bodyCriticalEdges;

    private volatile Set.Immutable<ITermVar> freeVars;

    public CExists(Iterable<ITermVar> vars, IConstraint constraint) {
        this(vars, constraint, null, null, null, null);
    }

    // Do not call this constructor. Call withArguments() or withCause() instead.
    public CExists(Iterable<ITermVar> vars, IConstraint constraint, @Nullable IConstraint cause) {
        this(vars, constraint, cause, null, null, null);
    }

    // Do not call this constructor. Call withArguments(), withCause(), or withBodyCriticalEdges() instead.
    public CExists(
            Iterable<ITermVar> vars,
            IConstraint constraint,
            @Nullable IConstraint cause,
            ICompleteness.Immutable bodyCriticalEdges
    ) {
        this(vars, constraint, cause, null, bodyCriticalEdges, null);
    }

    // Private constructor, so we can add more fields in the future. Externally call the appropriate with*() functions instead.
    private CExists(
            Iterable<ITermVar> vars,
            IConstraint constraint,
            @Nullable IConstraint cause,
            @Nullable CExists origin,
            @Nullable ICompleteness.Immutable bodyCriticalEdges,
            @Nullable Set.Immutable<ITermVar> freeVars
    ) {
        this.vars = CapsuleUtil.toSet(vars);
        this.constraint = constraint;
        this.cause = cause;
        this.origin = origin;
        this.bodyCriticalEdges = bodyCriticalEdges;
        this.freeVars = freeVars;
    }


    public Set.Immutable<ITermVar> vars() {
        return vars;
    }

    public IConstraint constraint() {
        return constraint;
    }

    public CExists withConstraint(IConstraint constraint) {
        return withArguments(vars, constraint);
    }

    public CExists withArguments(Iterable<ITermVar> vars, IConstraint constraint) {
        if (this.vars == vars &&
            this.constraint == constraint
        ) {
            // Avoid creating new objects if the arguments are the exact same objects.
            // NOTE: Using `==` (instead of `Objects.equals()`) is cheap and already covers 99% of cases.
            return this;
        }
        return new CExists(vars, constraint, cause, origin, bodyCriticalEdges, null);
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CExists withCause(@Nullable IConstraint cause) {
        if (this.cause == cause) {
            // Avoid creating new objects if the arguments are the exact same objects.
            // NOTE: Using `==` (instead of `Objects.equals()`) is cheap and already covers 99% of cases.
            return this;
        }
        return new CExists(vars, constraint, cause, origin, bodyCriticalEdges, freeVars);
    }

    @Override public @Nullable CExists origin() {
        return origin;
    }

    @Override public Optional<ICompleteness.Immutable> bodyCriticalEdges() {
        return Optional.ofNullable(bodyCriticalEdges);
    }

    @Override public CExists withBodyCriticalEdges(@Nullable ICompleteness.Immutable criticalEdges) {
        if (this.bodyCriticalEdges == criticalEdges) {
            // Avoid creating new objects if the arguments are the exact same objects.
            // NOTE: Using `==` (instead of `Objects.equals()`) is cheap and already covers 99% of cases.
            return this;
        }
        return new CExists(vars, constraint, cause, origin, criticalEdges, freeVars);
    }


    @Override public <R> R match(Cases<R> cases) {
        return cases.caseExists(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseExists(this);
    }

    @Override public Set.Immutable<ITermVar> getVars() {
        return Set.Immutable.union(
                vars,
                constraint.getVars()
        );
    }

    @Override public Set.Immutable<ITermVar> freeVars() {
        Set.Immutable<ITermVar> result = freeVars;
        if (result == null) {
            Set.Transient<ITermVar> _freeVars = CapsuleUtil.transientSet();
            doVisitFreeVars(_freeVars::__insert);
            result = _freeVars.freeze();
            freeVars = result;
        }
        return result;
    }

    @Override public void visitFreeVars(Action1<ITermVar> onFreeVar) {
        freeVars().forEach(onFreeVar::apply);
    }

    private void doVisitFreeVars(Action1<ITermVar> onFreeVar) {
        constraint.visitFreeVars(v -> {
            if (!vars.contains(v)) {
                onFreeVar.apply(v);
            }
        });
    }

    @Override public CExists apply(ISubstitution.Immutable subst) {
        return apply(subst, false);
    }

    @Override public CExists unsafeApply(ISubstitution.Immutable subst) {
        return unsafeApply(subst, false);
    }

    @Override public CExists apply(IRenaming subst) {
        return apply(subst, false);
    }

    @Override public CExists apply(ISubstitution.Immutable subst, boolean trackOrigin) {
        ISubstitution.Immutable localSubst = subst.removeAll(vars).retainAll(freeVars());
        if (localSubst.isEmpty()) {
            return this;
        }

        IConstraint constraint = this.constraint;
        @Nullable ICompleteness.Immutable bodyCriticalEdges = this.bodyCriticalEdges;
        Set.Immutable<ITermVar> freeVars = this.freeVars;

        if (freeVars != null) {
            // before renaming is included in localSubst
            // note that this is only correct because of .retainAll(freeVars()) above, or it might include too much!
            freeVars = freeVars.__removeAll(localSubst.domainSet()).__insertAll(localSubst.rangeSet());
        }

        final FreshVars fresh = new FreshVars(localSubst.domainSet(), localSubst.rangeSet(), freeVars());
        final IRenaming ren = fresh.fresh(vars);
        final Set.Immutable<ITermVar> vars = fresh.fix();

        if (!ren.isEmpty()) {
            localSubst = ren.asSubstitution().compose(localSubst);
        }

        constraint = constraint.apply(localSubst, trackOrigin);
        if (bodyCriticalEdges != null) {
            bodyCriticalEdges = bodyCriticalEdges.apply(localSubst);
        }

        return new CExists(
                vars,
                constraint,
                cause,
                origin == null && trackOrigin ? this : origin,
                bodyCriticalEdges,
                freeVars
        );
    }

    @Override public CExists unsafeApply(ISubstitution.Immutable subst, boolean trackOrigin) {
        ISubstitution.Immutable localSubst = subst.removeAll(vars);
        if (localSubst.isEmpty()) {
            return this;
        }

        IConstraint constraint = this.constraint;
        @Nullable ICompleteness.Immutable bodyCriticalEdges = this.bodyCriticalEdges;

        constraint = constraint.unsafeApply(localSubst, trackOrigin);
        if (bodyCriticalEdges != null) {
            bodyCriticalEdges = bodyCriticalEdges.apply(localSubst);
        }

        return new CExists(
                vars,
                constraint,
                cause,
                origin == null && trackOrigin ? this : origin,
                bodyCriticalEdges,
                null
        );
    }

    @Override public CExists apply(IRenaming subst, boolean trackOrigin) {
        Set.Immutable<ITermVar> vars = this.vars;
        IConstraint constraint = this.constraint;
        ICompleteness.Immutable bodyCriticalEdges = this.bodyCriticalEdges;

        vars = CapsuleUtil.toSet(subst.rename(vars));
        constraint = constraint.apply(subst, trackOrigin);
        if (bodyCriticalEdges != null) {
            bodyCriticalEdges = bodyCriticalEdges.apply(subst);
        }

        return new CExists(
                vars,
                constraint,
                cause,
                origin == null && trackOrigin ? this : origin,
                bodyCriticalEdges,
                null
        );
    }


    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("{").append(termToString.format(vars)).append("} ");
        sb.append(constraint.toString(termToString));
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }


    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        final CExists that = (CExists)o;
        // @formatter:off
        return this.hashCode == that.hashCode
            && Objects.equals(this.vars, that.vars)
            && Objects.equals(this.constraint, that.constraint)
            && Objects.equals(this.cause, that.cause)
            && Objects.equals(this.origin, that.origin);
        // @formatter:on
    }

    private final int hashCode = computeHashCode();

    @Override public int hashCode() {
        return hashCode;
    }

    private int computeHashCode() {
        return Objects.hash(
                vars,
                constraint,
                cause,
                origin
        );
    }

}
