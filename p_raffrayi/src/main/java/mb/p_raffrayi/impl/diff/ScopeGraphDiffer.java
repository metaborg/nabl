package mb.p_raffrayi.impl.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.metaborg.util.RefBool;
import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.HashTrieRelation3;
import org.metaborg.util.collection.IRelation3;
import org.metaborg.util.collection.MultiSetMap;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.future.AggregateFuture;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.Futures;
import org.metaborg.util.future.ICompletable;
import org.metaborg.util.future.ICompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.tuple.Tuple2;
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
import mb.p_raffrayi.TypeCheckingFailedException;
import mb.scopegraph.oopsla20.diff.BiMap;
import mb.scopegraph.oopsla20.diff.BiMaps;
import mb.scopegraph.oopsla20.diff.Edge;
import mb.scopegraph.oopsla20.diff.ScopeGraphDiff;

public class ScopeGraphDiffer<S, L, D> implements IScopeGraphDiffer<S, L, D> {

    private static final ILogger logger = LoggerUtils.logger(ScopeGraphDiffer.class);

    private final IDifferContext<S, L, D> currentContext;
    private final IDifferContext<S, L, D> previousContext;
    private final IDifferOps<S, L, D> differOps;
    private final CompletableFuture<ScopeGraphDiff<S, L, D>> result = new CompletableFuture<>();

    // Intermediate match results

    private final BiMap.Transient<S> matchedScopes = BiMap.Transient.of();
    private final BiMap.Transient<Edge<S, L>> matchedEdges = BiMap.Transient.of();

    private final MultiSetMap.Transient<S, Edge<S, L>> addedEdges = MultiSetMap.Transient.of();
    private final MultiSetMap.Transient<S, Edge<S, L>> removedEdges = MultiSetMap.Transient.of();

    private final Map.Transient<S, Optional<D>> currentScopeData = CapsuleUtil.transientMap();
    private final Map.Transient<S, Optional<D>> previousScopeData = CapsuleUtil.transientMap();

    // Final match results

    private final Set.Transient<S> addedScopes = CapsuleUtil.transientSet();
    private final Set.Transient<S> removedScopes = CapsuleUtil.transientSet();

    // Observations

    private final Set.Transient<S> seenCurrentScopes = CapsuleUtil.transientSet();
    private final IRelation3.Transient<S, L, S> seenCurrentEdges = HashTrieRelation3.Transient.of();

    private final Set.Transient<S> seenPreviousScopes = CapsuleUtil.transientSet();
    private final IRelation3.Transient<S, L, S> seenPreviousEdges = HashTrieRelation3.Transient.of();

    private final Set.Transient<S> completedPreviousScopes = CapsuleUtil.transientSet();
    // Delays

    /**
     * Delays to be fired when the previous scope key is matched (or marked as removed).
     */
    private final MultiSetMap.Transient<S, ICompletable<Unit>> previousScopeProcessedDelays =
            MultiSetMap.Transient.of();

    /**
     * Delays to be fired when the previous scope key is completed (i.e. all outgoing edges are matched or removed).
     */
    private final MultiSetMap.Transient<S, ICompletable<Unit>> previousScopeCompletedDelays =
            MultiSetMap.Transient.of();

    /**
     * Delays to be fired when edge in current scope graph is matched/added.
     */
    private final MultiSetMap.Transient<Edge<S, L>, ICompletable<Unit>> currentEdgeCompleteDelays =
            MultiSetMap.Transient.of();

    // Internal state maintenance

    private final AtomicInteger pendingResults = new AtomicInteger(0);
    private final AtomicInteger fixedPointNesting = new AtomicInteger(0);
    private final AtomicBoolean typeCheckerFinished = new AtomicBoolean(false);

    private final Queue<EdgeMatch> edgeMatches = new PriorityQueue<>();

    public ScopeGraphDiffer(IDifferContext<S, L, D> context, IDifferContext<S, L, D> previousContext,
            IDifferOps<S, L, D> differOps) {
        this.currentContext = context;
        this.previousContext = previousContext;
        this.differOps = differOps;
    }

