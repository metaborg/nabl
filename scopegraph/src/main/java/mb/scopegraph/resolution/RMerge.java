package mb.scopegraph.resolution;

import java.io.Serializable;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

import mb.scopegraph.oopsla20.reference.ResolutionException;

public final class RMerge<L> implements RExp<L>, Serializable {

    private static final long serialVersionUID = 1L;

    private final ImmutableList<RVar> envs;

    public RMerge(Iterable<RVar> envs) {
        this.envs = ImmutableList.copyOf(envs);
    }

    public RMerge(RVar... envs) {
        this.envs = ImmutableList.copyOf(envs);
    }

    public ImmutableList<RVar> envs() {
        return envs;
    }

    @Override public <R> R match(Cases<L, R> cases) {
        return cases.caseMerge(envs);
    }

    @Override public <R, E extends Throwable> R matchInResolution(ResolutionCases<L, R> cases)
            throws ResolutionException, InterruptedException {
        return cases.caseMerge(envs);
    }

    @Override public String toString() {
        return "merge(" + envs.stream().map(RVar::toString).collect(Collectors.joining(", ")) + ")";
    }

    @Override public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        }
        if(obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        @SuppressWarnings("unchecked") final RMerge<L> other = (RMerge<L>) obj;
        return Objects.equals(envs, other.envs);
    }

    @Override public int hashCode() {
        return Objects.hash(envs);
    }

}
