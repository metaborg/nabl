package mb.statix.generator.scopegraph;

import java.util.List;
import java.util.Optional;

import org.metaborg.util.collection.ImList;

import mb.scopegraph.oopsla20.path.IResolutionPath;

/**
 * An environment.
 *
 * @param <S> the type of scopes
 * @param <L> the type of labels
 * @param <D> the type of data
 * @param <X> the type of conditions
 */
public class Env<S, L, D, X> {

    /** The matches in the environment. */
    public final List<Match<S, L, D, X>> matches;
    /** THe rejections in the environment. */
    public final List<Match<S, L, D, X>> rejects;

    /**
     * Initializes a new instance of the {@link Env} class.
     *
     * @param matches a list of matches in the environment
     * @param rejects a list of rejections in the environment
     */
    private Env(ImList.Immutable<Match<S, L, D, X>> matches, ImList.Immutable<Match<S, L, D, X>> rejects) {
        this.matches = matches;
        this.rejects = rejects;
    }

    /**
     * Gets whether the environment is empty.
     *
     * @return {@code true} when the environment is empty;
     * otherwise, {@code false}
     */
    public boolean isEmpty() {
        return matches.isEmpty();
    }

    /**
     * Gets whether the environment is nullable,
     * i.e., whether all matches are conditional.
     *
     * @return {@code true} when the environment is nullable;
     * otherwise, {@code false}
     */
    public boolean isNullable() {
        return matches.stream().allMatch(m -> m.condition.isPresent());
    }

    /**
     * Creates an empty environment.
     *
     * @param <S> the type of scopes
     * @param <L> the type of labels
     * @param <D> the type of data
     * @param <X> the type of conditions
     * @return the empty environment
     */
    public static <S, L, D, X> Env<S, L, D, X> empty() {
        return new Env<>(ImList.Immutable.of(), ImList.Immutable.of());
    }

    /**
     * Creates an environment that matches the specified path and condition.
     *
     * @param path the path
     * @param x the condition
     * @param <S> the type of scopes
     * @param <L> the type of labels
     * @param <D> the type of data
     * @param <X> the type of conditions
     * @return the new environment
     */
    public static <S, L, D, X> Env<S, L, D, X> match(IResolutionPath<S, L, D> path, Optional<X> x) {
        return new Env<>(ImList.Immutable.of(new Match<>(path, x)), ImList.Immutable.of());
    }

    /**
     * Creates a new environment builder.
     *
     * @param <S> the type of scopes
     * @param <L> the type of labels
     * @param <D> the type of data
     * @param <X> the type of conditions
     * @return the new environment builder
     */
    public static <S, L, D, X> Builder<S, L, D, X> builder() {
        return new Builder<>();
    }

    /**
     * An environment builder.
     *
     * @param <S> the type of scopes
     * @param <L> the type of labels
     * @param <D> the type of data
     * @param <X> the type of conditions
     */
    public static class Builder<S, L, D, X> {

        private final ImList.Transient<Match<S, L, D, X>> matches;
        private final ImList.Transient<Match<S, L, D, X>> rejects;

        /**
         * Initializes a new instance of the {@link Builder} class.
         */
        public Builder() {
            this.matches = ImList.Transient.of();
            this.rejects = ImList.Transient.of();
        }

        /**
         * Adds a match of the specified path and condition.
         *
         * @param path the path that is matched
         * @param x the condition; or none if it is unconditional
         */
        public void match(IResolutionPath<S, L, D> path, Optional<X> x) {
            match(new Match<>(path, x));
        }

        /**
         * Adds a match.
         *
         * @param match the match to accept
         */
        public void match(Match<S, L, D, X> match) {
            matches.add(match);
        }

        /**
         * Adds a reject of the specified match.
         *
         * @param match the match to reject
         */
        public void reject(Match<S, L, D, X> match) {
            if(!match.condition.isPresent()) {
                throw new IllegalArgumentException("Cannot reject unconditional match");
            }
            rejects.add(match);
        }

        /**
         * Accepts all matches and rejects all rejects from the specified environment.
         *
         * @param env the environment to accept
         */
        public void match(Env<S, L, D, X> env) {
            matches.addAll(env.matches);
            rejects.addAll(env.rejects);
        }

        /**
         * Rejects all matches and rejects all rejects from the specified environment.
         *
         * @param env the environment to reject
         */
        public void reject(Env<S, L, D, X> env) {
            if(env.matches.stream().anyMatch(m -> !m.condition.isPresent())) {
                throw new IllegalArgumentException("Cannot reject environment with unconditional matches");
            }
            rejects.addAll(env.matches);
            rejects.addAll(env.rejects);
        }

        /**
         * Builds the environment.
         *
         * @return the build environment
         */
        public Env<S, L, D, X> build() {
            return new Env<>(matches.freeze(), rejects.freeze());
        }

    }

}