package mb.statix.concurrent.solver;

import java.util.Optional;

import org.immutables.value.Value;

import mb.nabl2.terms.ITerm;
import mb.statix.concurrent.p_raffrayi.impl.IInitialState;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.spec.Rule;

@Value.Immutable
public abstract class AStatixUnit implements IStatixUnit {

    @Value.Parameter @Override public abstract String resource();

    @Value.Parameter @Override public abstract Optional<Rule> rule();

    @Value.Parameter @Override public abstract IInitialState<Scope, ITerm, ITerm, UnitResult> initialState();

    @Override public String toString() {
        return "StatixUnit@" + System.identityHashCode(this);
    }

}