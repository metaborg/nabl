package mb.statix.solver.constraint;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Optional;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.statix.scopegraph.path.IScopePath;
import mb.statix.solver.ConstraintContext;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.Result;
import mb.statix.solver.State;

public class CPathScopes implements IConstraint {

    private final ITerm pathTerm;
    private final ITerm scopesTerm;

    private final @Nullable IConstraint cause;

    public CPathScopes(ITerm pathTerm, ITerm scopesTerm) {
        this(pathTerm, scopesTerm, null);
    }

    public CPathScopes(ITerm pathTerm, ITerm scopesTerm, @Nullable IConstraint cause) {
        this.pathTerm = pathTerm;
        this.scopesTerm = scopesTerm;
        this.cause = cause;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CPathScopes withCause(@Nullable IConstraint cause) {
        return new CPathScopes(pathTerm, scopesTerm, cause);
    }

    @Override public CPathScopes apply(ISubstitution.Immutable subst) {
        return new CPathScopes(subst.apply(pathTerm), subst.apply(scopesTerm), cause);
    }

    @Override public Optional<Result> solve(State state, ConstraintContext params) throws Delay {
        final IUnifier unifier = state.unifier();
        if(!(unifier.isGround(pathTerm))) {
            throw Delay.ofVars(unifier.getVars(pathTerm));
        }
        @SuppressWarnings("unchecked") final IScopePath<ITerm, ITerm> path =
                M.blobValue(IScopePath.class).match(pathTerm, unifier).orElseThrow(
                        () -> new IllegalArgumentException("Expected path, got " + unifier.toString(pathTerm)));
        return Optional
                .of(Result.of(state, ImmutableSet.of(new CEqual(B.newList(path.scopes()), scopesTerm, this))));
    }

    @Override public String toString(IUnifier unifier) {
        final StringBuilder sb = new StringBuilder();
        sb.append("scopes(");
        sb.append(unifier.toString(pathTerm));
        sb.append(", ");
        sb.append(unifier.toString(scopesTerm));
        sb.append(")");
        return sb.toString();
    }

    @Override public String toString() {
        return toString(PersistentUnifier.Immutable.of());
    }

}