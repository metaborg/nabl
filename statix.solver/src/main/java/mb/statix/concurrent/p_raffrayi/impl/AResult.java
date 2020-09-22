package mb.statix.concurrent.p_raffrayi.impl;

import java.util.Map;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.statix.concurrent.p_raffrayi.IResult;
import mb.statix.concurrent.p_raffrayi.IUnitResult;

@Value.Immutable
@Serial.Version(42L)
public abstract class AResult<S, L, D, R> implements IResult<S, L, D, R> {

    @Value.Parameter @Override public abstract Map<String, IUnitResult<S, L, D, R>> unitResults();

}
