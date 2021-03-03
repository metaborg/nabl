package mb.statix.concurrent.p_raffrayi.diff;

import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.metaborg.util.RefBool;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.unit.Unit;

import com.google.common.collect.Sets;
import com.google.common.collect.Streams;

import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Map.Immutable;
import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.nabl2.util.CapsuleUtil;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation3;
import mb.nabl2.util.collections.MultiSetMap;
import mb.statix.concurrent.actors.futures.AggregateFuture;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.ICompletable;
import mb.statix.concurrent.actors.futures.ICompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.scopegraph.diff.BiMap;
import mb.statix.scopegraph.diff.Edge;
import mb.statix.scopegraph.diff.ScopeGraphDiff;

public class ScopeGraphDiffer<S, L, D> implements IScopeGraphDiffer<S, L, D> {

    private static final ILogger logger = LoggerUtils.logger(ScopeGraphDiffer.class);
    private static final IFuture<Unit> COMPLETE = CompletableFuture.completedFuture(Unit.unit);

    private final IScopeGraphDifferContext<S, L, D> context;
    private final CompletableFuture<ScopeGraphDiff<S, L, D>> complete = new CompletableFuture<>();

    // Intermediate match results

    private final BiMap.Transient<S> matchedScopes = BiMap.Transient.of();
    private final BiMap.Transient<Edge<S, L>> matchedEdges = BiMap.Transient.of();

    private final MultiSetMap.Transient<S, Edge<S, L>> addedEdges = MultiSetMap.Transient.of();
    private final MultiSetMap.Transient<S, Edge<S, L>> removedEdges = MultiSetMap.Transient.of();

    private final Map.Transient<S, Optional<D>> currentScopeData = CapsuleUtil.transientMap();
    private final Map.Transient<S, Optional<D>> previousScopeData = CapsuleUtil.transientMap();

    // Observations

    private final Set.Transient<S> seenCurrentScopes = Set.Transient.of();
    private final IRelation3.Transient<S, L, S> seenCurrentEdges = HashTrieRelation3.Transient.of();

    private final Set.Transient<S> seenPreviousScopes = Set.Transient.of();
    private final IRelation3.Transient<S, L, S> seenPreviousEdges = HashTrieRelation3.Transient.of();

    // Delays

    /**
     * Delays to be fired when the previous scope key is matched (or marked as removed).
     */
    private final MultiSetMap.Transient<S, ICompletable<Unit>> delays_ps = MultiSetMap.Transient.of();

    /**
     * Delays to be fired when the previous scope key is completed (i.e. all outgoing edges are matched or removed).
     */
    private final MultiSetMap.Transient<S, ICompletable<Unit>> delays_ps_complete = MultiSetMap.Transient.of();

    /**
     * Delays to be fired when edge in current scope graph is matched/added.
     */
    private final MultiSetMap.Transient<Edge<S, L>, ICompletable<Unit>> delays_ce = MultiSetMap.Transient.of();

    // Internal state maintenance

    private final AtomicInteger pendingResults = new AtomicInteger(0);
    private final AtomicInteger fixedPointNesting = new AtomicInteger(0);
    private final AtomicBoolean typeCheckerFinished = new AtomicBoolean(false);

    private final Queue<EdgeMatch> edgeMatches = new PriorityQueue<>();

    public ScopeGraphDiffer(IScopeGraphDifferContext<S, L, D> context) {
        this.context = context;
    }

    // Diffing

    @Override public IFuture<ScopeGraphDiff<S, L, D>> diff(List<S> currentRootScopes, List<S> previousRootScopes) {
        try {
            logger.debug("Start scope graph differ");
            if(currentRootScopes.size() != previousRootScopes.size()) {
                logger.error("Current and previous root scope number differ.");
                throw new IllegalStateException("Current and previous root scope number differ.");
            }

            final BiMap.Transient<S> rootMatches = BiMap.Transient.of();
            for(int i = 0; i < currentRootScopes.size(); i++) {
                rootMatches.put(currentRootScopes.get(i), previousRootScopes.get(i));
            }

            // Calculate matches caused by root scope matches
            BiMap.Immutable<S> initialMatches;
            if((initialMatches = canScopesMatch(rootMatches.freeze()).orElse(null)) == null) {
                logger.error("Current and previous root scope number differ.");
                throw new IllegalStateException("Provided root scopes cannot be matched.");
            }

            initialMatches.entrySet().forEach(e -> match(e.getKey(), e.getValue()));
            logger.debug("Scheduled initial matches");

            fixedpoint();
        } catch(Throwable ex) {
            logger.error("Differ initialization failed.", ex);
            complete.completeExceptionally(ex);
        }
        return complete;
    }

