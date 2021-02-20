package mb.statix.concurrent.p_raffrayi.impl;

import java.util.Optional;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.statix.concurrent.p_raffrayi.IUnitResult;

@Value.Immutable
@Serial.Version(42L)
public abstract class AInitialState<S, L, D, R> implements IInitialState<S, L, D, R> {

    @Override @Value.Parameter public abstract boolean changed();

    @Override @Value.Parameter public abstract Optional<IUnitResult<S, L, D, R>> previousResult();

}
