package mb.statix.solver.constraint;

import java.io.Serializable;
import java.util.Optional;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.metaborg.util.log.Level;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.RigidVarsException;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.log.IDebugContext;
import mb.statix.taico.solver.MConstraintContext;
import mb.statix.taico.solver.MConstraintResult;
import mb.statix.taico.solver.IMState;

/**
 * Implementation for an equality constraint.
 * 
 * <pre>term1 == term2</pre>
 */
public class CEqual implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final ITerm term1;
    private final ITerm term2;

    private final @Nullable IConstraint cause;

    public CEqual(ITerm term1, ITerm term2) {
        this(term1, term2, null);
    }

    public CEqual(ITerm term1, ITerm term2, @Nullable IConstraint cause) {
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

    @Override public CEqual withCause(@Nullable IConstraint cause) {
        return new CEqual(term1, term2, cause);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseEqual(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseEqual(this);
    }

    @Override public CEqual apply(ISubstitution.Immutable subst) {
        return new CEqual(subst.apply(term1), subst.apply(term2), cause);
    }
    
    @Override
    public Optional<MConstraintResult> solve(IMState state, MConstraintContext params) throws Delay {
        IDebugContext debug = params.debug();
        IUnifier.Immutable unifier = state.unifier();
        try {
            final IUnifier.Immutable.Result<IUnifier.Immutable> result;
            if((result = unifier.unify(term1, term2, v -> params.isRigid(v, state)).orElse(null)) != null) {
                if(debug.isEnabled(Level.Info)) {
                    debug.info("Unification succeeded: {}", result.result());
                }
                //TODO CONSTRAINT-CONCURRENCY Concurrency point for unifier modifications
                state.setUnifier(result.unifier());
                return Optional.of(MConstraintResult.ofVars(result.result().varSet()));
            } else {
                if(debug.isEnabled(Level.Info)) {
                    debug.info("Unification failed: {} != {}", unifier.toString(term1), unifier.toString(term2));
                }
                return Optional.empty();
            }
        } catch(OccursException e) {
            if(debug.isEnabled(Level.Info)) {
                debug.info("Unification failed: {} != {}", unifier.toString(term1), unifier.toString(term2));
            }
            return Optional.empty();
        } catch(RigidVarsException e) {
            throw Delay.ofVars(e.vars());
        }
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append(termToString.format(term1));
        sb.append(" == ");
        sb.append(termToString.format(term2));
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

}