    @Override public boolean matchScopes(BiMap.Immutable<S> scopes) {
        logger.debug("Matching scopes {}", scopes);
        scopes.keySet().forEach(this::scheduleCurrentData);
        scopes.valueSet().forEach(this::schedulePreviousData);

        final BiMap<S> newMatches;
        if((newMatches = canScopesMatch(scopes).orElse(null)) == null) {
            logger.trace("Scopes cannot match");
            return false;
        }

        logger.trace("Matching succeeded.");
        for(Map.Entry<S, S> entry : newMatches.entrySet()) {
            match(entry.getKey(), entry.getValue());
        }
        return true;
    }

    @Override public void typeCheckerFinished() {
        typeCheckerFinished.set(true);
        fixedpoint();
    }

    private void fixedpoint() {
        logger.trace("Calculating fixpoint");
        fixedPointNesting.incrementAndGet();

        try {
            try {
                while(!edgeMatches.isEmpty()) {
                    EdgeMatch m = edgeMatches.remove();
                    matchEdge(m.currentEdge, m.previousEdges);
                }

                logger.trace("Reached fixpoint. Nesting level: {}, Pending: {}", fixedPointNesting.get(),
                    pendingResults.get());
            } finally {
                fixedPointNesting.decrementAndGet();
            }

            if(fixedPointNesting.get() == 0 && pendingResults.get() == 0 && typeCheckerFinished.get()
                && !complete.isDone()) {
                logger.info("Differ reached termination condition. Finalizing.");
                currentScopeData.keySet().retainAll(Sets.difference(seenCurrentScopes, matchedScopes.keySet()));
                previousScopeData.keySet().retainAll(Sets.difference(seenPreviousScopes, matchedScopes.valueSet()));

                Set.Transient<Edge<S, L>> addedEdges = CapsuleUtil.transientSet();
                this.addedEdges.entrySet().stream().forEach(x -> x.getValue().forEach(addedEdges::__insert));
                Set.Transient<Edge<S, L>> removedEdges = CapsuleUtil.transientSet();
                this.removedEdges.entrySet().stream().forEach(x -> x.getValue().forEach(removedEdges::__insert));

                // @formatter:off
                ScopeGraphDiff<S, L, D> result = new ScopeGraphDiff<S, L, D>(
                    matchedScopes.freeze(),
                    matchedEdges.freeze(),
                    currentScopeData.freeze(),
                    addedEdges.freeze(),
                    previousScopeData.freeze(),
                    removedEdges.freeze()
                );
                // @formatter:on
                complete.complete(result);
            }
        } catch(Throwable ex) {
            logger.error("Error computating fixedpoint.", ex);
            complete.completeExceptionally(ex);
        }
    }

    private <R> void diffK(K<R> k, R r, Throwable ex) {
        logger.trace("Continuing");
        try {
            if(ex != null) {
                throw ex;
            }
            k.k(r);
            fixedpoint();
        } catch(Throwable e) {
            logger.error("Continuation terminated unexpectedly.", e);
            complete.completeExceptionally(e);
        }
        logger.trace("Finished continuation");
    }

    private Unit matchEdge(Edge<S, L> currentEdge, Immutable<Edge<S, L>, BiMap.Immutable<S>> previousEdges) {
        logger.debug("Matching edge {} with candidates {}", currentEdge, previousEdges);
        if(previousEdges.isEmpty()) {
            return added(currentEdge);
        }
        final Entry<Edge<S, L>, BiMap.Immutable<S>> previousEdge = previousEdges.entrySet().iterator().next();

        if(matchScopes(previousEdge.getValue())) {
            logger.trace("Matching {} with {} succeeded.", currentEdge, previousEdge);
            return match(currentEdge, previousEdge.getKey());
        } else {
            logger.trace("Matching {} with {} failed, queueing match for remainder.", currentEdge, previousEdge);
            return queue(new EdgeMatch(currentEdge, previousEdges.__remove(previousEdge.getKey())));
        }
    }

