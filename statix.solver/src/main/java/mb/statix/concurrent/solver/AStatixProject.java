package mb.statix.concurrent.solver;

import java.util.Map;
import java.util.Optional;

import org.immutables.value.Value;

import mb.nabl2.terms.ITerm;
import mb.statix.concurrent.p_raffrayi.impl.IInitialState;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.spec.Rule;

@Value.Immutable
public abstract class AStatixProject implements IStatixProject {

    @Value.Parameter @Override public abstract String resource();

    @Value.Parameter @Override public abstract Optional<Rule> rule();

    @Value.Parameter @Override public abstract Map<String, IStatixGroup> groups();

    @Value.Parameter @Override public abstract Map<String, IStatixUnit> units();

    @Value.Parameter @Override public abstract Map<String, IStatixLibrary> libraries();

    @Value.Parameter @Override public abstract IInitialState<Scope, ITerm, ITerm, ProjectResult> initialState();

    @Override public String toString() {
        return "StatixProject@" + System.identityHashCode(this);
    }

}