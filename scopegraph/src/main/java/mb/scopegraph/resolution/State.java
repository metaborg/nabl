package mb.scopegraph.resolution;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class State<L> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ImmutableList<RStep<L>> resolutionSteps;
    private final RVar resultVar;

    private final boolean accepting;
    private final ImmutableMap<L, String> transitions;

    public State(Iterable<RStep<L>> resolutionSteps, RVar resultVar, boolean accepting, Map<L, String> transitions) {
        this.resolutionSteps = ImmutableList.copyOf(resolutionSteps);
        this.resultVar = resultVar;

        this.accepting = accepting;
        this.transitions = ImmutableMap.copyOf(transitions);
    }

    public ImmutableList<RStep<L>> resolutionSteps() {
        return resolutionSteps;
    }

    public RVar resultVar() {
        return resultVar;
    }

    public boolean isAccepting() {
        return accepting;
    }

    public @Nullable String transitionStateId(L label) {
        return transitions.get(label);
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
        @SuppressWarnings("unchecked") final State<L> other = (State<L>) obj;
        return Objects.equals(resolutionSteps, other.resolutionSteps) && Objects.equals(resultVar, other.resultVar);
    }

    private volatile int hashCode;

    @Override public int hashCode() {
        int result = hashCode;
        if(result == 0) {
            result = Objects.hash(resolutionSteps, resultVar);
            hashCode = result;
        }
        return result;
    }

}