    private Optional<BiMap.Immutable<S>> canScopesMatch(BiMap.Immutable<S> scopes) {
        final BiMap.Transient<S> newMatches = BiMap.Transient.of();

        for(Map.Entry<S, S> entry : scopes.entrySet()) {
            final S currentScope = entry.getKey();
            final S previousScope = entry.getValue();
            if(!context.isMatchAllowed(currentScope, previousScope)) {
                logger.trace("Matching {} with {} is not allowed by context.", currentScope, previousScope);
                return Optional.empty();
            } else if(!matchedScopes.canPut(currentScope, previousScope)) {
                logger.trace("Matching {} with {} is not allowed: one or both is already matched.", currentScope,
                    previousScope);
                return Optional.empty();
            } else if(matchedScopes.containsEntry(currentScope, previousScope)) {
                // skip this pair as it was already matched
            } else {
                newMatches.put(currentScope, previousScope);
            }
        }
        logger.trace("Scopes {} match.", scopes);
        return Optional.of(newMatches.freeze());
    }

    /**
     * Schedule edge matches from the given source scopes.
     */
    private IFuture<Unit> scheduleEdgeMatches(S currentSource, S previousSource, L label) {
        ICompletableFuture<Unit> result = new CompletableFuture<>();
        logger.debug("Scheduling edge matches for {} ~ {} and label {}", currentSource, previousSource, label);
        final IFuture<Set.Immutable<Edge<S, L>>> currentEdgesFuture = context.getCurrentEdges(currentSource, label)
            .thenApply(currentTargetScopes -> Streams.stream(currentTargetScopes)
                .map(currentTarget -> new Edge<>(currentSource, label, currentTarget))
                .collect(CapsuleCollectors.toSet()));

        final IFuture<Set.Immutable<Edge<S, L>>> previousEdgesFuture = context.getPreviousEdges(previousSource, label)
            .thenApply(previousTargetScopes -> Streams.stream(previousTargetScopes)
                .map(previousTarget -> new Edge<>(previousSource, label, previousTarget))
                .collect(CapsuleCollectors.toSet()));

        final IFuture<Tuple2<Set.Immutable<Edge<S, L>>, Set.Immutable<Edge<S, L>>>> edgesFuture =
            AggregateFuture.apply(currentEdgesFuture, previousEdgesFuture);

        final K<Tuple2<Set.Immutable<Edge<S, L>>, Set.Immutable<Edge<S, L>>>> k = (res) -> {
            Set.Immutable<Edge<S, L>> currentEdges = res._1();
            Set.Immutable<Edge<S, L>> previousEdges = res._2();

            previousEdges.forEach(edge -> seenPreviousEdges.put(edge.source, edge.label, edge.target));
            currentEdges.forEach(edge -> seenCurrentEdges.put(edge.source, edge.label, edge.target));

            scheduleRemovedEdges(previousSource, currentEdges, previousEdges).thenAccept(u -> {
                logger.debug("Edge matches for {} ~ {} and label {} finished", currentSource, previousSource, label);
                result.complete(u);
            });

            for(Edge<S, L> currentEdge : currentEdges) {
                IFuture<List<Tuple2<Edge<S, L>, Optional<BiMap.Immutable<S>>>>> matchesFuture =
                    aggregateAll(previousEdges, previousEdge -> {
                        return matchScopes(currentEdge.target, previousEdge.target)
                            .thenApply(matchedScopes -> Tuple2.of(previousEdge, matchedScopes));
                    });

                K<List<Tuple2<Edge<S, L>, Optional<BiMap.Immutable<S>>>>> k2 = (r) -> {
                    final Map.Transient<Edge<S, L>, BiMap.Immutable<S>> matchingPreviousEdges = Map.Transient.of();
                    r.stream().filter(x -> x._2().isPresent()).map(x -> Tuple2.of(x._1(), x._2().get()))
                        .map(x -> Tuple2.of(x._1(), canScopesMatch(x._2()))).filter(x -> x._2().isPresent())
                        .map(x -> Tuple2.of(x._1(), x._2().get()))
                        .forEach(x -> matchingPreviousEdges.__put(x._1(), x._2()));

                    return queue(new EdgeMatch(currentEdge, matchingPreviousEdges.freeze()));
                };

                future(matchesFuture, k2);
            }

            return Unit.unit;
        };

        future(edgesFuture, k);
        return result;
    }

