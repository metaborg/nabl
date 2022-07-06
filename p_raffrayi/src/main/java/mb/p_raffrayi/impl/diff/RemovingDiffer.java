package mb.p_raffrayi.impl.diff;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;

import com.google.common.collect.Multimap;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.IScopeGraph.Immutable;
import mb.scopegraph.oopsla20.diff.BiMap;
import mb.scopegraph.oopsla20.diff.Edge;
import mb.scopegraph.oopsla20.diff.ScopeGraphDiff;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.scopegraph.patching.IPatchCollection;

public class RemovingDiffer<S, L, D> implements IScopeGraphDiffer<S, L, D> {

    private IScopeGraph.Immutable<S, L, D> previousGraph;
    private final IDifferOps<S, L, D> differOps;

    public RemovingDiffer(IScopeGraph.Immutable<S, L, D> previousGraph, IDifferOps<S, L, D> differOps) {
        this.previousGraph = previousGraph;
        this.differOps = differOps;
    }

    @Override public IFuture<ScopeGraphDiff<S, L, D>> diff(List<S> currentRootScopes, List<S> previousRootScopes) {
        final Map.Transient<S, D> removedScopes = CapsuleUtil.transientMap();
        final Set.Transient<Edge<S, L>> removedEdges = CapsuleUtil.transientSet();

        final Queue<S> queue = new LinkedList<>(previousRootScopes);

        while(!queue.isEmpty()) {
            final S scope = queue.remove();
            final D datum = previousGraph.getData(scope).orElse(differOps.embed(scope));
            removedScopes.__put(scope, datum);

            for(L label : previousGraph.getLabels()) {
                for(S tgt : previousGraph.getEdges(scope, label)) {
                    removedEdges.__insert(new Edge<>(scope, label, tgt));
                    if(!removedScopes.containsKey(tgt)) {
                        queue.add(tgt);
                    }
                }
            }
        }

        // @formatter:off
        return CompletableFuture.completedFuture(new ScopeGraphDiff<>(
            BiMap.Immutable.of(),
            BiMap.Immutable.of(),
            CapsuleUtil.immutableMap(),
            CapsuleUtil.immutableSet(),
            removedScopes.freeze(),
            removedEdges.freeze()
        ));
        // @formatter:on
    }

    @Override public IFuture<ScopeGraphDiff<S, L, D>> diff(Immutable<S, L, D> initiallyMatchedGraph,
            Collection<S> scopes, Collection<S> sharedScopes, IPatchCollection.Immutable<S> patches, Collection<S> openScopes,
            Multimap<S, EdgeOrData<L>> openEdges) {
        throw new IllegalStateException("Removing differ cannot be used with initial scopegraph.");
    }

    @Override public boolean matchScopes(BiMap.Immutable<S> scopes) {
        // Method is used for matching shared scopes, which is silently allowed here.
        return true;
    }

    @Override public void typeCheckerFinished() {
    }

    @Override public IFuture<Optional<S>> match(S previousScope) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override public IFuture<ScopeDiff<S, L, D>> scopeDiff(S previousScope, L label) {
        return CompletableFuture.completedFuture(ScopeDiff.<S, L, D>builder().build());
    }

}
