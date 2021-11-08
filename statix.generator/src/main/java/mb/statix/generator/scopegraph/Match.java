package mb.statix.generator.scopegraph;

import java.util.Optional;

import mb.scopegraph.oopsla20.path.IResolutionPath;

/**
 * A match.
 *
 * @param <S> the type of scopes
 * @param <L> the type of labels
 * @param <D> the type of data
 * @param <X> the type of conditions
 */
public class Match<S, L, D, X> {

    /** The path that is matched. */
    public final IResolutionPath<S, L, D> path;
    /** The condition; or none if the match is unconditional. */
    public final Optional<X> condition;

    /**
     * Initializes a new instance of the {@link Match} class.
     *
     * @param path the path that is matched
     * @param condition the condition; or none if the match is unconditional
     */
    public Match(IResolutionPath<S, L, D> path, Optional<X> condition) {
        this.path = path;
        this.condition = condition;
    }

    @Override public String toString() {
        return condition.map(Object::toString).orElse("true") + " => " + path;
    }

}