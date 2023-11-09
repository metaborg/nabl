package mb.p_raffrayi.impl;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.collection.MultiSet;
import org.metaborg.util.collection.MultiSetMap;

import io.usethesource.capsule.Set;
import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.reference.EdgeOrData;

@Value.Immutable
@Serial.Version(42)
public abstract class AStateCapture<S, L, D, T> {

    @Value.Parameter public abstract Set.Immutable<S> scopes();

    @Value.Parameter public abstract IScopeGraph.Immutable<S, L, D> scopeGraph();

    @Value.Parameter public abstract MultiSet.Immutable<S> unInitializedScopes();

    @Value.Parameter public abstract MultiSet.Immutable<S> openScopes();

    @Value.Parameter public abstract MultiSetMap.Immutable<S, EdgeOrData<L>> openEdges();

    @Value.Parameter public abstract MultiSet.Immutable<String> scopeNameCounters();

    @Value.Parameter public abstract Set.Immutable<String> usedStableScopes();

    @Value.Parameter public abstract T typeCheckerState();

    public boolean isOpen(S scope, EdgeOrData<L> label) {
        if(!scopes().contains(scope)) {
            throw new IllegalStateException("Scope " + scope + " is not part of this capture.");
        }
        return unInitializedScopes().contains(scope) || openScopes().contains(scope)
                || openEdges().contains(scope, label);
    }

}
