package mb.statix.constraints.compiled;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ImmutableMap;

public class StateMachine implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ImmutableMap<String, State> states;

    private final State initial;

    public StateMachine(Map<String, State> states, State initial) {
        this.states = ImmutableMap.copyOf(states);
        this.initial = initial;
    }

    public State initial() {
        return initial;
    }

    public State state(String name) {
        final State result = states.get(name);
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
        final StateMachine other = (StateMachine) obj;
        return Objects.equals(states, other.states) && Objects.equals(initial, other.initial);

    }

    @Override public int hashCode() {
        return Objects.hash(states, initial);
    }

}
