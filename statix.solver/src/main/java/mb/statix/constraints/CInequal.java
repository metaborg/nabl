package mb.statix.constraints;

import java.io.Serializable;
import java.util.Optional;

import org.checkerframework.checker.nullness.qual.Nullable;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.TermFormatter;
import mb.statix.modular.solver.MConstraintContext;
import mb.statix.modular.solver.MConstraintResult;
import mb.statix.modular.solver.state.IMState;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;

/**
 * Implementation for the inequality constraint.
 * 
 * <pre>term1 != term2</pre>
 */
public class CInequal implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final ITerm term1;
    private final ITerm term2;

    private final @Nullable IConstraint cause;

    public CInequal(ITerm term1, ITerm term2) {
        this(term1, term2, null);
    }

    public CInequal(ITerm term1, ITerm term2, @Nullable IConstraint cause) {
        this.term1 = term1;
        this.term2 = term2;
        this.cause = cause;
    }

    public ITerm term1() {
        return term1;
    }

    public ITerm term2() {
        return term2;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CInequal withCause(@Nullable IConstraint cause) {
        return new CInequal(term1, term2, cause);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseInequal(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseInequal(this);
    }

    @Override public CInequal apply(ISubstitution.Immutable subst) {
        return new CInequal(subst.apply(term1), subst.apply(term2), cause);
    }
    
    @Override
    public Optional<MConstraintResult> solve(IMState state, MConstraintContext params) throws Delay {
        final IUnifier.Immutable unifier = state.unifier();
        return unifier.areEqual(term1, term2).matchOrThrow(result -> {
            if(result) {
                return Optional.empty();
            } else {
                return Optional.of(new MConstraintResult());
            }
        }, vars -> {
            throw Delay.ofVars(vars);
        });
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append(termToString.format(term1));
        sb.append(" != ");
        sb.append(termToString.format(term2));
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

}