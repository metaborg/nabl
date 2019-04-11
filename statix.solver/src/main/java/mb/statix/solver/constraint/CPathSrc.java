package mb.statix.solver.constraint;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Optional;

import javax.annotation.Nullable;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.path.IScopePath;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.taico.solver.MConstraintContext;
import mb.statix.taico.solver.MConstraintResult;
import mb.statix.taico.solver.MState;

public class CPathSrc implements IConstraint {

    private final ITerm pathTerm;
    private final ITerm srcTerm;

    private final @Nullable IConstraint cause;

    public CPathSrc(ITerm pathTerm, ITerm srcTerm) {
        this(pathTerm, srcTerm, null);
    }

    public CPathSrc(ITerm pathTerm, ITerm srcTerm, @Nullable IConstraint cause) {
        this.pathTerm = pathTerm;
        this.srcTerm = srcTerm;
        this.cause = cause;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CPathSrc withCause(@Nullable IConstraint cause) {
        return new CPathSrc(pathTerm, srcTerm, cause);
    }

    @Override public CPathSrc apply(ISubstitution.Immutable subst) {
        return new CPathSrc(subst.apply(pathTerm), subst.apply(srcTerm), cause);
    }

    @Override
    public Optional<MConstraintResult> solveMutable(MState state, MConstraintContext params) throws Delay {
        final IUnifier unifier = state.unifier();
        if(!(unifier.isGround(pathTerm))) {
            throw Delay.ofVars(unifier.getVars(pathTerm));
        }
        @SuppressWarnings("unchecked") final IScopePath<ITerm, ITerm> path =
                M.blobValue(IScopePath.class).match(pathTerm, unifier).orElseThrow(
                        () -> new IllegalArgumentException("Expected path, got " + unifier.toString(pathTerm)));
        return Optional.of(MConstraintResult.ofConstraints(state, new CEqual(path.getSource(), srcTerm, this)));
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("src(");
        sb.append(termToString.format(pathTerm));
        sb.append(", ");
        sb.append(termToString.format(srcTerm));
        sb.append(")");
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

}