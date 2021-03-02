package mb.statix.concurrent.p_raffrayi.diff;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.util.CapsuleUtil;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.ICompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.scopegraph.diff.BiMap;
import mb.statix.scopegraph.diff.BiMap.Immutable;
import mb.statix.scopegraph.diff.Edge;
import mb.statix.scopegraph.diff.ScopeGraphDiff;

public class AddingDiffer<S, L, D> implements IScopeGraphDiffer<S, L, D> {

    private final AtomicInteger pendingResults = new AtomicInteger(0);
    private IScopeGraphDifferContext<S, L, D> context;

    private Map.Transient<S, Optional<D>> addedScopes = CapsuleUtil.transientMap();
    private Set.Transient<Edge<S, L>> addedEdges = CapsuleUtil.transientSet();

    private Set.Transient<S> seenScopes = CapsuleUtil.transientSet();

    private final ICompletableFuture<ScopeGraphDiff<S, L, D>> diffResult = new CompletableFuture<>();

    public AddingDiffer(IScopeGraphDifferContext<S, L, D> context) {
        this.context = context;
    }

    @Override public IFuture<ScopeGraphDiff<S, L, D>> diff(List<S> currentRootScopes, List<S> previousRootScopes) {
        // TODO Auto-generated method stub
        currentRootScopes.forEach(this::addScope);

        tryFinalize();
        return diffResult;
    }

    // TODO rework to max recursion depth (fuel) and worklist
    private void addScope(S scope) {
        if(seenScopes.contains(scope)) {
            tryFinalize();
            return;
        }
        seenScopes.__insert(scope);
        pendingResults.incrementAndGet();
        context.currentDatum(scope).thenAccept(d -> {
            pendingResults.decrementAndGet();
            addedScopes.__put(scope, d);
            d.ifPresent(datum -> {
                context.getCurrentScopes(datum).forEach(this::addScope);
            });
            tryFinalize();
        });
        pendingResults.incrementAndGet();
        context.labels(scope).thenAccept(lbls -> {
            pendingResults.decrementAndGet();
            lbls.forEach(lbl -> {
                pendingResults.incrementAndGet();
                context.getCurrentEdges(scope, lbl).thenAccept(edges -> {
                    pendingResults.incrementAndGet();
                    edges.forEach(this::addScope);
                    tryFinalize();
                });
                // TODO cascade edges and scopes
                // TODO finalize at correct moment
            });
            tryFinalize();
        });
        tryFinalize();
    }

    private void tryFinalize() {
        if(pendingResults.get() == 0) {
            // @formatter:off
            diffResult.complete(new ScopeGraphDiff<>(
                BiMap.Immutable.of(),
                BiMap.Immutable.of(),
                addedScopes.freeze(),
                addedEdges.freeze(),
                CapsuleUtil.immutableMap(),
                CapsuleUtil.immutableSet()));
            // @formatter:on
        }
    }

    @Override public IFuture<Optional<S>> match(S previousScope) {
        throw new UnsupportedOperationException("There can be no previous scopes for an added unit.");
    }

    @Override public boolean matchScopes(Immutable<S> scopes) {
        throw new UnsupportedOperationException("There can be no previous scopes for an added unit.");
    }

    @Override public void typeCheckerFinished() {
        // TODO Wait for finalization?

    }

}
