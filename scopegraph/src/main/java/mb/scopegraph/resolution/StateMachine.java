package mb.scopegraph.resolution;

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

import io.usethesource.capsule.Map;

public class StateMachine<L> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map.Immutable<String, State<L>> states;

    private final State<L> initial;

    public StateMachine(Map.Immutable<String, State<L>> states, State<L> initial) {
        this.states = states;
        this.initial = initial;
    }

    public State<L> initial() {
        return initial;
    }

    public State<L> state(String name) {
        final State<L> result = states.get(name);
        if(result == null) {
            throw new IllegalStateException("Unknown state '" + name + "'.");
        }
        return result;
    }

    public Set<String> stateIds() {
        return states.keySet();
    }

    @Override public String toString() {
        return "StateMachine{states=" + states + ", initial=" + initial + "}";
    }

    @Override public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        }
        if(obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        @SuppressWarnings("unchecked") final StateMachine<L> other = (StateMachine<L>) obj;
        return Objects.equals(states, other.states) && Objects.equals(initial, other.initial);

    }

    @Override public int hashCode() {
        return Objects.hash(states, initial);
    }

}
