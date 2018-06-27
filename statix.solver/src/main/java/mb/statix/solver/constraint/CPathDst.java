package mb.statix.solver.constraint;

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

public class CPathDst implements IConstraint {

    private final ITerm pathTerm;
    private final ITerm dstTerm;

    public CPathDst(ITerm pathTerm, ITerm dstTerm) {
        this.pathTerm = pathTerm;
        this.dstTerm = dstTerm;
    }

    @Override public IConstraint apply(ISubstitution.Immutable subst) {
        return new CPathDst(subst.apply(pathTerm), subst.apply(dstTerm));
    }

    @Override public Optional<Result> solve(State state, Completeness completeness, IDebugContext debug) {
        final IUnifier unifier = state.unifier();
        if(!(unifier.isGround(pathTerm))) {
            return Optional.empty();
        }
        @SuppressWarnings("unchecked") final IScopePath<ITerm, ITerm> path =
                M.blobValue(IScopePath.class).match(pathTerm, unifier).orElseThrow(
                        () -> new IllegalArgumentException("Expected path, got " + unifier.toString(pathTerm)));
        return Optional.of(Result.of(state, ImmutableSet.of(new CEqual(path.getTarget(), dstTerm))));
    }

    @Override public String toString(IUnifier unifier) {
        final StringBuilder sb = new StringBuilder();
        sb.append("dst(");
        sb.append(unifier.toString(pathTerm));
        sb.append(", ");
        sb.append(unifier.toString(dstTerm));
        sb.append(")");
        return sb.toString();
    }

    @Override public String toString() {
        return toString(PersistentUnifier.Immutable.of());
    }

}