    ///////////////////////////////////////////////////////////////////////////
    // External API
    // * Can be accessed to start or finish the differ, or to read its state.
    ///////////////////////////////////////////////////////////////////////////

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
            if((initialMatches = consistent(rootMatches.freeze()).orElse(null)) == null) {
                logger.error("Current and previous root scope number differ.");
                throw new IllegalStateException("Provided root scopes cannot be matched.");
            }

            List<IFuture<Unit>> futures = new ArrayList<>();
            initialMatches.entrySet().forEach(e -> {
                S current = e.getKey();
                S previous = e.getValue();
                match(current, previous);
                ICompletableFuture<Unit> future = new CompletableFuture<>();
                consequences(current, previous).whenComplete((cOpt, ex) -> {
                    if(ex != null) {
                        future.completeExceptionally(ex);
                        return;
                    }
                    if(!cOpt.isPresent()) {
                        logger.error("Root match internally inconsistent: {} ~ {}.", current, previous);
                        future.completeExceptionally(new TypeCheckingFailedException("Root match internally inconsistent: " + current + " ~ " + previous));
                        return;
                    }
                    Optional<BiMap.Immutable<S>> matchesOpt = consistent(cOpt.get());
                    if(!matchesOpt.isPresent()) {
                        logger.error("Root match inconsistent: {} ~ {}.", current, previous);
                        future.completeExceptionally(new TypeCheckingFailedException("Root match inconsistent: " + current + " ~ " + previous));
                    }
                    matchesOpt.get().asMap().forEach(this::match);
                    future.complete(Unit.unit);
                });
                futures.add(future);
            });

