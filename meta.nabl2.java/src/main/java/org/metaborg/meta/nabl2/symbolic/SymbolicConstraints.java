package org.metaborg.meta.nabl2.symbolic;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.util.functions.Function1;

import io.usethesource.capsule.Set;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class SymbolicConstraints implements ISymbolicConstraints {

    @Value.Parameter public abstract Set.Immutable<ITerm> getFacts();

    @Value.Parameter public abstract Set.Immutable<ITerm> getGoals();

    public SymbolicConstraints map(Function1<ITerm, ITerm> mapper) {
        Set.Transient<ITerm> facts = Set.Transient.of();
        getFacts().stream().forEach(f -> facts.__insert(mapper.apply(f)));

        Set.Transient<ITerm> goals = Set.Transient.of();
        getGoals().stream().forEach(g -> goals.__insert(mapper.apply(g)));

        return ImmutableSymbolicConstraints.of(facts.freeze(), goals.freeze());
    }

    public static SymbolicConstraints of() {
        return ImmutableSymbolicConstraints.of(Set.Immutable.of(), Set.Immutable.of());
    }

}