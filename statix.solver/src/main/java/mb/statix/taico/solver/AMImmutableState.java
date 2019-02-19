package mb.statix.taico.solver;

import static mb.nabl2.terms.build.TermBuild.B;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.statix.spec.Spec;
import mb.statix.taico.module.IModule;
import mb.statix.taico.scopegraph.IOwnableScope;
import mb.statix.taico.scopegraph.ModuleScopeGraph;
import mb.statix.taico.scopegraph.OwnableScope;
import mb.statix.util.Capsules;

@Value.Immutable
@Serial.Version(value = 2L)
public abstract class AMImmutableState {
    Set.Immutable<IOwnableScope> canExtend;
    
    @Value.Parameter public abstract IModule owner();
    
    @Value.Parameter public abstract Spec spec();

    // --- variables ---

    @Value.Default int __varCounter() {
        return 0;
    }

    @Value.Default Set.Immutable<ITermVar> __vars() {
        return Set.Immutable.of();
    }

    public Tuple2<ITermVar, MImmutableState> freshVar(String base) {
        final int i = __varCounter() + 1;
        final String name = base.replaceAll("-", "_") + "-" + i;
        final ITermVar var = B.newVar("", name);
        final Set.Immutable<ITermVar> vars = __vars().__insert(var);
        return ImmutableTuple2.of(var, MImmutableState.builder().from(this).__varCounter(i).__vars(vars).build());
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

    public Tuple2<ITerm, MImmutableState> freshScope(String base) {
        final int i = __scopeCounter() + 1;
        final String name = base.replaceAll("-", "_") + "-" + i;
        final ITerm scope = new OwnableScope(owner(), "", name);
        final Set.Immutable<ITerm> scopes = __scopes().__insert(scope);
        return ImmutableTuple2.of(scope, MImmutableState.builder().from(this).__scopeCounter(i).__scopes(scopes).build());
    }

    public Set.Immutable<ITerm> scopes() {
        return __scopes();
    }

    // --- solution ---

    @Value.Default public IUnifier.Immutable unifier() {
        return PersistentUnifier.Immutable.of();
    }

    @Value.Default public ModuleScopeGraph scopeGraph() {
        return new ModuleScopeGraph(
                owner(),
                Capsules.newSet(spec().labels()),
                spec().endOfPath(),
                Capsules.newSet(spec().relations().keySet()),
                canExtend);
    }

}
