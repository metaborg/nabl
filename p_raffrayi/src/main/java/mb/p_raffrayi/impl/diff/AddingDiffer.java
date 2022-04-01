package mb.p_raffrayi.impl.diff;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.ICompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.scopegraph.oopsla20.IScopeGraph.Immutable;
import mb.scopegraph.oopsla20.diff.BiMap;
import mb.scopegraph.oopsla20.diff.Edge;
import mb.scopegraph.oopsla20.diff.ScopeGraphDiff;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.scopegraph.patching.IPatchCollection;

public class AddingDiffer<S, L, D> implements IScopeGraphDiffer<S, L, D> {

    private static ILogger logger = LoggerUtils.logger(AddingDiffer.class);

    private final AtomicInteger pendingResults = new AtomicInteger(0);
    private final IDifferContext<S, L, D> context;
    private final IDifferOps<S, L, D> differOps;
    private final Set.Immutable<L> edgeLabels;

    private final Map.Transient<S, D> addedScopes = CapsuleUtil.transientMap();
    private final Set.Transient<Edge<S, L>> addedEdges = CapsuleUtil.transientSet();

    private final Set.Transient<S> seenScopes = CapsuleUtil.transientSet();

    private final ICompletableFuture<ScopeGraphDiff<S, L, D>> diffResult = new CompletableFuture<>();

    private final Queue<S> worklist = Lists.newLinkedList();
    private final AtomicBoolean typeCheckerFinished = new AtomicBoolean();
    private final AtomicInteger nesting = new AtomicInteger(0);

    public AddingDiffer(IDifferContext<S, L, D> context, IDifferOps<S, L, D> differOps, Set.Immutable<L> edgeLabels) {
        this.context = context;
        this.differOps = differOps;
        this.edgeLabels = edgeLabels;
    }

    @Override public IFuture<ScopeGraphDiff<S, L, D>> diff(List<S> currentRootScopes, List<S> previousRootScopes) {
        currentRootScopes.forEach(this::addScope);

        fixedpoint();
        tryFinalize();

        return diffResult;
    }

    @Override public IFuture<ScopeGraphDiff<S, L, D>> diff(Immutable<S, L, D> initiallyMatchedGraph,
            Collection<S> scopes, Collection<S> sharedScopes, IPatchCollection.Immutable<S> patches,
            Collection<S> openScopes, Multimap<S, EdgeOrData<L>> openEdges) {
        throw new IllegalStateException("Adding differ cannot be used with initial scopegraph.");
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
    }

    // TODO rework to max recursion depth (fuel) and worklist
    private void addScope(S scope) {
        if(!seenScopes.contains(scope)) {
            seenScopes.__insert(scope);

            if(differOps.ownScope(scope)) {
                IFuture<Optional<D>> datumFuture = context.datum(scope);
                K<Optional<D>> processDatum = d -> {
                    addedScopes.__put(scope, d.orElse(differOps.embed(scope)));
                    d.ifPresent(datum -> {
                        differOps.getScopes(datum).forEach(this::addScope);
                    });
                };
                logger.trace("Schedule datum {}: {}", scope, datumFuture);
                future(datumFuture, processDatum);
            }

            if(differOps.ownOrSharedScope(scope)) {
                for(L lbl : edgeLabels) {
                    IFuture<Iterable<S>> edgesFuture = context.getEdges(scope, lbl);
                    K<Iterable<S>> processEdges = targets -> {
                        targets.forEach(target -> {
                            addedEdges.__insert(new Edge<S, L>(scope, lbl, target));
                            worklist.add(target);
                        });
                    };
                    future(edgesFuture, processEdges);
                }
            }
        }
    }

    private <R> void future(IFuture<R> f, K<R> k) {
        pendingResults.incrementAndGet();
        f.whenComplete((res, ex) -> {
            logger.debug("Complete {}.", f);
            pendingResults.decrementAndGet();
            if(ex != null) {
                diffResult.completeExceptionally(ex);
            } else {
                diffK(k, res);
            }
            tryFinalize();
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
        logger.trace("Try finalize differ. Pending: {}, nesting: {}, TC finished: {}", pendingResults.get(),
                nesting.get(), typeCheckerFinished.get());
        if(pendingResults.get() == 0 && nesting.get() == 0 && typeCheckerFinished.get()) {
            logger.debug("Finalizing differ.");
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

    @Override public IFuture<ScopeDiff<S, L, D>> scopeDiff(S previousScope, L label) {
        throw new UnsupportedOperationException("There can be no previous scopes for an added unit.");
    }

    private interface K<R> {
        void k(R res);
    }
}
