package mb.statix.constraints.compiled;

import java.io.Serializable;
import java.util.Objects;

import com.google.common.collect.ImmutableList;

public class State implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ImmutableList<RStep> resolutionSteps;

    private final RVar resultVar;

    public State(Iterable<RStep> resolutionSteps, RVar resultVar) {
        this.resolutionSteps = ImmutableList.copyOf(resolutionSteps);
        this.resultVar = resultVar;
    }

    public ImmutableList<RStep> resolutionSteps() {
        return resolutionSteps;
    }

    public RVar resultVar() {
        return resultVar;
    }

    @Override public String toString() {
        return "StateMachine{resolutionSteps=" + resolutionSteps + ", resultVar=" + resultVar + "}";
    }

    @Override public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        }
        if(obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        final State other = (State) obj;
        return Objects.equals(resolutionSteps, other.resolutionSteps) && Objects.equals(resultVar, other.resultVar);
    }

    @Override public int hashCode() {
        return Objects.hash(resolutionSteps, resultVar);
    }

}
