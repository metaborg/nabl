package mb.p_raffrayi.impl;

import java.util.Optional;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.p_raffrayi.IUnitResult;

@Value.Immutable
@Serial.Version(42L)
public abstract class AInitialState<S, L, D, R> implements IInitialState<S, L, D, R> {

    private static final IInitialState<?, ?, ?, ?> ADDED = InitialState.of(true, Optional.empty());

    @Override @Value.Parameter public abstract boolean changed();

    @Override @Value.Parameter public abstract Optional<IUnitResult<S, L, D, R>> previousResult();

    ////////////////////////////////////////
    // Factory methods for change options
    ////////////////////////////////////////

    @SuppressWarnings("unchecked") public static <S, L, D, R> IInitialState<S, L, D, R> added() {
        return (IInitialState<S, L, D, R>) AInitialState.ADDED;
    }

    public static <S, L, D, R> IInitialState<S, L, D, R> cached(IUnitResult<S, L, D, R> previousResult) {
        return InitialState.of(false, Optional.of(previousResult));
    }

    public static <S, L, D, R> IInitialState<S, L, D, R> changed(IUnitResult<S, L, D, R> previousResult) {
        return InitialState.of(true, Optional.of(previousResult));
    }
}
