package mb.statix.solver;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.Multimap;

import mb.nabl2.terms.unification.IUnifier;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class AState {

    @Value.Parameter public abstract Multimap<String, Rule> rules();

    @Value.Parameter public abstract int fresh();

    @Value.Parameter public abstract IUnifier.Immutable unifier();

    @Value.Parameter public abstract boolean isErroneous();

}