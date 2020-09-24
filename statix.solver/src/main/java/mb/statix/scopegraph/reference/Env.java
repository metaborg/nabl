package mb.statix.scopegraph.reference;

import java.util.Iterator;

import com.google.common.collect.ImmutableSet;

import mb.statix.scopegraph.path.IResolutionPath;

public class Env<S, L, D> implements Iterable<IResolutionPath<S, L, D>> {

    private final ImmutableSet<IResolutionPath<S, L, D>> paths;

    private Env(ImmutableSet<IResolutionPath<S, L, D>> paths) {
        this.paths = paths;
    }

    public boolean isEmpty() {
        return paths.isEmpty();
    }

    @Override public Iterator<IResolutionPath<S, L, D>> iterator() {
        return paths.iterator();
    }

    public static <S, L, D> Env<S, L, D> empty() {
        return new Env<>(ImmutableSet.of());
    }

    public static <S, L, D> Env<S, L, D> of(IResolutionPath<S, L, D> path) {
        return new Env<>(ImmutableSet.of(path));
    }

    public static <S, L, D> Builder<S, L, D> builder() {
        return new Builder<>();
    }

    public static class Builder<S, L, D> {

        private final ImmutableSet.Builder<IResolutionPath<S, L, D>> paths;

        private Builder() {
            this.paths = ImmutableSet.builder();
        }

        public void add(IResolutionPath<S, L, D> path) {
            this.paths.add(path);
        }

        public void addAll(Iterable<? extends IResolutionPath<S, L, D>> paths) {
            this.paths.addAll(paths);
        }

        public Env<S, L, D> build() {
            return new Env<>(paths.build());
        }

    }

}