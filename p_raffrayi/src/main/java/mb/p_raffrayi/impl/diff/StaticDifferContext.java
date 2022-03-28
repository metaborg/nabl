package mb.p_raffrayi.impl.diff;

import java.util.Optional;

import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;

import io.usethesource.capsule.Set;
import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.ScopeGraphUtil;

public class StaticDifferContext<S, L, D> implements IDifferContext<S, L, D> {

    private final IScopeGraph.Immutable<S, L, D> scopeGraph;
    private final java.util.Set<S> scopes;

    private final Set.Immutable<L> edgeLabels;

    private final IDifferDataOps<D> dataOps;

    public StaticDifferContext(IScopeGraph.Immutable<S, L, D> scopeGraph, java.util.Set<S> scopes,
            Set.Immutable<L> edgeLabels, IDifferDataOps<D> dataOps) {
        this.scopeGraph = scopeGraph;
        this.scopes = scopes;
        this.edgeLabels = edgeLabels;
        this.dataOps = dataOps;
    }

    @Override public IFuture<Iterable<S>> getEdges(S scope, L label) {
        return CompletableFuture.completedFuture(scopeGraph.getEdges(scope, label));
    }

    @Override public IFuture<Set.Immutable<L>> labels(S scope) {
        return CompletableFuture.completedFuture(edgeLabels);
    }

    @Override public IFuture<Optional<D>> datum(S scope) {
        return CompletableFuture.completedFuture(rawDatum(scope));
    }

    @Override public Optional<D> rawDatum(S scope) {
        return scopeGraph.getData(scope).map(dataOps::getExternalRepresentation);
    }

    @Override public boolean available(S scope) {
        return scopes.contains(scope);
    }

    @Override public String toString() {
        return "StaticDifferContext:\n" + ScopeGraphUtil.toString(scopeGraph, dataOps::getExternalRepresentation);
    }

}