    private IFuture<Unit> scheduleRemovedEdges(S previousScope, Set.Immutable<Edge<S, L>> currentEdges,
        Set.Immutable<Edge<S, L>> previousEdges) {

        ICompletableFuture<Unit> result = new CompletableFuture<>();
        // Invariant: for all previousEdges, the source should be previousScope
        IFuture<List<Unit>> f = aggregateAll(currentEdges, edge -> {
            ICompletableFuture<Unit> future = new CompletableFuture<>();
            delays_ce.put(edge, future);
            return future;
        });
        K<List<Unit>> k = __ -> {
            previousEdges.forEach(edge -> {
                if(!matchedEdges.containsValue(edge)) {
                    removed(edge);
                }
            });
            logger.trace("Edge matches for {} processed.", previousScope);
            result.complete(Unit.unit);
            return Unit.unit;
        };
        future(f, k);

        return result;
    }

    /**
     * Compute the patch required to match two scopes and their data.
     */
    private IFuture<Optional<BiMap.Immutable<S>>> matchScopes(S currentScope, S previousScope) {
        CompletableFuture<Optional<BiMap.Immutable<S>>> result = new CompletableFuture<>();
        final BiMap.Transient<S> _matches = BiMap.Transient.of();
        scopeMatch(currentScope, previousScope, _matches).thenAccept(match -> {
            if(match) {
                result.complete(Optional.of(_matches.freeze()));
            } else {
                result.complete(Optional.empty());
            }
        });
        return result;
    }

    private IFuture<Boolean> scopeMatch(S currentScope, S previousScope, BiMap.Transient<S> req) {
        if(!context.isMatchAllowed(currentScope, previousScope)) {
            return CompletableFuture.completedFuture(false);
        }
        if(!req.canPut(currentScope, previousScope)) {
            return CompletableFuture.completedFuture(false);
        }
        if(req.containsEntry(currentScope, previousScope)) {
            return CompletableFuture.completedFuture(true);
        }
        req.put(currentScope, previousScope);

        ICompletableFuture<Boolean> result = new CompletableFuture<>();
        if(context.ownScope(currentScope)) {
            IFuture<Tuple2<Optional<D>, Optional<D>>> f =
                AggregateFuture.apply(context.currentDatum(currentScope), context.previousDatum(previousScope));
            K<Tuple2<Optional<D>, Optional<D>>> k = r -> {
                final Optional<D> currentData = r._1();
                final Optional<D> previousData = r._2();
                if(currentData.isPresent() != previousData.isPresent()) {
                    result.complete(false);
                } else if(currentData.isPresent() && previousData.isPresent()) {
                    IFuture<Boolean> fm = context.matchDatums(currentData.get(), previousData.get(),
                        (leftScope, rightScope) -> scopeMatch(leftScope, rightScope, req));
                    K<Boolean> km = m -> {
                        result.complete(m);
                        return Unit.unit;
                    };
                    future(fm, km);
                } else {
                    result.complete(true);
                }
                return Unit.unit;
            };
            future(f, k);
        } else {
            // We do not own the scope, hence ask owner to which current scope it is matched.
            IFuture<Optional<S>> f = context.externalMatch(previousScope);
            K<Optional<S>> k = m -> {
                if(m.isPresent()) {
                    // Insert new remote match
                    match(m.get(), previousScope);
                    result.complete(m.get().equals(currentScope));
                } else {
                    result.complete(false);
                }
                return Unit.unit;
            };
            future(f, k);
        }
        return result;
    }

