package mb.statix.generator.scopegraph;

import java.util.Optional;

import mb.statix.scopegraph.path.IResolutionPath;

public class Match<S, L, D, X> {

    public final IResolutionPath<S, L, D> path;
    public final Optional<X> condition;

    public Match(IResolutionPath<S, L, D> path, Optional<X> condition) {
        this.path = path;
        this.condition = condition;
    }

    @Override public String toString() {
        return condition.map(Object::toString).orElse("true") + " => " + path;
    }

}