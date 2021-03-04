package mb.statix.scopegraph.terms.newPath;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import io.usethesource.capsule.Set;
import mb.nabl2.util.collections.ConsList;
import mb.statix.scopegraph.path.IScopePath;
import mb.statix.scopegraph.path.IStep;
import mb.statix.scopegraph.terms.path.Paths;

public class ScopePath<S, L> implements IScopePath<S, L> {

    private final S source;
    private final Path<S, L> path;

    // pre-computed derived values
    private final int size;
    private final Set.Immutable<S> scopeSet;
    // FIXME add hashCode?

    public ScopePath(S source) {
        this(source, null, 0, Set.Immutable.of(source));
    }

    ScopePath(S source, Path<S, L> path, int size, Set.Immutable<S> scopeSet) {
        this.source = source;
        this.path = path;
        this.size = size;
        this.scopeSet = scopeSet;
    }

    @Override public S getSource() {
        return source;
    }

    @Override public S getTarget() {
        return path == null ? source : path.target;
    }

    @Override public int size() {
        return size;
    }

    @Override public Set.Immutable<S> scopeSet() {
        return scopeSet;
    }

    @Override public ConsList<S> scopes() {
        throw new UnsupportedOperationException();
    }

    @Override public ConsList<L> labels() {
        throw new UnsupportedOperationException();
    }

    @Override public Iterator<IStep<S, L>> iterator() {
        return new PathIterator<>(source, path);
    }

    public Optional<ScopePath<S, L>> step(L label, S target) {
        if(scopeSet.contains(target)) {
            return Optional.empty();
        }
        final Path<S, L> newPath = new Path<>(path, label, target);
        return Optional.of(new ScopePath<>(source, newPath, size + 1, scopeSet.__insert(target)));
    }

    public <D> ResolutionPath<S, L, D> resolve(D datum) {
        return new ResolutionPath<>(this, datum);
    }

    @Override public String toString(boolean includeSource, boolean includeTarget) {
        final StringBuilder sb = new StringBuilder();
        Path<S, L> path = reverse(this.path);
        if(includeSource) {
            sb.append(source);
        }
        while(path != null) {
            sb.append(Paths.PATH_SEPARATOR).append(path.label);
            if(path.prefix != null || includeTarget) {
                sb.append(Paths.PATH_SEPARATOR).append(path.target);
            }
            path = path.prefix;

        }
        return sb.toString();
    }

    @Override public String toString() {
        return toString(true, true);
    }


    @Override public int hashCode() {
        return Objects.hash(source, path);
    }

    @Override public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        @SuppressWarnings("rawtypes") ScopePath other = (ScopePath) obj;
        if(size != other.size)
            return false;
        return Objects.equals(source, other.source) && Objects.equals(path, other.path);
    }

    private static <S, L> Path<S, L> reverse(Path<S, L> path) {
        Path<S, L> revPath = null;
        while(path != null) {
            revPath = new Path<>(revPath, path.label, path.target);
            path = path.prefix;
        }
        return revPath;
    }

    private static class Path<S, L> {

        public final Path<S, L> prefix;
        public final L label;
        public final S target;

        public Path(Path<S, L> path, L label, S target) {
            this.prefix = path;
            this.label = label;
            this.target = target;
        }

        @Override public int hashCode() {
            return Objects.hash(label, target, prefix);
        }

        @Override public boolean equals(Object obj) {
            if(this == obj)
                return true;
            if(obj == null)
                return false;
            if(getClass() != obj.getClass())
                return false;
            @SuppressWarnings("rawtypes") Path other = (Path) obj;
            return Objects.equals(label, other.label) && Objects.equals(target, other.target)
                    && Objects.equals(prefix, other.prefix);
        }


    }

    private static class PathIterator<S, L> implements Iterator<IStep<S, L>> {

        private S source;
        private Path<S, L> path;

        private PathIterator(S source, Path<S, L> path) {
            this.source = source;
            this.path = reverse(path);
        }

        @Override public boolean hasNext() {
            return path != null;
        }

        @Override public IStep<S, L> next() {
            if(path == null) {
                throw new NoSuchElementException();
            }
            final IStep<S, L> step = Paths.edge(this.source, path.label, path.target);
            this.source = path.target;
            this.path = path.prefix;
            return step;
        }

    }

}