    // Result processing
    private Unit match(S currentScope, S previousScope) {
        if(matchedScopes.containsEntry(currentScope, previousScope)) {
            throw new IllegalStateException("Scope " + currentScope + " is already matched with " + previousScope);
        }

        logger.debug("Matched {} ~ {}", currentScope, previousScope);
        matchedScopes.put(currentScope, previousScope);

        logger.trace("Scheduling edge matches for {} ~ {}", currentScope, previousScope);

        if(context.ownOrSharedScope(currentScope)) {
            // We can only own edges from scopes that we own, or that are shared with us.
            final IFuture<Iterable<L>> labels = context.labels(currentScope)
                .whenComplete((l, __) -> logger.trace("Labels for {}: {}", currentScope, l));
            final K<Iterable<L>> scheduleMatches = (lbls) -> {
                logger.trace("Received labels for {}, scheduling edge matches.", currentScope);
                aggregateAll(lbls, lbl -> scheduleEdgeMatches(currentScope, previousScope, lbl)).thenAccept(__ -> {
                    logger.debug("All edge matches for {} ~ {} finished.", currentScope, previousScope);
                    logger.trace("Complete (PSC) {}", previousScope);
                    if(delays_ps_complete.containsKey(previousScope)) {
                        delays_ps_complete.get(previousScope).forEach(c -> c.complete(Unit.unit));
                    }
                    logger.trace("Finished complete (PSC) {}", previousScope);
                });
                return Unit.unit;
            };
            future(labels, scheduleMatches);
        }

        logger.trace("Scheduling scope observations for {} ~ {}", currentScope, previousScope);

        // Collect all scopes in data term of current scope
        IFuture<Unit> fc = scheduleCurrentData(currentScope);

        // Collect all scopes in data term of previous scope
        IFuture<Unit> fp = schedulePreviousData(previousScope);

        logger.trace("Complete (PS) {}", previousScope);
        if(delays_ps.containsKey(previousScope)) {
            delays_ps.get(previousScope).forEach(c -> c.complete(Unit.unit));
        }
        logger.trace("Finished complete (PS) {}", previousScope);

        return Unit.unit;
    }

    private IFuture<Unit> scheduleCurrentData(S currentScope) {
        if(seenCurrentScopes.__insert(currentScope)) {
            ICompletableFuture<Unit> result = new CompletableFuture<>();
            IFuture<Optional<D>> cd = context.currentDatum(currentScope);
            K<Optional<D>> insertCS = d -> {
                logger.trace("Data for {} complete: ", currentScope, d);
                currentScopeData.__put(currentScope, d);

                Set.Immutable<S> dataScopes = d.map(context::getCurrentScopes).orElse(Set.Immutable.of());
                logger.trace("Scopes observed in datum of {}: {}", currentScope, dataScopes);
                seenCurrentScopes.__insertAll(dataScopes);

                dataScopes.forEach(this::scheduleCurrentData);
                result.complete(Unit.unit);
                return Unit.unit;
            };
            future(cd, insertCS);
            return result;
        }
        return COMPLETE;
    }

    private IFuture<Unit> schedulePreviousData(S previousScope) {
        if(seenPreviousScopes.__insert(previousScope)) {
            ICompletableFuture<Unit> result = new CompletableFuture<>();
            IFuture<Optional<D>> pd = context.previousDatum(previousScope);
            K<Optional<D>> insertPS = d -> {
                logger.trace("Data for {} complete: ", previousScope, d);
                previousScopeData.__put(previousScope, d);

                Set.Immutable<S> dataScopes = d.map(context::getPreviousScopes).orElse(Set.Immutable.of());
                logger.trace("Scopes observed in datum of {}: {}", previousScope, dataScopes);
                seenPreviousScopes.__insertAll(dataScopes);

                dataScopes.forEach(this::schedulePreviousData);
                result.complete(Unit.unit);
                return Unit.unit;
            };
            future(pd, insertPS);
            return result;
        }
        return COMPLETE;
    }

    private Unit match(Edge<S, L> current, Edge<S, L> previous) {
        if(matchedEdges.containsKey(current) || matchedEdges.containsValue(previous)) {
            throw new IllegalStateException("At least one of both edges is already added.");
        }

        if(addedEdges.containsValue(current)) {
            throw new IllegalStateException("Edge " + current + " is already marked as added.");
        }

        if(removedEdges.containsValue(previous)) {
            throw new IllegalStateException("Edge " + previous + " is already marked as removed.");
        }

        logger.debug("Matching {} ~ {}", current, previous);
        matchedEdges.put(current, previous);

        logger.trace("Complete (CE), {}", current);
        delays_ce.get(current).forEach(c -> c.complete(Unit.unit));
        logger.trace("Finished complete (CE), {}", current);

        return Unit.unit;
    }

