package org.metaborg.meta.nabl2.solver;

import java.util.Set;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.terms.ITerm;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class SymbolicConstraints implements ISymbolicConstraints {

    @Value.Parameter public abstract Set<ITerm> getFacts();

    @Value.Parameter public abstract Set<ITerm> getGoals();
    
}