package mb.statix.concurrent;

import java.util.Optional;

import org.immutables.value.Value;

import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.Scope;
import mb.statix.spec.Rule;
import mb.p_raffrayi.impl.IInitialState;

@Value.Immutable
public abstract class AStatixUnit implements IStatixUnit {

    @Value.Parameter @Override public abstract String resource();

    @Value.Parameter @Override public abstract Optional<Rule> rule();

    @Value.Parameter @Override public abstract IInitialState<Scope, ITerm, ITerm, UnitResult> initialState();

    @Override public String toString() {
        return "StatixUnit@" + System.identityHashCode(this);
    }

}