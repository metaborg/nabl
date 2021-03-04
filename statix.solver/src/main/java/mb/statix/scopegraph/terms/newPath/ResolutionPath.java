package mb.statix.scopegraph.terms.newPath;

import java.util.Objects;

import io.usethesource.capsule.Set.Immutable;
import mb.nabl2.util.collections.ConsList;
import mb.statix.scopegraph.path.IResolutionPath;

public class ResolutionPath<S, L, D> implements IResolutionPath<S, L, D> {

    private final ScopePath<S, L> path;
    private final D datum;

    ResolutionPath(ScopePath<S, L> path, D datum) {
        this.path = path;
        this.datum = datum;
    }

    @Override public ScopePath<S, L> getPath() {
        return path;
    }

    @Override public D getDatum() {
        return datum;
    }

    public int size() {
        return path.size();
    }

    @Override public Immutable<S> scopeSet() {
        return path.scopeSet();
    }

    @Override public ConsList<S> scopes() {
        return path.scopes();
    }

    @Override public ConsList<L> labels() {
        return path.labels();
    }

    @Override public int hashCode() {
        return Objects.hash(datum, path);
    }

    @Override public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        @SuppressWarnings("rawtypes") ResolutionPath other = (ResolutionPath) obj;
        return Objects.equals(datum, other.datum) && Objects.equals(path, other.path);
    }


}