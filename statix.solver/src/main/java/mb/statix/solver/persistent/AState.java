package mb.statix.solver.persistent;

import static mb.nabl2.terms.build.TermBuild.B;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.stratego.TermIndex;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.IUnifier.Immutable.Result;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.Unifiers;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation3;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.reference.ScopeGraph;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IState;
import mb.statix.spec.Spec;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class AState implements IState.Immutable {

    @Override @Value.Parameter public abstract Spec spec();

    @Override @Value.Default public String resource() {
        return "";
    }

    @Override public IState.Immutable add(IState.Immutable other) {
        final Set.Immutable<ITermVar> vars = vars().union(other.vars());
        final Set.Immutable<Scope> scopes = scopes().union(other.scopes());
        final IUnifier.Immutable unifier;
        try {
            unifier = unifier().unify(other.unifier()).map(Result::unifier)
                    .orElseThrow(() -> new IllegalArgumentException("Cannot merge unifiers."));
        } catch(OccursException e) {
            throw new IllegalArgumentException("Cannot merge unifiers.");
        }
        final IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph = scopeGraph().addAll(other.scopeGraph());
        // @formatter:off
        return State.builder().from(this)
            .__vars(vars)
            .__scopes(scopes)
            .unifier(unifier)
            .scopeGraph(scopeGraph)
            .build();
        // @formatter:on
    }

    // --- variables ---

    @Value.Default int __varCounter() {
        return 0;
    }

    @Value.Default Set.Immutable<ITermVar> __vars() {
        return Set.Immutable.of();
    }

    @Override public Tuple2<ITermVar, IState.Immutable> freshVar(String base) {
        final int i = __varCounter() + 1;
        final String name = base.replaceAll("-", "_") + "-" + i;
        final ITermVar var = B.newVar(resource(), name);
        final Set.Immutable<ITermVar> vars = __vars().__insert(var);
        return ImmutableTuple2.of(var, State.builder().from(this).__varCounter(i).__vars(vars).build());
    }

    @Override public Set.Immutable<ITermVar> vars() {
        return __vars();
    }

    // --- scopes ---

    @Value.Default int __scopeCounter() {
        return 0;
    }

    @Value.Default Set.Immutable<Scope> __scopes() {
        return Set.Immutable.of();
    }

    @Override public Tuple2<Scope, IState.Immutable> freshScope(String base) {
        final int i = __scopeCounter() + 1;
        final String name = base.replaceAll("-", "_") + "-" + i;
        final Scope scope = Scope.of(resource(), name);
        final Set.Immutable<Scope> scopes = __scopes().__insert(scope);
        return ImmutableTuple2.of(scope, State.builder().from(this).__scopeCounter(i).__scopes(scopes).build());
    }

    @Override public Set.Immutable<Scope> scopes() {
        return __scopes();
    }

    // --- solution ---

    @Override @Value.Default public IUnifier.Immutable unifier() {
        return Unifiers.Immutable.of();
    }

    @Override @Value.Default public IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph() {
        return ScopeGraph.Immutable.of(spec().edgeLabels(), spec().relationLabels(), spec().noRelationLabel());
    }

    @Override @Value.Default public IRelation3.Immutable<TermIndex, ITerm, ITerm> termProperties() {
        return HashTrieRelation3.Immutable.of();
    }

}
