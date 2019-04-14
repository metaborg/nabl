package mb.statix.solver;

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
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.reference.ScopeGraph;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.spec.Spec;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class AState {

    @Value.Parameter public abstract Spec spec();

    @Value.Default public String resource() {
        return "";
    }

    public State clearVarsAndScopes() {
        return State.copyOf(this).with__vars(Set.Immutable.of()).with__scopes(Set.Immutable.of());
    }

    public State retainVarsAndClearScopes(Set.Immutable<ITermVar> vars) {
        return State.copyOf(this).with__vars(Set.Immutable.intersect(vars(), vars)).with__scopes(Set.Immutable.of());
    }

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
        final ITermVar var = B.newVar(resource(), name);
        final Set.Immutable<ITermVar> vars = __vars().__insert(var);
        return ImmutableTuple2.of(var, State.builder().from(this).__varCounter(i).__vars(vars).build());
    }

    public Tuple2<ITermVar, State> freshRigidVar(String base) {
        final int i = __varCounter() + 1;
        final String name = base.replaceAll("-", "_") + "-" + i;
        final ITermVar var = B.newVar(resource(), name);
        // same as freshVar, but do not add to vars
        return ImmutableTuple2.of(var, State.builder().from(this).__varCounter(i).build());
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
        final ITerm scope = Scope.of(resource(), name);
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