    private Unit added(Edge<S, L> edge) {
        if(matchedEdges.containsKey(edge)) {
            throw new IllegalStateException("Edge is already matched.");
        }

        if(addedEdges.containsValue(edge)) {
            throw new IllegalStateException("Edge " + edge + " is already marked as added.");
        }

        logger.trace("Added {}", edge);
        addedEdges.put(edge.source, edge);

        logger.trace("Complete (CE), {}", edge);
        delays_ce.get(edge).forEach(c -> c.complete(Unit.unit));
        logger.trace("Finished complete (CE), {}", edge);
        scheduleCurrentData(edge.target);

        return Unit.unit;
    }

    private Unit removed(Edge<S, L> edge) {
        if(matchedEdges.containsValue(edge)) {
            throw new IllegalStateException("Edge is already matched.");
        }

        if(removedEdges.containsValue(edge)) {
            throw new IllegalStateException("Edge " + edge + " is already marked as removed.");
        }

        logger.trace("Removed {}", edge);
        removedEdges.put(edge.source, edge);

        schedulePreviousData(edge.target);

        return Unit.unit;
    }

    private Unit queue(EdgeMatch match) {
        logger.trace("Queuing delayed match {}", match);
        edgeMatches.add(match);

        return Unit.unit;
    }

    private <R> Unit future(IFuture<R> future, K<R> k) {
        RefBool executed = new RefBool(false);
        pendingResults.incrementAndGet();
        future.handle((r, ex) -> {
            if(executed.get()) {
                throw new IllegalStateException("Continuation executed earlier");
            }
            executed.set(true);
            pendingResults.decrementAndGet();
            diffK(k, r, ex);
            return Unit.unit;
        });
        return Unit.unit;
    }

    // Queries

    @Override public IFuture<Optional<S>> match(S previousScope) {
        if(matchedScopes.containsValue(previousScope)) {
            S currentScope = matchedScopes.getValue(previousScope);
            return CompletableFuture.completedFuture(Optional.of(currentScope));
        }
        final ICompletableFuture<Unit> result = new CompletableFuture<>();
        delays_ps.put(previousScope, result);
        return result.thenApply(__ -> {
            logger.trace("Handling PS complete.");
            // When previousScope is released, it should be either in matchedScopes
            // or in removedScopes.
            if(!matchedScopes.containsValue(previousScope)) {
                logger.error("Scope {} is completed, but not added to matched or removed set.", previousScope);
                throw new IllegalStateException(
                    "Scope " + previousScope + " is completed, but not added to matched or removed set.");
            }
            // If null, it is in removed scopes, so returning is safe.
            return Optional.ofNullable(matchedScopes.getValue(previousScope));
        });
    }

    @Override public IFuture<IScopeDiff<S, L, D>> scopeDiff(S previousScope) {
        ICompletableFuture<Unit> result = new CompletableFuture<>();

        delays_ps_complete.put(previousScope, result);

        return result.thenApply(__ -> {
            if(matchedScopes.containsValue(previousScope)) {
                S currentScope = matchedScopes.getValue(previousScope);
                return Matched.of(currentScope, addedEdges.get(currentScope), removedEdges.get(previousScope));
            }
            return Removed.of();
        });
    }

    // Helper methods and classes

    private static <T, R> IFuture<List<R>> aggregateAll(Iterable<T> items, Function1<T, IFuture<R>> mapper) {
        return new AggregateFuture<R>(Streams.stream(items).map(mapper::apply).collect(Collectors.toSet()));
    }

    private class EdgeMatch implements Comparable<EdgeMatch> {

        public final Edge<S, L> currentEdge;
        public final Map.Immutable<Edge<S, L>, BiMap.Immutable<S>> previousEdges;

        public EdgeMatch(Edge<S, L> currentEdge, Map.Immutable<Edge<S, L>, BiMap.Immutable<S>> previousEdges) {
            this.currentEdge = currentEdge;
            this.previousEdges = previousEdges;
        }

        @Override public int compareTo(EdgeMatch that) {
            return this.previousEdges.size() - that.previousEdges.size();
        }

        @Override public String toString() {
            return currentEdge + " ~ " + previousEdges;
        }

    }

    ///////////////////////////////////////////////////////////////////////////
    // K
    ///////////////////////////////////////////////////////////////////////////

    @FunctionalInterface
    private interface K<R> {
        Unit k(R result);
    }


}
