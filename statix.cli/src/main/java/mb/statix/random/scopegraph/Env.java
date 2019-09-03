package mb.statix.random.scopegraph;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class Env<S, L, D, X> {

    public final Set<Match<S, L, D, X>> matches;

    private Env(ImmutableSet<Match<S, L, D, X>> matches) {
        this.matches = ImmutableSet.copyOf(matches);
    }

    public static <S, L, D, X> Env<S, L, D, X> of() {
        return new Env<>(ImmutableSet.of());
    }

    public static <S, L, D, X> Env<S, L, D, X> of(Iterable<Match<S, L, D, X>> paths) {
        return new Env<>(ImmutableSet.copyOf(paths));
    }

}