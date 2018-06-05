package mb.statix.spec;

import java.util.Map;
import java.util.Set;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.Multimap;

import mb.nabl2.terms.ITerm;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class ASpec {

    @Value.Parameter public abstract Multimap<String, Rule> rules();

    @Value.Parameter public abstract Set<ITerm> labels();

    @Value.Parameter public abstract ITerm endOfPath();

    @Value.Parameter public abstract Map<ITerm, Type> relations();

}