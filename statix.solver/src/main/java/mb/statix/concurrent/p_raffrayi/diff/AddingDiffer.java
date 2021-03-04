package mb.statix.concurrent.p_raffrayi.diff;

import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.Lists;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.util.CapsuleUtil;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.ICompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.scopegraph.diff.BiMap;
import mb.statix.scopegraph.diff.Edge;
import mb.statix.scopegraph.diff.ScopeGraphDiff;

public class AddingDiffer<S, L, D> implements IScopeGraphDiffer<S, L, D> {

    private final AtomicInteger pendingResults = new AtomicInteger(0);
    private IScopeGraphDifferContext<S, L, D> context;

    private Map.Transient<S, Optional<D>> addedScopes = CapsuleUtil.transientMap();
    private Set.Transient<Edge<S, L>> addedEdges = CapsuleUtil.transientSet();

    private Set.Transient<S> seenScopes = CapsuleUtil.transientSet();

    private final ICompletableFuture<ScopeGraphDiff<S, L, D>> diffResult = new CompletableFuture<>();

    private final Queue<S> worklist = Lists.newLinkedList();
    private final AtomicBoolean typeCheckerFinished = new AtomicBoolean();
    private final AtomicInteger nesting = new AtomicInteger(0);

    public AddingDiffer(IScopeGraphDifferContext<S, L, D> context) {
        this.context = context;
    }

    @Override public IFuture<ScopeGraphDiff<S, L, D>> diff(List<S> currentRootScopes, List<S> previousRootScopes) {
        // TODO Auto-generated method stub
        currentRootScopes.forEach(this::addScope);

        fixedpoint();
        return diffResult;
    }

    private void fixedpoint() {
        try {
            nesting.incrementAndGet();
            while(!worklist.isEmpty()) {
                addScope(worklist.poll());
            }
        } finally {
            nesting.decrementAndGet();
        }

        tryFinalize();
    }

    // TODO rework to max recursion depth (fuel) and worklist
    private void addScope(S scope) {
        if(!seenScopes.contains(scope)) {
            seenScopes.__insert(scope);

            IFuture<Optional<D>> datumFuture = context.currentDatum(scope);
            K<Optional<D>> processDatum = d -> {
                addedScopes.__put(scope, d);
                d.ifPresent(datum -> {
                    context.getCurrentScopes(datum).forEach(this::addScope);
                });
                tryFinalize();
            };
            future(datumFuture, processDatum);

            IFuture<Iterable<L>> f = context.labels(scope);
            K<Iterable<L>> k = lbls -> {
                lbls.forEach(lbl -> {
                    IFuture<Iterable<S>> edgesFuture = context.getCurrentEdges(scope, lbl);
                    K<Iterable<S>> processEdges = targets -> {
                        targets.forEach(target -> {
                            addedEdges.__insert(new Edge<S, L>(scope, lbl, target));
                            worklist.add(target);
                        });
                    };
                    future(edgesFuture, processEdges);
                });
            };
            future(f, k);
        }
    }

    private <R> void future(IFuture<R> f, K<R> k) {
        pendingResults.incrementAndGet();
        f.thenAccept(res -> {
            pendingResults.decrementAndGet();
            diffK(k, res);
        });
    }

    private <R> void diffK(K<R> k, R r) {
        try {
            k.k(r);
            fixedpoint();
        } catch(Throwable ex) {
            diffResult.completeExceptionally(ex);
        }
    }

    private void tryFinalize() {
        if(pendingResults.get() == 0 && nesting.get() == 0 && typeCheckerFinished.get()) {
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

    @Override public boolean matchScopes(BiMap.Immutable<S> scopes) {
        throw new UnsupportedOperationException("There can be no previous scopes for an added unit.");
    }

    @Override public void typeCheckerFinished() {
        typeCheckerFinished.set(true);
        tryFinalize();
    }

    @Override public IFuture<IScopeDiff<S, L, D>> scopeDiff(S previousScope) {
        throw new UnsupportedOperationException("There can be no previous scopes for an added unit.");
    }

    private interface K<R> {
        void k(R res);
    }
}
