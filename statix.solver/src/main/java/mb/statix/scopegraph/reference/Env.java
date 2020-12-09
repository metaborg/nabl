package mb.statix.scopegraph.reference;

import java.util.Iterator;

import io.usethesource.capsule.Set;
import mb.nabl2.util.CapsuleUtil;
import mb.statix.scopegraph.path.IResolutionPath;

public class Env<S, L, D> implements Iterable<IResolutionPath<S, L, D>> {

    @SuppressWarnings("rawtypes") private static final Env EMPTY = new Env<>(CapsuleUtil.immutableSet());

    private final Set.Immutable<IResolutionPath<S, L, D>> paths;

    private Env(Set.Immutable<IResolutionPath<S, L, D>> paths) {
        this.paths = paths;
    }

    public boolean isEmpty() {
        return paths.isEmpty();
    }

    @Override public Iterator<IResolutionPath<S, L, D>> iterator() {
        return paths.iterator();
    }

    @SuppressWarnings("unchecked") public static <S, L, D> Env<S, L, D> empty() {
        return EMPTY;
    }

    public static <S, L, D> Env<S, L, D> of(IResolutionPath<S, L, D> path) {
        return new Env<>(CapsuleUtil.immutableSet(path));
    }

    public static <S, L, D> Builder<S, L, D> builder() {
        return new Builder<>();
    }

    public static class Builder<S, L, D> {

        private final Set.Transient<IResolutionPath<S, L, D>> paths;

        private Builder() {
            this.paths = CapsuleUtil.transientSet();
        }

        public void add(IResolutionPath<S, L, D> path) {
            this.paths.__insert(path);
        }

        public void addAll(Iterable<? extends IResolutionPath<S, L, D>> paths) {
            for(IResolutionPath<S, L, D> path : paths) {
                this.paths.__insert(path);
            }
        }

        public Env<S, L, D> build() {
            return paths.isEmpty() ? empty() : new Env<>(paths.freeze());
        }

    }

}