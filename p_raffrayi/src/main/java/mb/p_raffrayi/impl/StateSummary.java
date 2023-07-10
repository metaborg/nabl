package mb.p_raffrayi.impl;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.Action0;
import org.metaborg.util.functions.Function0;

import io.usethesource.capsule.Set;

public class StateSummary<S, L, D> {

    public enum State {
        ACTIVE,
        UNKNOWN,
        RELEASED
    }

    private final State state;
    private final IProcess<S, L, D> self;
    private final Set.Immutable<IProcess<S, L, D>> dependencies;

    private StateSummary(State state, IProcess<S, L, D> self, java.util.Set<IProcess<S, L, D>> dependencies) {
        this.state = state;
        this.self = self;
        this.dependencies = CapsuleUtil.toSet(dependencies);
    }

    public State getState() {
        return state;
    }

    public IProcess<S, L, D> getSelf() {
        return self;
    }

    public Set.Immutable<IProcess<S, L, D>> getDependencies() {
        return dependencies;
    }

    public <T> T match(Function0<T> onRestart, Function0<T> onRelease, Function0<T> onReleased) {
        switch(state) {
            case ACTIVE:
                return onRestart.apply();
            case UNKNOWN:
                return onRelease.apply();
            case RELEASED:
                return onReleased.apply();
            default:
                throw new IllegalStateException("Unknown state" + state + ".");
        }
    }

    public void accept(Action0 onRestart, Action0 onRelease, Action0 onReleased) {
        switch(state) {
            case ACTIVE:
                onRestart.apply();
                break;
            case UNKNOWN:
                onRelease.apply();
                break;
            case RELEASED:
                onReleased.apply();
                break;
            default:
                throw new IllegalStateException("Unknown state" + state + ".");
        }
    }

    @Override public String toString() {
        return "StateSummary{state=" + state + ", self=" + self + ", dependencies=" + dependencies + "}";
    }

    public static <S, L, D> StateSummary<S, L, D> restart(IProcess<S, L, D> self, java.util.Set<IProcess<S, L, D>> dependencies) {
        return new StateSummary<>(State.ACTIVE, self, dependencies);
    }

    public static <S, L, D> StateSummary<S, L, D> release(IProcess<S, L, D> self, java.util.Set<IProcess<S, L, D>> dependencies) {
        return new StateSummary<>(State.UNKNOWN, self, dependencies);
    }

    public static <S, L, D> StateSummary<S, L, D> released(IProcess<S, L, D> self, java.util.Set<IProcess<S, L, D>> dependencies) {
        return new StateSummary<>(State.RELEASED, self, dependencies);
    }

}
