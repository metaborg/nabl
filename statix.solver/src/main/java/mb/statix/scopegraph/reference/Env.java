package mb.statix.scopegraph.reference;

import java.util.Iterator;

import com.google.common.collect.Iterators;

import io.usethesource.capsule.Set;
import mb.nabl2.util.CapsuleUtil;
import mb.statix.scopegraph.terms.newPath.ResolutionPath;

public class Env<S, L, D> implements Iterable<ResolutionPath<S, L, D>> {

    @SuppressWarnings("rawtypes") private static final Env EMPTY = new Env<>(CapsuleUtil.immutableSet());

    private final Set.Immutable<ResolutionPath<S, L, D>> paths;

    private Env(Set.Immutable<ResolutionPath<S, L, D>> paths) {
        this.paths = paths;
    }

    public boolean isEmpty() {
        return paths.isEmpty();
    }

    @Override public Iterator<ResolutionPath<S, L, D>> iterator() {
        return Iterators.transform(paths.iterator(), p -> p);
    }

    @SuppressWarnings("unchecked") public static <S, L, D> Env<S, L, D> empty() {
        return EMPTY;
    }

    public static <S, L, D> Env<S, L, D> of(ResolutionPath<S, L, D> path) {
        return new Env<>(CapsuleUtil.immutableSet(path));
    }

    public static <S, L, D> Builder<S, L, D> builder() {
        return new Builder<>();
    }

    public static class Builder<S, L, D> {

        private final Set.Transient<ResolutionPath<S, L, D>> paths;

        private Builder() {
            this.paths = CapsuleUtil.transientSet();
        }

        public void add(ResolutionPath<S, L, D> path) {
            this.paths.__insert(path);
        }

        public void addAll(Iterable<? extends ResolutionPath<S, L, D>> paths) {
            for(ResolutionPath<S, L, D> path : paths) {
                this.paths.__insert(path);
            }
        }

        public Env<S, L, D> build() {
            return paths.isEmpty() ? empty() : new Env<>(paths.freeze());
        }

    }

}