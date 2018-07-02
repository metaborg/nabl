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
import mb.statix.solver.Completeness;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.Result;
import mb.statix.solver.State;
import mb.statix.solver.log.IDebugContext;

public class CPathLabels implements IConstraint {

    private final ITerm pathTerm;
    private final ITerm labelsTerm;

    private final @Nullable IConstraint cause;

    public CPathLabels(ITerm pathTerm, ITerm labelsTerm) {
        this(pathTerm, labelsTerm, null);
    }

    public CPathLabels(ITerm pathTerm, ITerm labelsTerm, @Nullable IConstraint cause) {
        this.pathTerm = pathTerm;
        this.labelsTerm = labelsTerm;
        this.cause = cause;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CPathLabels withCause(@Nullable IConstraint cause) {
        return new CPathLabels(pathTerm, labelsTerm, cause);
    }

    @Override public CPathLabels apply(ISubstitution.Immutable subst) {
        return new CPathLabels(subst.apply(pathTerm), subst.apply(labelsTerm), cause);
    }

    @Override public Optional<Result> solve(State state, Completeness completeness, IDebugContext debug) throws Delay {
        final IUnifier unifier = state.unifier();
        if(!(unifier.isGround(pathTerm))) {
            throw new Delay();
        }
        @SuppressWarnings("unchecked") final IScopePath<ITerm, ITerm> path =
                M.blobValue(IScopePath.class).match(pathTerm, unifier).orElseThrow(
                        () -> new IllegalArgumentException("Expected path, got " + unifier.toString(pathTerm)));
        return Optional
                .of(Result.of(state, ImmutableSet.of(new CEqual(B.newList(path.getLabels()), labelsTerm, this))));
    }

    @Override public String toString(IUnifier unifier) {
        final StringBuilder sb = new StringBuilder();
        sb.append("labels(");
        sb.append(unifier.toString(pathTerm));
        sb.append(", ");
        sb.append(unifier.toString(labelsTerm));
        sb.append(")");
        return sb.toString();
    }

    @Override public String toString() {
        return toString(PersistentUnifier.Immutable.of());
    }

}