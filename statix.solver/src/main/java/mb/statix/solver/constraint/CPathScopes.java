package mb.statix.solver.constraint;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Optional;

import org.metaborg.util.functions.Function1;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.statix.scopegraph.path.IScopePath;
import mb.statix.solver.Completeness;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.Result;
import mb.statix.solver.State;

public class CPathScopes implements IConstraint {

    private final ITerm pathTerm;
    private final ITerm scopesTerm;

    public CPathScopes(ITerm pathTerm, ITerm scopesTerm) {
        this.pathTerm = pathTerm;
        this.scopesTerm = scopesTerm;
    }

    @Override public IConstraint apply(Function1<ITerm, ITerm> map) {
        return new CPathScopes(map.apply(pathTerm), map.apply(scopesTerm));
    }

    @Override public Optional<Result> solve(State state, Completeness completeness, IDebugContext debug) {
        final IUnifier unifier = state.unifier();
        if(!(unifier.isGround(pathTerm))) {
            return Optional.empty();
        }
        @SuppressWarnings("unchecked") final IScopePath<ITerm, ITerm> path =
                M.blobValue(IScopePath.class).match(pathTerm, unifier).orElseThrow(
                        () -> new IllegalArgumentException("Expected path, got " + unifier.toString(pathTerm)));
        return Optional.of(Result.of(state, ImmutableSet.of(new CEqual(B.newList(path.getScopes()), scopesTerm))));
    }

    @Override public String toString(IUnifier unifier) {
        final StringBuilder sb = new StringBuilder();
        sb.append("scopes(");
        sb.append(unifier.toString(pathTerm));
        sb.append(",");
        sb.append(unifier.toString(scopesTerm));
        sb.append(")");
        return sb.toString();
    }

    @Override public String toString() {
        return toString(PersistentUnifier.Immutable.of());
    }

}