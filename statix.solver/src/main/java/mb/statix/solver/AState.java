package mb.statix.solver;

import static mb.nabl2.terms.build.TermBuild.B;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.scopegraph.terms.ImmutableScope;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.reference.ScopeGraph;
import mb.statix.spec.ASpec;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class AState {

    @Value.Parameter public abstract ASpec spec();

    @Value.Default int varCounter() {
        return 0;
    }

    public Tuple2<ITermVar, State> freshVar(String base) {
        final int i = varCounter() + 1;
        final String name = base.replaceAll("-", "_") + "-" + i;
        final ITermVar var = B.newVar("", name);
        return ImmutableTuple2.of(var, State.copyOf(this).withVarCounter(i));
    }

    @Value.Default int scopeCounter() {
        return 0;
    }

    public Tuple2<ITerm, State> freshScope(String base) {
        final int i = scopeCounter() + 1;
        final String name = base.replaceAll("-", "_") + "-" + i;
        final ITerm scope = ImmutableScope.of("", name);
        return ImmutableTuple2.of(scope, State.copyOf(this).withScopeCounter(i));
    }

    @Value.Default public IUnifier.Immutable unifier() {
        return PersistentUnifier.Immutable.of();
    }

    @Value.Default public IScopeGraph.Immutable<ITerm, ITerm, ITerm, ITerm> scopeGraph() {
        return ScopeGraph.Immutable.of(spec().labels(), spec().endOfPath(), spec().relations().keySet());
    }

    @Value.Default public boolean isErroneous() {
        return false;
    }

}