            new AggregateFuture<>(futures).whenComplete((__, ex) -> {
                if(ex != null) {
                    result.completeExceptionally(ex);
                }

                logger.debug("Scheduled initial matches");
                fixedpoint();
            });
        } catch(Throwable ex) {
            logger.error("Differ initialization failed.", ex);
            result.completeExceptionally(ex);
        }
        return result;
    }

    @Override public boolean matchScopes(BiMap.Immutable<S> scopes) {
        logger.debug("Matching scopes {}", scopes);
        scopes.keySet().forEach(this::scheduleCurrentData);
        scopes.valueSet().forEach(this::schedulePreviousData);

        final BiMap<S> newMatches;
        if((newMatches = consistent(scopes).orElse(null)) == null) {
            logger.trace("Scopes cannot match");
            return false;
        }

        logger.trace("Matching {} succeeded.", scopes);
        for(Map.Entry<S, S> entry : newMatches.entrySet()) {
            match(entry.getKey(), entry.getValue());
        }
        return true;
    }

    @Override public void typeCheckerFinished() {
        typeCheckerFinished.set(true);
        fixedpoint();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Driver
    ///////////////////////////////////////////////////////////////////////////

    private void fixedpoint() {
        logger.trace("Calculating fixpoint");
        // Due to synchronously completing futures, we can have nested fixpoint calls
        // We only complete outer calls (either when finished directly from a call to diff(),
        // or after an asynchronous future completion via diffK.
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
                    && !result.isDone()) {
                result.complete(finalizeDiff());
            }
        } catch(Throwable ex) {
            logger.error("Error computating fixedpoint.", ex);
            result.completeExceptionally(ex);
        }
    }

    private <R> void diffK(K<R> k, R r, Throwable ex) {
        logger.trace("Continuing");
        try {
            k.k(r, ex);
            fixedpoint();
        } catch(Throwable e) {
            logger.error("Continuation terminated unexpectedly.", e);
            result.completeExceptionally(e);
        }
        logger.trace("Finished continuation");
    }

    ///////////////////////////////////////////////////////////////////////////
    // Persistent Matching
    // * State-sensitive, synchronous part of the algorithm
    ///////////////////////////////////////////////////////////////////////////

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

    /**
     * Indicates whether a set of scopes can be matched consistently with the current state, but without taking scope
     * data into account.
     *
     * Used when scheduling edge matches, and providing external matches.
     */
    private Optional<BiMap.Immutable<S>> consistent(BiMap.Immutable<S> scopes) {
        final BiMap.Transient<S> newMatches = BiMap.Transient.of();

        for(Map.Entry<S, S> entry : scopes.entrySet()) {
            final S currentScope = entry.getKey();
            final S previousScope = entry.getValue();
            if(!differOps.isMatchAllowed(currentScope, previousScope)) {
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

    ///////////////////////////////////////////////////////////////////////////
    // Transient Matching
    // * State-agnostic, asynchronous part of the algorithm
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Compute the patch required to match two scopes and their data.
     */
    private IFuture<Optional<BiMap.Immutable<S>>> consequences(S currentScope, S previousScope) {
        return consequences(currentScope, previousScope, BiMap.Immutable.of());
    }

    /**
     * Computes matchability of two scopes, without taking the current state into account.
     * Additional implied matches are collected in the {@code req} argument.
     */
    private IFuture<Optional<BiMap.Immutable<S>>> consequences(S currentScope, S previousScope, BiMap.Immutable<S> req) {
        if(!differOps.isMatchAllowed(currentScope, previousScope)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        if(!req.canPut(currentScope, previousScope)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        if(req.containsEntry(currentScope, previousScope)) {
            return CompletableFuture.completedFuture(Optional.of(req));
        }
        final BiMap.Immutable<S> newReq = req.put(currentScope, previousScope);

        if(differOps.ownScope(currentScope)) {
            // Match data of own scope
            return AggregateFuture.apply(currentContext.datum(currentScope), previousContext.datum(previousScope)).thenCompose(r -> {
                final Optional<D> currentData = r._1();
                final Optional<D> previousData = r._2();
                if(currentData.isPresent() != previousData.isPresent()) {
                    return CompletableFuture.completedFuture(Optional.empty());
                } else if(currentData.isPresent() && previousData.isPresent()) {
                    // Scopes with data can only match if data match
                    // Calculate immediate consequences of data match
                    final Optional<BiMap.Immutable<S>> dataMatch = differOps.matchDatums(currentData.get(), previousData.get())
                            .flatMap(scopeMatches -> BiMaps.safeMerge(newReq, scopeMatches));
                    // @formatter:off
                    // Calculate transitive closure of consequences.
                    return Futures.reducePartial(dataMatch, dataMatch.map(BiMap.Immutable::asMap).map(Map.Immutable::entrySet),
                        (aggMatches, match) -> consequences(match.getKey(), match.getValue(), aggMatches),
                        BiMaps::safeMerge
                    );
                    // @formatter:on
                }
                // Both scopes don't have data
                return CompletableFuture.completedFuture(Optional.of(newReq));
            });
        } else {
            // We do not own the scope, hence ask owner to which current scope it is matched.
            return differOps.externalMatch(previousScope).thenApply(match -> {
                return match.flatMap(target -> {
                    // Insert new remote match
                    match(target, previousScope);
                    if(target.equals(currentScope)) {
                        return Optional.of(BiMap.Immutable.of());
                    }
                    return Optional.empty();
                });
            });
        }
    }


    /**
     * Schedule edge matches from the given source scopes.
     */
    private IFuture<Unit> scheduleEdgeMatches(S currentSource, S previousSource, L label) {
        // Result that is completed when all edges (both current and previous) are processed.
        final ICompletableFuture<Unit> result = new CompletableFuture<>();
        logger.debug("Scheduling edge matches for {} ~ {} and label {}", currentSource, previousSource, label);

        // Get current edges for label
        final IFuture<Set.Immutable<Edge<S, L>>> currentEdgesFuture = currentContext.getEdges(currentSource, label)
                .thenApply(currentTargetScopes -> Streams.stream(currentTargetScopes)
                        .map(currentTarget -> new Edge<>(currentSource, label, currentTarget))
                        .collect(CapsuleCollectors.toSet()));

        // Get previous edges for label
        final IFuture<Set.Immutable<Edge<S, L>>> previousEdgesFuture = previousContext.getEdges(previousSource, label)
                .thenApply(previousTargetScopes -> Streams.stream(previousTargetScopes)
                        .map(previousTarget -> new Edge<>(previousSource, label, previousTarget))
                        .collect(CapsuleCollectors.toSet()));

        // Combine results
        final IFuture<Tuple2<Set.Immutable<Edge<S, L>>, Set.Immutable<Edge<S, L>>>> edgesFuture =
                AggregateFuture.apply(currentEdgesFuture, previousEdgesFuture);

        final K<Tuple2<Set.Immutable<Edge<S, L>>, Set.Immutable<Edge<S, L>>>> k = (res, ex) -> {
            if(ex != null) {
                result.completeExceptionally(ex);
                return Unit.unit;
            }
            Set.Immutable<Edge<S, L>> currentEdges = res._1();
            Set.Immutable<Edge<S, L>> previousEdges = res._2();

            // Log edges as observed
            previousEdges.forEach(edge -> seenPreviousEdges.put(edge.source, edge.label, edge.target));
            currentEdges.forEach(edge -> seenCurrentEdges.put(edge.source, edge.label, edge.target));

            // When all current edges are processed (matched/added), mark remaining previous edges as removed.
            // Eventually,
            scheduleRemovedEdges(previousSource, currentEdges, previousEdges).whenComplete((u, ex2) -> {
                logger.debug("Edge matches for {} ~ {} and label {} finished", currentSource, previousSource, label);
                result.complete(u, ex2);
            });

            for(Edge<S, L> currentEdge : currentEdges) {
                // For each candidate edge, compute which scopes must be matched in order to make the edge match
                // This involves the target scopes, but also the scopes in the data of these target scopes.
                IFuture<List<Tuple2<Edge<S, L>, Optional<BiMap.Immutable<S>>>>> matchesFuture =
                        aggregateAll(previousEdges, previousEdge -> {
                            return consequences(currentEdge.target, previousEdge.target)
                                    .thenApply(matchedScopes -> Tuple2.of(previousEdge, matchedScopes));
                        });

                // When match computation is complete, schedule edge matches for processing.
                K<List<Tuple2<Edge<S, L>, Optional<BiMap.Immutable<S>>>>> k2 = (r, ex2) -> {
                    if(ex2 != null) {
                        result.completeExceptionally(ex2);
                        return Unit.unit;
                    }
                    final Map.Transient<Edge<S, L>, BiMap.Immutable<S>> matchingPreviousEdges = Map.Transient.of();
                    // @formatter:off
                    // filter out all previous edges that cannot be matched (indicated by empty optional)
                    r.stream().filter(x -> x._2().isPresent())
                        // unwrap required matches
                        .map(x -> Tuple2.of(x._1(), x._2().get()))
                        // Check if required matches are consistent with current state and each other
                        .map(x -> Tuple2.of(x._1(), consistent(x._2())))
                        // Filter for consistent matches
                        .filter(x -> x._2().isPresent())
                        // Unwrap matches
                        .map(x -> Tuple2.of(x._1(), x._2().get()))
                        // Add all remaining candidates to set.
                        .forEach(x -> matchingPreviousEdges.__put(x._1(), x._2()));
                    // @formatter:on

                    return queue(new EdgeMatch(currentEdge, matchingPreviousEdges.freeze()));
                };

                future(matchesFuture, k2);
            }

            return Unit.unit;
        };

        future(edgesFuture, k);
        return result;
    }

    /**
     * Ensures that, when all edges in {@code currentEdges} are matches/added, all unmatched edges in
     * {@code previousEdges} are marked as removed.
     */
    private IFuture<Unit> scheduleRemovedEdges(S previousScope, Set.Immutable<Edge<S, L>> currentEdges,
            Set.Immutable<Edge<S, L>> previousEdges) {
        ICompletableFuture<Unit> result = new CompletableFuture<>();

        // Invariant: for all previousEdges, the source should be previousScope
        IFuture<List<Unit>> allCurrentEdgesProcessed = aggregateAll(currentEdges, edge -> {
            ICompletableFuture<Unit> future = new CompletableFuture<>();
            currentEdgeCompleteDelays.put(edge, future);
            return future;
        });
        K<List<Unit>> processPreviousEdges = (__, ex) -> {
            previousEdges.forEach(edge -> {
                if(isPreviousEdgeOpen(edge)) {
                    removed(edge);
                }
                // TODO: assert previous edge closed.
            });
            logger.trace("Edge matches for {} processed.", previousScope);
            result.complete(Unit.unit, ex);
            return Unit.unit;
        };
        future(allCurrentEdgesProcessed, processPreviousEdges);

        return result;
    }

    private void scheduleCurrentData(S currentScope) {
        if(seenCurrentScopes.__insert(currentScope)) {
            IFuture<Optional<D>> cd = currentContext.datum(currentScope);
            K<Optional<D>> insertCS = (d, ex) -> {
                if(ex == null) {
                    logger.trace("Data for {} complete: ", currentScope, d);
                    currentScopeData.__put(currentScope, d);

                    Collection<S> dataScopes = d.map(differOps::getScopes).orElse(Set.Immutable.of());
                    logger.trace("Scopes observed in datum of {}: {}", currentScope, dataScopes);
                    seenCurrentScopes.__insertAll(CapsuleUtil.toSet(dataScopes));

                    dataScopes.forEach(this::scheduleCurrentData);
                } else {
                    result.completeExceptionally(ex);
                }
                return Unit.unit;
            };
            future(cd, insertCS);
        }
    }

    private void schedulePreviousData(S previousScope) {
        if(seenPreviousScopes.__insert(previousScope)) {
            IFuture<Optional<D>> pd = previousContext.datum(previousScope);
            K<Optional<D>> insertPS = (d, ex) -> {
                if(ex == null) {
                    logger.trace("Data for {} complete: ", previousScope, d);
                    previousScopeData.__put(previousScope, d);

                    Collection<S> dataScopes = d.map(differOps::getScopes).orElse(Set.Immutable.of());
                    logger.trace("Scopes observed in datum of {}: {}", previousScope, dataScopes);
                    seenPreviousScopes.__insertAll(CapsuleUtil.toSet(dataScopes));

                    dataScopes.forEach(this::schedulePreviousData);
                } else {
                    result.completeExceptionally(ex);
                }
                return Unit.unit;
            };
            future(pd, insertPS);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Result processing
    ///////////////////////////////////////////////////////////////////////////

    private Unit match(S currentScope, S previousScope) {
        if(matchedScopes.containsEntry(currentScope, previousScope)) {
            return Unit.unit;
        }
        assertCurrentScopeOpen(currentScope);
        assertPreviousScopeOpen(previousScope);

        logger.debug("Matched {} ~ {}", currentScope, previousScope);
        matchedScopes.put(currentScope, previousScope);

        logger.trace("Scheduling edge matches for {} ~ {}", currentScope, previousScope);

        if(differOps.ownOrSharedScope(currentScope)) {
            // We can only own edges from scopes that we own, or that are shared with us.
            final IFuture<Set.Immutable<L>> labels =
                    AggregateFuture.apply(currentContext.labels(currentScope), previousContext.labels(previousScope))
                            .thenApply(lbls -> Set.Immutable.union(lbls._1(), lbls._2()))
                            .whenComplete((l, __) -> logger.trace("Labels for {}: {}", currentScope, l));
            final K<Set.Immutable<L>> scheduleMatches = (lbls, ex) -> {
                if(ex == null) {
                    logger.trace("Received labels for {}, scheduling edge matches.", currentScope);
                    aggregateAll(lbls, lbl -> scheduleEdgeMatches(currentScope, previousScope, lbl)).thenAccept(__ -> {
                        logger.debug("All edge matches for {} ~ {} finished.", currentScope, previousScope);
                        previousScopeComplete(previousScope);
                    });
                } else {
                    result.completeExceptionally(ex);
                }
                return Unit.unit;
            };
            future(labels, scheduleMatches);
        }

        logger.trace("Scheduling scope observations for {} ~ {}", currentScope, previousScope);

        // Collect all scopes in data term of current scope
        scheduleCurrentData(currentScope);

        // Collect all scopes in data term of previous scope
        schedulePreviousData(previousScope);
        previousScopeProcessed(previousScope);

        return Unit.unit;
    }

    private Unit match(Edge<S, L> current, Edge<S, L> previous) {
        assertCurrentEdgeOpen(current);
        assertPreviousEdgeOpen(previous);

        logger.debug("Matching {} ~ {}", current, previous);
        matchedEdges.put(current, previous);

        currentEdgeComplete(current);

        return Unit.unit;
    }

    private Unit added(Edge<S, L> edge) {
        assertCurrentEdgeOpen(edge);

        logger.trace("Added {}", edge);
        addedEdges.put(edge.source, edge);

        currentEdgeComplete(edge);
        scheduleCurrentData(edge.target);

        return Unit.unit;
    }

    private Unit removed(Edge<S, L> edge) {
        assertPreviousEdgeOpen(edge);

        logger.trace("Removed {}", edge);
        removedEdges.put(edge.source, edge);

        schedulePreviousData(edge.target);

        return Unit.unit;
    }

    private Unit added(S currentScope) {
        assertCurrentScopeOpen(currentScope);

        logger.trace("Added {}", currentScope);
        addedScopes.__insert(currentScope);

        scheduleCurrentData(currentScope);

        return Unit.unit;
    }

    private Unit removed(S previousScope) {
        assertPreviousScopeOpen(previousScope);

        logger.trace("Removed {}", previousScope);
        removedScopes.__insert(previousScope);

        schedulePreviousData(previousScope);
        previousScopeProcessed(previousScope);

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
                throw new IllegalStateException("Continuation cannot be executed multiple times.");
            }
            executed.set(true);
            pendingResults.decrementAndGet();
            diffK(k, r, ex);
            return Unit.unit;
        });
        return Unit.unit;
    }

    private ScopeGraphDiff<S, L, D> finalizeDiff() {
        Sets.difference(seenCurrentScopes, matchedScopes.keySet()).forEach(this::added);
        Sets.difference(seenPreviousScopes, matchedScopes.valueSet()).forEach(this::removed);

        currentScopeData.keySet().retainAll(addedScopes);
        previousScopeData.keySet().retainAll(removedScopes);

        Set.Transient<Edge<S, L>> addedEdges = CapsuleUtil.transientSet();
        this.addedEdges.asMap().values().forEach(x -> x.forEach(addedEdges::__insert));

        Set.Transient<Edge<S, L>> removedEdges = CapsuleUtil.transientSet();
        this.removedEdges.asMap().values().forEach(x -> x.forEach(removedEdges::__insert));

        // Clean up pending delays
        previousScopeProcessedDelays.asMap().forEach((s, delays) -> delays.forEach(delay -> {
            logger.error("Pending scope processed delay: {}.", s);
            delay.completeExceptionally(new IllegalStateException("Pending after differ finalization"));
        }));

        previousScopeCompletedDelays.asMap().forEach((s, delays) -> delays.forEach(delay -> {
            logger.error("Pending scope completed delay: {}.", s);
            delay.completeExceptionally(new IllegalStateException("Pending after differ finalization"));
        }));

        currentEdgeCompleteDelays.asMap().forEach((edge, delays) -> delays.forEach(delay -> {
            logger.error("Pending edge complete delay: {}.", edge);
            delay.completeExceptionally(new IllegalStateException("Pending after differ finalization."));
        }));

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
        return result;
    }

    // Queries

    @Override public IFuture<Optional<S>> match(S previousScope) {
        if(matchedScopes.containsValue(previousScope)) {
            S currentScope = matchedScopes.getValue(previousScope);
            return CompletableFuture.completedFuture(Optional.of(currentScope));
        }

        final ICompletableFuture<Unit> result = new CompletableFuture<>();
        previousScopeProcessedDelays.put(previousScope, result);
        return result.thenApply(__ -> {
            logger.trace("Handling PS complete.");
            // When previousScope is released, it should be either in matchedScopes or in removedScopes.
            // If null, it is in removed scopes, so returning is safe.
            return Optional.ofNullable(matchedScopes.getValue(previousScope));
        });
    }

    @Override public IFuture<IScopeDiff<S, L, D>> scopeDiff(S previousScope) {
        if(completedPreviousScopes.contains(previousScope)) {
            return CompletableFuture.completedFuture(buildScopeDiff(previousScope));
        }

        ICompletableFuture<Unit> result = new CompletableFuture<>();
        previousScopeCompletedDelays.put(previousScope, result);

        return result.thenApply(__ -> buildScopeDiff(previousScope));
    }

    private IScopeDiff<S, L, D> buildScopeDiff(S previousScope) {
        if(matchedScopes.containsValue(previousScope)) {
            S currentScope = matchedScopes.getValue(previousScope);
            return Matched.of(currentScope, addedEdges.get(currentScope), removedEdges.get(previousScope));
        }
        return Removed.of();
    }

    // Events

    private void previousScopeProcessed(S previousScope) {
        logger.trace("Complete (PS) {}", previousScope);
        previousScopeProcessedDelays.removeKey(previousScope).forEach(c -> c.complete(Unit.unit));
        logger.trace("Finished complete (PS) {}", previousScope);
    }

    private void previousScopeComplete(S previousScope) {
        logger.trace("Complete (PSC) {}", previousScope);
        completedPreviousScopes.__insert(previousScope);
        previousScopeCompletedDelays.removeKey(previousScope).forEach(c -> c.complete(Unit.unit));
        logger.trace("Finished complete (PSC) {}", previousScope);
    }

    private void currentEdgeComplete(Edge<S, L> current) {
        logger.trace("Complete (CE), {}", current);
        currentEdgeCompleteDelays.removeKey(current).forEach(c -> c.complete(Unit.unit));
        logger.trace("Finished complete (CE), {}", current);
    }

    // Helper methods and classes

    private static <T, R> IFuture<List<R>> aggregateAll(Iterable<T> items, Function1<T, IFuture<R>> mapper) {
        return new AggregateFuture<R>(Streams.stream(items).map(mapper::apply).collect(Collectors.toSet()));
    }

    private boolean isCurrentScopeOpen(S scope) {
        return !result.isDone() && !matchedScopes.containsKey(scope) && !addedScopes.contains(scope);
    }

    private boolean isPreviousScopeOpen(S scope) {
        return !result.isDone() && !matchedScopes.containsValue(scope) && !removedScopes.contains(scope);
    }

    private boolean isCurrentEdgeOpen(Edge<S, L> edge) {
        return !result.isDone() && !matchedEdges.containsKey(edge) && !addedEdges.containsValue(edge);
    }

    private boolean isPreviousEdgeOpen(Edge<S, L> edge) {
        return !result.isDone() && !matchedEdges.containsValue(edge) && !removedEdges.containsValue(edge);
    }

    private void assertCurrentScopeOpen(S scope) {
        if(!isCurrentScopeOpen(scope)) {
            throw new IllegalStateException("Scope " + scope + " is already matched.");
        }
    }

    private void assertPreviousScopeOpen(S scope) {
        if(!isPreviousScopeOpen(scope)) {
            throw new IllegalStateException("Scope " + scope + " is already matched.");
        }
    }

    private void assertCurrentEdgeOpen(Edge<S, L> edge) {
        if(!isCurrentEdgeOpen(edge)) {
            throw new IllegalStateException("Edge " + edge + " is already matched/added.");
        }
    }

    private void assertPreviousEdgeOpen(Edge<S, L> edge) {
        if(!isPreviousEdgeOpen(edge)) {
            throw new IllegalStateException("Edge " + edge + " is already matched/removed.");
        }
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
        Unit k(R result, Throwable ex);
    }

}
