package mb.statix.random.scopegraph;

import mb.statix.scopegraph.path.IResolutionPath;

public class Match<S, L, D, X> {

    public final IResolutionPath<S, L, D> path;
    public final X x;

    private Match(IResolutionPath<S, L, D> path, X x) {
        this.path = path;
        this.x = x;
    }

    public static <S, L, D, X> Match<S, L, D, X> of(IResolutionPath<S, L, D> path, X x) {
        return new Match<>(path, x);
    }

}