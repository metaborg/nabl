package mb.nabl2.spoofax.analysis;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.solver.ISolution;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class FinalResult {

    @Value.Parameter public abstract ISolution solution();

}