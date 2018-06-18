package mb.statix.solver.constraint;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Optional;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.statix.scopegraph.path.IScopePath;
import mb.statix.solver.Completeness;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.Result;
import mb.statix.solver.State;

public class CPathLabels implements IConstraint {

    private final ITerm pathTerm;
    private final ITerm labelsTerm;

    public CPathLabels(ITerm pathTerm, ITerm labelsTerm) {
        this.pathTerm = pathTerm;
        this.labelsTerm = labelsTerm;
    }

    @Override public IConstraint apply(ISubstitution.Immutable subst) {
        return new CPathLabels(subst.apply(pathTerm), subst.apply(labelsTerm));
    }

    @Override public Optional<Result> solve(State state, Completeness completeness, IDebugContext debug) {
        final IUnifier unifier = state.unifier();
        if(!(unifier.isGround(pathTerm))) {
            return Optional.empty();
        }
        @SuppressWarnings("unchecked") final IScopePath<ITerm, ITerm> path =
                M.blobValue(IScopePath.class).match(pathTerm, unifier).orElseThrow(
                        () -> new IllegalArgumentException("Expected path, got " + unifier.toString(pathTerm)));
        return Optional.of(Result.of(state, ImmutableSet.of(new CEqual(B.newList(path.getLabels()), labelsTerm))));
    }

    @Override public String toString(IUnifier unifier) {
        final StringBuilder sb = new StringBuilder();
        sb.append("labels(");
        sb.append(unifier.toString(pathTerm));
        sb.append(",");
        sb.append(unifier.toString(labelsTerm));
        sb.append(")");
        return sb.toString();
    }

    @Override public String toString() {
        return toString(PersistentUnifier.Immutable.of());
    }

}