package mb.p_raffrayi.impl.diff;

import java.util.Optional;

import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;

import io.usethesource.capsule.Set;
import mb.scopegraph.oopsla20.IScopeGraph;

public class StaticDifferContext<S, L, D> implements IDifferContext<S, L, D> {

    private final IScopeGraph.Immutable<S, L, D> scopeGraph;

    public StaticDifferContext(IScopeGraph.Immutable<S, L, D> scopeGraph) {
        this.scopeGraph = scopeGraph;
    }

    @Override public IFuture<Iterable<S>> getEdges(S scope, L label) {
        return CompletableFuture.completedFuture(scopeGraph.getEdges(scope, label));
    }

    @Override public IFuture<Set.Immutable<L>> labels(S scope) {
        return CompletableFuture.completedFuture(scopeGraph.getLabels());
    }

    @Override public IFuture<Optional<D>> datum(S scope) {
        return CompletableFuture.completedFuture(scopeGraph.getData(scope));
    }

}
