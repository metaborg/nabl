package mb.scopegraph.ecoop21;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import mb.scopegraph.resolution.State;
import mb.scopegraph.resolution.StateMachine;

public class StateLabelWf<L> implements LabelWf<L>, Serializable {

    private static final long serialVersionUID = 42L;

    private final StateMachine<L> stateMachine;
    private final State<L> state;

    public StateLabelWf(StateMachine<L> stateMachine, State<L> state) {
        this.stateMachine = stateMachine;
        this.state = state;
    }

    @Override public Optional<LabelWf<L>> step(L l) {
        return Optional.ofNullable(state.transitionStateId(l)).map(stateMachine::state)
                .map(st -> new StateLabelWf<>(stateMachine, st));
    }

    @Override public boolean accepting() {
        return state.isAccepting();
    }

    @Override public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        }
        if(obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        @SuppressWarnings("unchecked") final StateLabelWf<L> other = (StateLabelWf<L>) obj;
        return Objects.equals(state, other.state) && Objects.equals(stateMachine, other.stateMachine);
    }

    private volatile int hashCode;

    @Override public int hashCode() {
        int result = hashCode;
        if(result == 0) {
            result = Objects.hash(stateMachine, state);
            hashCode = result;
        }
        return result;
    }

}
