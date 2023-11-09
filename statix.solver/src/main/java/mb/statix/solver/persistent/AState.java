package mb.statix.solver.persistent;

import static mb.nabl2.terms.build.TermBuild.B;

import jakarta.annotation.Nullable;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.tuple.Tuple2;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.terms.IAttachments;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.stratego.TermIndex;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.Unifiers;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.reference.ScopeGraph;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.IState;
import mb.statix.solver.ITermProperty;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class AState implements IState.Immutable {

    @Override @Value.Default public String resource() {
        return "";
    }

    @Override public IState.Immutable add(IState.Immutable other) {
        final Set.Immutable<ITermVar> vars = vars().union(other.vars());
        final Set.Immutable<Scope> scopes = scopes().union(other.scopes());
        final IUniDisunifier.Immutable unifier;
        try {
            unifier = unifier().uniDisunify(other.unifier()).map(IUniDisunifier.Result::unifier)
                    .orElseThrow(() -> new IllegalArgumentException("Cannot merge unifiers."));
        } catch(OccursException e) {
            throw new IllegalArgumentException("Cannot merge unifiers.");
        }
        final IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph = scopeGraph().addAll(other.scopeGraph());
        final Map.Immutable<Tuple2<TermIndex, ITerm>, ITermProperty> termProperties =
                termProperties().__putAll(other.termProperties());
        // @formatter:off
        return State.builder().from(this)
            .__vars(vars)
            .__scopes(scopes)
            .unifier(unifier)
            .scopeGraph(scopeGraph)
            .termProperties(termProperties)
            .build();
        // @formatter:on
    }

    @Override public Immutable subState() {
        State self = (State) this;
        return self.with__scopes(CapsuleUtil.immutableSet()).with__vars(CapsuleUtil.immutableSet());
    }

    // --- variables ---

    @Value.Default int __varCounter() {
        return 0;
    }

    @Value.Default Set.Immutable<ITermVar> __vars() {
        return CapsuleUtil.immutableSet();
    }

    @Override public Tuple2<ITermVar, IState.Immutable> freshVar(ITermVar var) {
        return freshVar(var.getName(), var.getAttachments());
    }

    @Override public Tuple2<ITermVar, IState.Immutable> freshWld() {
        return freshVar("_", null);
    }

    private Tuple2<ITermVar, IState.Immutable> freshVar(String name, @Nullable IAttachments attachments) {
        final int i = __varCounter() + 1;
        final String newName = name.replace('-', '_') + "-" + i;
        final ITermVar newVar = B.newVar(resource(), newName, attachments);
        final Set.Immutable<ITermVar> vars = __vars().__insert(newVar);
        return Tuple2.of(newVar, State.builder().from(this).__varCounter(i).__vars(vars).build());
    }

    @Override public Set.Immutable<ITermVar> vars() {
        return __vars();
    }

    // --- scopes ---

    @Value.Default int __scopeCounter() {
        return 0;
    }

    @Value.Default Set.Immutable<Scope> __scopes() {
        return CapsuleUtil.immutableSet();
    }

    @Override public Tuple2<Scope, IState.Immutable> freshScope(String base) {
        final int i = __scopeCounter() + 1;
        final String name = base.replace('-', '_') + "-" + i;
        final Scope scope = Scope.of(resource(), name);
        final Set.Immutable<Scope> scopes = __scopes().__insert(scope);
        return Tuple2.of(scope, State.builder().from(this).__scopeCounter(i).__scopes(scopes).build());
    }

    @Override public Set.Immutable<Scope> scopes() {
        return __scopes();
    }

    // --- solution ---

    @Value.Parameter @Override public abstract IUniDisunifier.Immutable unifier();

    @Value.Parameter @Override public abstract IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph();

    @Value.Parameter @Override public abstract Map.Immutable<Tuple2<TermIndex, ITerm>, ITermProperty> termProperties();

    public static State of() {
        return State.of(Unifiers.Immutable.of(), ScopeGraph.Immutable.of(), Map.Immutable.of());
    }

}
