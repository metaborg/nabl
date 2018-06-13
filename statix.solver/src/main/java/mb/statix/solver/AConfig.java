package mb.statix.solver;

import java.util.Set;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class AConfig {

    @Value.Parameter public abstract State state();

    @Value.Parameter public abstract Set<IConstraint> constraints();

    @Value.Parameter public abstract Completeness completeness();

}