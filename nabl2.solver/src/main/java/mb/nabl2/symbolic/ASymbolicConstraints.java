package mb.nabl2.symbolic;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.functions.Function1;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class ASymbolicConstraints implements ISymbolicConstraints {

    @Override @Value.Parameter public abstract Set.Immutable<ITerm> getFacts();

    @Override @Value.Parameter public abstract Set.Immutable<ITerm> getGoals();

    @Override public SymbolicConstraints map(Function1<ITerm, ITerm> mapper) {
        Set.Transient<ITerm> facts = Set.Transient.of();
        getFacts().stream().forEach(f -> facts.__insert(mapper.apply(f)));

        Set.Transient<ITerm> goals = Set.Transient.of();
        getGoals().stream().forEach(g -> goals.__insert(mapper.apply(g)));

        return SymbolicConstraints.of(facts.freeze(), goals.freeze());
    }

    public static SymbolicConstraints of() {
        return SymbolicConstraints.of(Set.Immutable.of(), Set.Immutable.of());
    }

}