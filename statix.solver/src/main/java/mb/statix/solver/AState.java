package mb.statix.solver;

import static mb.nabl2.terms.build.TermBuild.B;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import io.usethesource.capsule.Set;
import mb.nabl2.scopegraph.terms.ImmutableScope;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.reference.ScopeGraph;
import mb.statix.spec.Spec;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class AState {

    @Value.Parameter public abstract Spec spec();

    // --- variables ---

    @Value.Default int __varCounter() {
        return 0;
    }

    @Value.Default Set.Immutable<ITermVar> __vars() {
        return Set.Immutable.of();
    }

    public Tuple2<ITermVar, State> freshVar(String base) {
        final int i = __varCounter() + 1;
        final String name = base.replaceAll("-", "_") + "-" + i;
        final ITermVar var = B.newVar("", name);
        final Set.Immutable<ITermVar> vars = __vars().__insert(var);
        return ImmutableTuple2.of(var, State.builder().from(this).__varCounter(i).__vars(vars).build());
    }

    public Set.Immutable<ITermVar> vars() {
        return __vars();
    }

    // --- scopes ---

    @Value.Default int __scopeCounter() {
        return 0;
    }

    @Value.Default Set.Immutable<ITerm> __scopes() {
        return Set.Immutable.of();
    }

    public Tuple2<ITerm, State> freshScope(String base) {
        final int i = __scopeCounter() + 1;
        final String name = base.replaceAll("-", "_") + "-" + i;
        final ITerm scope = ImmutableScope.of("", name);
        final Set.Immutable<ITerm> scopes = __scopes().__insert(scope);
        return ImmutableTuple2.of(scope, State.builder().from(this).__scopeCounter(i).__scopes(scopes).build());
    }

    public Set.Immutable<ITerm> scopes() {
        return __scopes();
    }

    // --- solution ---

    @Value.Default public IUnifier.Immutable unifier() {
        return PersistentUnifier.Immutable.of();
    }

    @Value.Default public IScopeGraph.Immutable<ITerm, ITerm, ITerm> scopeGraph() {
        return ScopeGraph.Immutable.of(spec().labels(), spec().endOfPath(), spec().relations().keySet());
    }

}