package mb.statix.constraints;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

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

public class CExists implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final Set.Immutable<ITermVar> vars;
    private final IConstraint constraint;

    private final @Nullable IConstraint cause;
    private final @Nullable ICompleteness.Immutable bodyCriticalEdges;

    private volatile Set.Immutable<ITermVar> freeVars;

    public CExists(Iterable<ITermVar> vars, IConstraint constraint) {
        this(vars, constraint, null, null, null);
    }

    public CExists(Iterable<ITermVar> vars, IConstraint constraint, @Nullable IConstraint cause) {
        this(vars, constraint, cause, null, null);
    }

    public CExists(Iterable<ITermVar> vars, IConstraint constraint, @Nullable IConstraint cause,
            ICompleteness.Immutable bodyCriticalEdges) {
        this(vars, constraint, cause, bodyCriticalEdges, null);
    }

    private CExists(Iterable<ITermVar> vars, IConstraint constraint, @Nullable IConstraint cause,
            @Nullable ICompleteness.Immutable bodyCriticalEdges, @Nullable Set.Immutable<ITermVar> freeVars) {
        this.vars = CapsuleUtil.toSet(vars);
        this.constraint = constraint;
        this.cause = cause;
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
        return new CExists(vars, constraint, cause, bodyCriticalEdges, null);
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CExists withCause(@Nullable IConstraint cause) {
        return new CExists(vars, constraint, cause, bodyCriticalEdges, freeVars);
    }

    @Override public Optional<ICompleteness.Immutable> bodyCriticalEdges() {
        return Optional.ofNullable(bodyCriticalEdges);
    }

    @Override public CExists withBodyCriticalEdges(@Nullable ICompleteness.Immutable criticalEdges) {
        return new CExists(vars, constraint, cause, criticalEdges, freeVars);
    }

    @Override public Set.Immutable<ITermVar> getVars() {
        return Set.Immutable.union(
            vars,
            constraint.getVars()
        );
    }

    @Override public Set.Immutable<ITermVar> freeVars() {
        Set.Immutable<ITermVar> result = freeVars;
        if(freeVars == null) {
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
            if(!vars.contains(v)) {
                onFreeVar.apply(v);
            }
        });
    }


    @Override public CExists apply(ISubstitution.Immutable subst) {
        ISubstitution.Immutable localSubst = subst.removeAll(vars).retainAll(freeVars());
        if(localSubst.isEmpty()) {
            return this;
        }

        IConstraint constraint = this.constraint;
        @Nullable ICompleteness.Immutable bodyCriticalEdges = this.bodyCriticalEdges;
        Set.Immutable<ITermVar> freeVars = this.freeVars;

        if(freeVars != null) {
            // before renaming is included in localSubst
            // note that this is only correct because of .retainAll(freeVars()) above, or it might include too much!
            freeVars = freeVars.__removeAll(localSubst.domainSet()).__insertAll(localSubst.rangeSet());
        }

        final FreshVars fresh = new FreshVars(localSubst.domainSet(), localSubst.rangeSet(), freeVars());
        final IRenaming ren = fresh.fresh(vars);
        final Set.Immutable<ITermVar> vars = fresh.fix();

        if(!ren.isEmpty()) {
            localSubst = ren.asSubstitution().compose(localSubst);
        }

        constraint = constraint.apply(localSubst);
        if(bodyCriticalEdges != null) {
            bodyCriticalEdges = bodyCriticalEdges.apply(localSubst);
        }

        return new CExists(vars, constraint, cause, bodyCriticalEdges, freeVars);
    }

    @Override public CExists unsafeApply(ISubstitution.Immutable subst) {
        ISubstitution.Immutable localSubst = subst.removeAll(vars);
        if(localSubst.isEmpty()) {
            return this;
        }

        IConstraint constraint = this.constraint;
        @Nullable ICompleteness.Immutable bodyCriticalEdges = this.bodyCriticalEdges;

        constraint = constraint.unsafeApply(localSubst);
        if(bodyCriticalEdges != null) {
            bodyCriticalEdges = bodyCriticalEdges.apply(localSubst);
        }

        return new CExists(vars, constraint, cause, bodyCriticalEdges, null);
    }

    @Override public CExists apply(IRenaming subst) {
        Set.Immutable<ITermVar> vars = this.vars;
        IConstraint constraint = this.constraint;
        ICompleteness.Immutable bodyCriticalEdges = this.bodyCriticalEdges;

        vars = CapsuleUtil.toSet(subst.rename(vars));
        constraint = constraint.apply(subst);
        if(bodyCriticalEdges != null) {
            bodyCriticalEdges = bodyCriticalEdges.apply(subst);
        }

        return new CExists(vars, constraint, cause, bodyCriticalEdges, null);
    }


    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("{").append(termToString.format(vars)).append("} ");
        sb.append(constraint.toString(termToString));
        return sb.toString();
    }

    @Override public Tag constraintTag() {
        return Tag.CExists;
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }


    @Override public boolean equals(Object o) {
        if(this == o)
            return true;
        if(o == null || getClass() != o.getClass())
            return false;
        CExists cExists = (CExists) o;
        // @formatter:off
        return Objects.equals(vars, cExists.vars)
                && Objects.equals(constraint, cExists.constraint)
                && Objects.equals(cause, cExists.cause);
        // @formatter:on
    }

    private volatile int hashCode;

    @Override public int hashCode() {
        int result = hashCode;
        if(result == 0) {
            result = Objects.hash(vars, constraint, cause);
            hashCode = result;
        }
        return result;
    }

}
