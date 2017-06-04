package org.metaborg.meta.nabl2.symbolic;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.terms.ITerm;

import io.usethesource.capsule.Set;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class SymbolicConstraints implements ISymbolicConstraints {

    @Value.Parameter public abstract Set.Immutable<ITerm> getFacts();

    @Value.Parameter public abstract Set.Immutable<ITerm> getGoals();

    public static SymbolicConstraints of() {
        return ImmutableSymbolicConstraints.of(Set.Immutable.of(), Set.Immutable.of());
    }

}