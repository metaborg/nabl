package mb.statix.constraints.compiled;

import java.io.Serializable;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

public final class RMerge implements RExp, Serializable {

    private static final long serialVersionUID = 1L;

    private final ImmutableList<RVar> envs;

    public RMerge(ImmutableList<RVar> envs) {
        this.envs = envs;
    }

    public ImmutableList<RVar> envs() {
        return envs;
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseMerge(envs);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
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

        final RMerge other = (RMerge) obj;
        return Objects.equals(envs, other.envs);
    }

    @Override public int hashCode() {
        return Objects.hash(envs);
    }

}
