package mb.scopegraph.resolution;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import io.usethesource.capsule.Map;
import java.util.Objects;

import jakarta.annotation.Nullable;

public class State<L> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<RStep<L>> resolutionSteps;
    private final RVar resultVar;

    private final boolean accepting;
    private final Map.Immutable<L, String> transitions;

    public State(Collection<RStep<L>> resolutionSteps, RVar resultVar, boolean accepting, Map.Immutable<L, String> transitions) {
        this.resolutionSteps = Collections.unmodifiableList(new ArrayList<>(resolutionSteps));
        this.resultVar = resultVar;

        this.accepting = accepting;
        this.transitions = transitions;
    }

    public List<RStep<L>> resolutionSteps() {
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
