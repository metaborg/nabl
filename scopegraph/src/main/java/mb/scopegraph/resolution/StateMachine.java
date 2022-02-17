package mb.scopegraph.resolution;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ImmutableMap;

public class StateMachine<L> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ImmutableMap<String, State<L>> states;

    private final State<L> initial;

    public StateMachine(Map<String, State<L>> states, State<L> initial) {
        this.states = ImmutableMap.copyOf(states);
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
