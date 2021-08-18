package mb.p_raffrayi.impl;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.collection.MultiSet;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

import io.usethesource.capsule.Set;
import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.reference.EdgeOrData;

@Value.Immutable
@Serial.Version(42)
public abstract class AStateCapture<S, L, D, T> {

    @Value.Parameter public abstract Set.Immutable<S> scopes();

    @Value.Parameter public abstract IScopeGraph.Immutable<S, L, D> scopeGraph();

    @Value.Parameter public abstract Multiset<S> unInitializedScopes();

    @Value.Parameter public abstract Multiset<S> openScopes();

    @Value.Parameter public abstract Multimap<S, EdgeOrData<L>> openEdges();

    @Value.Parameter public abstract MultiSet.Immutable<String> scopeNameCounters();

    @Value.Parameter public abstract T typeCheckerState();

    public boolean isOpen(S scope, EdgeOrData<L> label) {
        if(!scopes().contains(scope)) {
            throw new IllegalStateException("Scope " + scope + " is not part of this capture.");
        }
        return unInitializedScopes().contains(scope) || openScopes().contains(scope)
                || openEdges().containsEntry(scope, label);
    }

}
