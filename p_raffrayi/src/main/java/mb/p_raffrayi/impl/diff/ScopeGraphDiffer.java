package mb.p_raffrayi.impl.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.metaborg.util.functions.Action1;
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
    private Throwable failure;

    // Intermediate match results

    private final BiMap.Transient<S> matchedScopes = BiMap.Transient.of();
    private final BiMap.Transient<Edge<S, L>> matchedEdges = BiMap.Transient.of();

    private final MultiSetMap.Transient<S, Edge<S, L>> addedEdges = MultiSetMap.Transient.of();
    private final MultiSetMap.Transient<S, Edge<S, L>> matchedOutgoingEdges = MultiSetMap.Transient.of();
    private final MultiSetMap.Transient<S, Edge<S, L>> removedEdges = MultiSetMap.Transient.of();

    private final Map.Transient<S, Optional<D>> currentScopeData = CapsuleUtil.transientMap();
    private final Map.Transient<S, Optional<D>> previousScopeData = CapsuleUtil.transientMap();

    // Final match results

    private final Set.Transient<S> addedScopes = CapsuleUtil.transientSet();
    private final Set.Transient<S> removedScopes = CapsuleUtil.transientSet();

    // Observations

    private final Set.Transient<S> seenCurrentScopes = CapsuleUtil.transientSet();
    private final Set.Transient<S> openCurrentScopes = CapsuleUtil.transientSet();
    private final IRelation3.Transient<S, L, S> seenCurrentEdges = HashTrieRelation3.Transient.of();

    private final Set.Transient<S> seenPreviousScopes = CapsuleUtil.transientSet();
    private final Set.Transient<S> openPreviousScopes = CapsuleUtil.transientSet();
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

    private final Set.Transient<IToken<S, L>> waitFors = CapsuleUtil.transientSet();

    // Internal state maintenance

    private final AtomicInteger pendingResults = new AtomicInteger(0);
    private final AtomicBoolean inFixedPoint = new AtomicBoolean(false);
    private final AtomicBoolean inFinalize = new AtomicBoolean(false);
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
                scheduleCurrentData(current);
                schedulePreviousData(previous);

                match(current, previous);
                ICompletableFuture<Unit> future = new CompletableFuture<>();
                consequences(current, previous).whenComplete((cOpt, ex) -> {
                    if(ex != null) {
                        future.completeExceptionally(ex);
                        return;
                    }
                    if(!cOpt.isPresent()) {
                        logger.error("Root match internally inconsistent: {} ~ {}.", current, previous);
                        future.completeExceptionally(new TypeCheckingFailedException(
                                "Root match internally inconsistent: " + current + " ~ " + previous));
                        return;
                    }
                    Optional<BiMap.Immutable<S>> matchesOpt = consistent(cOpt.get());
                    if(!matchesOpt.isPresent()) {
                        logger.error("Root match inconsistent: {} ~ {}.", current, previous);
                        future.completeExceptionally(new TypeCheckingFailedException(
                                "Root match inconsistent: " + current + " ~ " + previous));
                    }
                    matchesOpt.get().asMap().forEach(this::match);
                    future.complete(Unit.unit);
                });
                futures.add(future);
            });

            AggregateFuture.of(futures).whenComplete((__, ex) -> {
                if(ex != null) {
                    failure(ex);
                }

                logger.debug("Scheduled initial matches");
                fixedpoint();
            });
        } catch(Throwable ex) {
            logger.error("Differ initialization failed.", ex);
            failure(ex);
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
        if(inFixedPoint.getAndSet(true)) {
            return;
        }

        try {
            boolean exec = true;
            while(exec) {
                try {
                    exec = false;
                    while(!edgeMatches.isEmpty()) {
                        EdgeMatch m = edgeMatches.remove();
                        matchEdge(m.currentEdge, m.previousEdges);
                    }

                    logger.trace("Reached fixpoint. Pending: {}", pendingResults.get());
                } finally {
                    inFixedPoint.set(false);
                }

                if(pendingResults.get() == 0 && typeCheckerFinished.get() && !result.isDone() && !inFinalize.get()) {
                    try {
                        inFinalize.set(true);
                        exec = tryFinalizeDiff();
                    } finally {
                        inFinalize.set(false);
                    }
                }
            }
        } catch(Throwable ex) {
            logger.error("Error computating fixedpoint.", ex);
            failure(ex);
        }
    }

    private <R> void diffK(K<R> k, R r, Throwable ex) {
        logger.trace("Continuing");
        try {
            k.k(r, ex);
            fixedpoint();
        } catch(Throwable e) {
            logger.error("Continuation terminated unexpectedly.", e);
            failure(e);
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
     * Computes matchability of two scopes, without taking the current state into account. Additional implied matches
     * are collected in the {@code req} argument.
     */
    private IFuture<Optional<BiMap.Immutable<S>>> consequences(S currentScope, S previousScope,
            BiMap.Immutable<S> req) {
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
            return AggregateFuture
                    .apply(waitFor(currentContext.datum(currentScope)), waitFor(previousContext.datum(previousScope)))
                    .thenCompose(r -> {
                        final Optional<D> currentData = r._1();
                        final Optional<D> previousData = r._2();
                        if(currentData.isPresent() != previousData.isPresent()) {
                            return CompletableFuture.completedFuture(Optional.empty());
                        } else if(currentData.isPresent() && previousData.isPresent()) {
                            // Scopes with data can only match if data match
                            // Calculate immediate consequences of data match
                            final Optional<BiMap.Immutable<S>> dataMatch =
                                    differOps.matchDatums(currentData.get(), previousData.get())
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
            return waitFor(differOps.externalMatch(previousScope)).thenApply(match -> {
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
        final IFuture<Set.Immutable<Edge<S, L>>> currentEdgesFuture =
                currentContext.getEdges(currentSource, label)
                        .thenApply(currentTargetScopes -> Streams.stream(currentTargetScopes)
                                .map(currentTarget -> new Edge<>(currentSource, label, currentTarget))
                                .collect(CapsuleCollectors.toSet()));

        // Get previous edges for label
        final IFuture<Set.Immutable<Edge<S, L>>> previousEdgesFuture =
                previousContext.getEdges(previousSource, label)
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

            // When all current edges are processed (matched/added), mark remaining previous edges as removed.
            // Eventually,
            scheduleRemovedEdges(previousSource, currentEdges, previousEdges).whenComplete((u, ex2) -> {
                logger.debug("Edge matches for {} ~ {} and label {} finished", currentSource, previousSource, label);
                result.complete(u, ex2);
            });

            return processEdgeMatches(currentEdges, previousEdges);
        };

        future(edgesFuture, k);
        return result;
    }

    private Unit processEdgeMatches(Set.Immutable<Edge<S, L>> currentEdges, Set.Immutable<Edge<S, L>> previousEdges) {
        // Log edges as observed
        previousEdges.forEach(edge -> seenPreviousEdges.put(edge.source, edge.label, edge.target));
        currentEdges.forEach(edge -> seenCurrentEdges.put(edge.source, edge.label, edge.target));

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

            final ICompletableFuture<List<Tuple2<Edge<S, L>, Optional<BiMap.Immutable<S>>>>> matchesResult =
                    new CompletableFuture<>();
            matchesFuture.whenComplete((u, ex2) -> {
                if(ex2 != null) {
                    logger.debug("Error matching edge {} - treat it as undecided.", currentEdge);
                    logger.debug("error:", ex2);
                    failure(ex2);
                    // TODO: special category for undecided edges?
                    matchesResult.complete(Collections.emptyList(), null);
                } else {
                    matchesResult.complete(u);
                }
            });

            future(matchesResult, k2);
        }

        return Unit.unit;
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
            if(!completeIfFailure(future)) {
                waitFors.__insert(EdgeCompleted.of(edge));
                currentEdgeCompleteDelays.put(edge, future);
            }
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
        if(differOps.ownScope(currentScope) && seenCurrentScopes.__insert(currentScope)) {
            logger.trace("scheduling data for current scope {}.", currentScope);
            openCurrentScopes.__insert(currentScope);
            IFuture<Optional<D>> cd = currentContext.datum(currentScope);
            K<Optional<D>> insertCS = (d, ex) -> {
                if(ex == null) {
                    logger.trace("Data for {} complete: ", currentScope, d);
                    currentScopeData.__put(currentScope, d);

                    Collection<S> dataScopes = d.map(differOps::getScopes).orElse(Set.Immutable.of());
                    logger.trace("Scopes observed in datum of {}: {}", currentScope, dataScopes);

                    dataScopes.forEach(this::scheduleCurrentData);
                } else {
                    failure(ex);
                }
                return Unit.unit;
            };
            future(cd, insertCS);
        }
    }

    private void schedulePreviousData(S previousScope) {
        if(differOps.ownScope(previousScope) && seenPreviousScopes.__insert(previousScope)) {
            logger.trace("scheduling data for previous scope {}.", previousScope);
            openPreviousScopes.__insert(previousScope);
            IFuture<Optional<D>> pd = previousContext.datum(previousScope);
            K<Optional<D>> insertPS = (d, ex) -> {
                if(ex == null) {
                    logger.trace("Data for {} complete: ", previousScope, d);
                    previousScopeData.__put(previousScope, d);

                    Collection<S> dataScopes = d.map(differOps::getScopes).orElse(Set.Immutable.of());
                    logger.trace("Scopes observed in datum of {}: {}", previousScope, dataScopes);

                    dataScopes.forEach(this::schedulePreviousData);
                } else {
                    failure(ex);
                }
                return Unit.unit;
            };
            future(pd, insertPS);
        }
    }

    private IFuture<Unit> visitAllEdges(IDifferContext<S, L, D> context, S scope, Action1<Edge<S, L>> visit) {
        final IFuture<Unit> future = context.labels(scope).thenCompose(labels -> {
            return aggregateAll(labels, label -> {
                final IFuture<Iterable<S>> edgesFuture = context.getEdges(scope, label);
                K<Iterable<S>> addEdges = (targets, ex) -> {
                    targets.forEach(target -> {
                        visit.apply(new Edge<>(scope, label, target));
                    });
                    return Unit.unit;
                };
                future(edgesFuture, addEdges);
                return edgesFuture.thenApply(__ -> Unit.unit);
            });
        }).thenApply(__ -> Unit.unit);
        future(future);
        return future;
    }

    private IFuture<Unit> addAllEdges(S currentScope) {
        return visitAllEdges(currentContext, currentScope, this::added);
    }

    private IFuture<Unit> removeAllEdges(S previousScope) {
        return visitAllEdges(previousContext, previousScope, this::removed);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Result processing
    ///////////////////////////////////////////////////////////////////////////

    private void closeCurrentScope(S currentScope) {
        if(!differOps.ownScope(currentScope)) {
            return;
        }

        if(!seenCurrentScopes.contains(currentScope)) {
            throw new IllegalStateException("Closing unobserved scope: " + currentScope);
        }

        if(!matchedScopes.containsKey(currentScope) && !addedScopes.contains(currentScope)) {
            throw new IllegalStateException("Closing scope that is neither matched nor added: " + currentScope);
        }

        logger.trace("closing current scope {} for matching.", currentScope);
        if(!openCurrentScopes.__remove(currentScope)) {
            throw new IllegalStateException("Closing scope that is already closed: " + currentScope);
        }
    }

    private void closePreviousScope(S previousScope) {
        if(!differOps.ownScope(previousScope)) {
            return;
        }

        if(!seenPreviousScopes.contains(previousScope)) {
            throw new IllegalStateException("Closing unobserved scope: " + previousScope);
        }

        if(!matchedScopes.containsValue(previousScope) && !removedScopes.contains(previousScope)) {
            throw new IllegalStateException("Closing scope that is neither matched nor removed: " + previousScope);
        }

        logger.trace("closing previous scope {} for matching.", previousScope);
        if(!openPreviousScopes.__remove(previousScope)) {
            throw new IllegalStateException("Closing scope that is already closed: " + previousScope);
        }
    }

    private Unit match(S currentScope, S previousScope) {
        if(matchedScopes.containsEntry(currentScope, previousScope)) {
            return Unit.unit;
        }
        assertCurrentScopeOpen(currentScope);
        assertPreviousScopeOpen(previousScope);

        logger.debug("Matched {} ~ {}", currentScope, previousScope);
        matchedScopes.put(currentScope, previousScope);
        scheduleCurrentData(currentScope);
        schedulePreviousData(previousScope);
        closeCurrentScope(currentScope);
        closePreviousScope(previousScope);

        logger.trace("Scheduling edge matches for {} ~ {}", currentScope, previousScope);

        if(differOps.ownOrSharedScope(currentScope)) {
            // We can only own edges from scopes that we own, or that are shared with us.
            final IFuture<Set.Immutable<L>> labels = AggregateFuture
                    .apply(waitFor(currentContext.labels(currentScope)), waitFor(previousContext.labels(previousScope)))
                    .thenApply(lbls -> Set.Immutable.union(lbls._1(), lbls._2()))
                    .whenComplete((l, __) -> logger.trace("Labels for {}: {}", currentScope, l));
            final K<Set.Immutable<L>> scheduleMatches = (lbls, ex) -> {
                if(ex == null) {
                    logger.trace("Received labels for {}, scheduling edge matches.", currentScope);
                    aggregateAll(lbls, lbl -> scheduleEdgeMatches(currentScope, previousScope, lbl))
                            .whenComplete((__, ex2) -> {
                                if(ex2 != null) {
                                    logger.trace("Edge matches for {} ~ {} errored.", currentScope, previousScope);
                                    logger.trace("error:", ex2);
                                    failure(ex2);
                                } else {
                                    logger.debug("All edge matches for {} ~ {} finished.", currentScope, previousScope);
                                }
                                previousScopeComplete(previousScope);
                            });
                } else {
                    failure(ex);
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
        matchedOutgoingEdges.put(previous.source, previous);

        scheduleCurrentData(current.target);
        schedulePreviousData(previous.target);

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
        scheduleCurrentData(currentScope);

        logger.trace("Added {}", currentScope);
        addedScopes.__insert(currentScope);
        closeCurrentScope(currentScope);

        addAllEdges(currentScope);

        return Unit.unit;
    }

    private Unit removed(S previousScope) {
        assertPreviousScopeOpen(previousScope);
        schedulePreviousData(previousScope);

        logger.trace("Removed {}", previousScope);
        removedScopes.__insert(previousScope);
        closePreviousScope(previousScope);

        previousScopeProcessed(previousScope);
        removeAllEdges(previousScope).whenComplete((__, ex) -> {
            if(ex != null) {
                failure(ex);
            }
            previousScopeComplete(previousScope);
        });

        return Unit.unit;
    }

    private Unit queue(EdgeMatch match) {
        logger.trace("Queuing delayed match {}", match);
        edgeMatches.add(match);

        return Unit.unit;
    }

    private <R> Unit future(IFuture<R> future, K<R> k) {
        RefBool executed = new RefBool(false);
        waitFor(future).handle((r, ex) -> {
            if(executed.get()) {
                throw new IllegalStateException("Continuation cannot be executed multiple times.");
            }
            executed.set(true);
            diffK(k, r, ex);
            return Unit.unit;
        });
        return Unit.unit;
    }

    private Unit future(IFuture<Unit> future) {
        final K<Unit> k = (u, ex) -> {
            if(ex != null) {
                failure(ex);
            }
            return u;
        };
        return future(future, k);
    }

    private boolean tryFinalizeDiff() {
        logger.debug("Marking all open scopes as added/removed.");
        openCurrentScopes.forEach(this::added);
        openPreviousScopes.forEach(this::removed);

        if(pendingResults.get() == 0 && typeCheckerFinished.get() && !result.isDone() && edgeMatches.isEmpty()) {
            logger.debug("Finalizing diff.");
            Map.Transient<S, D> addedScopes = CapsuleUtil.transientMap();
            currentScopeData.keySet().retainAll(this.addedScopes);
            currentScopeData.forEach((s, d) -> addedScopes.__put(s, d.orElse(differOps.embed(s))));

            Map.Transient<S, D> removedScopes = CapsuleUtil.transientMap();
            previousScopeData.keySet().retainAll(this.removedScopes);
            previousScopeData.forEach((s, d) -> removedScopes.__put(s, d.orElse(differOps.embed(s))));

            Set.Transient<Edge<S, L>> addedEdges = CapsuleUtil.transientSet();
            this.addedEdges.asMap().values().forEach(x -> x.forEach(addedEdges::__insert));

            Set.Transient<Edge<S, L>> removedEdges = CapsuleUtil.transientSet();
            this.removedEdges.asMap().values().forEach(x -> x.forEach(removedEdges::__insert));

            // Clean up pending delays
            previousScopeProcessedDelays.asMap().forEach((s, delays) -> {
                logger.error("Pending previous scope processed delays for {}.", s);
                throw new IllegalStateException("Pending previous scope processed delays for " + s + ".");
            });

            previousScopeCompletedDelays.asMap().forEach((s, delays) -> {
                logger.error("Pending previous scope completed delays for {}.", s);
                throw new IllegalStateException("Pending previous scope completed delays for " + s + ".");
            });

            currentEdgeCompleteDelays.asMap().forEach((edge, delays) -> {
                logger.error("Pending current edge processed delays for {}.", edge);
                throw new IllegalStateException("Pending current edge processed delays for " + edge + ".");
            });

            // @formatter:off
            final ScopeGraphDiff<S, L, D> result = new ScopeGraphDiff<S, L, D>(
                matchedScopes.freeze(),
                matchedEdges.freeze(),
                addedScopes.freeze(),
                addedEdges.freeze(),
                removedScopes.freeze(),
                removedEdges.freeze()
            );
            // @formatter:on
            this.result.complete(result);
            return false;
        }
        return true;
    }

    // Queries

    @Override public IFuture<Optional<S>> match(S previousScope) {
        if(matchedScopes.containsValue(previousScope)) {
            final S currentScope = matchedScopes.getValue(previousScope);
            return CompletableFuture.completedFuture(Optional.of(currentScope));
        }

        if(removedScopes.contains(previousScope)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        final ICompletableFuture<Unit> result = new CompletableFuture<>();
        if(!completeIfFailure(result)) {
            previousScopeProcessedDelays.put(previousScope, result);
            waitFors.__insert(ScopeProcessed.of(previousScope));
        }
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

        final ICompletableFuture<Unit> result = new CompletableFuture<>();
        if(!completeIfFailure(result)) {
            waitFors.__insert(ScopeCompleted.of(previousScope));
            previousScopeCompletedDelays.put(previousScope, result);
        }

        return result.thenApply(__ -> buildScopeDiff(previousScope));
    }

    private IScopeDiff<S, L, D> buildScopeDiff(S previousScope) {
        if(matchedScopes.containsValue(previousScope)) {
            S currentScope = matchedScopes.getValue(previousScope);
            return Matched.of(currentScope, addedEdges.get(currentScope), matchedOutgoingEdges.get(previousScope),
                    removedEdges.get(previousScope));
        }
        return Removed.of();
    }

    // Events

    private void previousScopeProcessed(S previousScope) {
        logger.trace("Complete (PS) {}", previousScope);
        waitFors.__remove(ScopeProcessed.of(previousScope));
        previousScopeProcessedDelays.removeKey(previousScope).forEach(c -> c.complete(Unit.unit));
        logger.trace("Finished complete (PS) {}", previousScope);
    }

    private void previousScopeComplete(S previousScope) {
        logger.trace("Complete (PSC) {}", previousScope);
        completedPreviousScopes.__insert(previousScope);
        waitFors.__remove(ScopeCompleted.of(previousScope));
        previousScopeCompletedDelays.removeKey(previousScope).forEach(c -> c.complete(Unit.unit));
        logger.trace("Finished complete (PSC) {}", previousScope);
    }

    private void currentEdgeComplete(Edge<S, L> current) {
        logger.trace("Complete (CE), {}", current);
        waitFors.__remove(EdgeCompleted.of(current));
        currentEdgeCompleteDelays.removeKey(current).forEach(c -> c.complete(Unit.unit));
        logger.trace("Finished complete (CE), {}", current);
    }

    private void failure(Throwable ex) {
        failure = ex;
        result.completeExceptionally(ex);
        previousScopeProcessedDelays.asMap().forEach((s, delays) -> delays.forEach(d -> {
            d.completeExceptionally(ex);
        }));
        previousScopeCompletedDelays.asMap().forEach((s, delays) -> delays.forEach(d -> {
            d.completeExceptionally(ex);
        }));
        currentEdgeCompleteDelays.asMap().forEach((s, delays) -> delays.forEach(d -> {
            d.completeExceptionally(ex);
        }));
    }

    private boolean completeIfFailure(ICompletableFuture<?> future) {
        if(failure != null) {
            future.completeExceptionally(failure);
            return true;
        }
        return false;
    }

    // Helper methods and classes

    private static <T, R> IFuture<List<R>> aggregateAll(Iterable<T> items, Function1<T, IFuture<R>> mapper) {
        return AggregateFuture.of(Streams.stream(items).map(mapper::apply).collect(Collectors.toSet()));
    }

    private boolean successfullyCompleted() {
        return result.isDone() && failure == null;
    }

    private boolean isCurrentScopeOpen(S scope) {
        return !successfullyCompleted() && !matchedScopes.containsKey(scope) && !addedScopes.contains(scope);
    }

    private boolean isPreviousScopeOpen(S scope) {
        return !successfullyCompleted() && !matchedScopes.containsValue(scope) && !removedScopes.contains(scope);
    }

    private boolean isCurrentEdgeOpen(Edge<S, L> edge) {
        return !successfullyCompleted() && !matchedEdges.containsKey(edge) && !addedEdges.containsValue(edge);
    }

    private boolean isPreviousEdgeOpen(Edge<S, L> edge) {
        return !successfullyCompleted() && !matchedEdges.containsValue(edge) && !removedEdges.containsValue(edge);
    }

    private void assertCurrentScopeOpen(S scope) {
        if(!isCurrentScopeOpen(scope)) {
            throw new IllegalStateException("Scope " + scope + " is already matched/added.");
        }
    }

    private void assertPreviousScopeOpen(S scope) {
        if(!isPreviousScopeOpen(scope)) {
            throw new IllegalStateException("Scope " + scope + " is already matched/removed.");
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

    private <U> IFuture<U> waitFor(IFuture<U> future) {
        pendingResults.incrementAndGet();
        return future.whenComplete((__, ex) -> pendingResults.decrementAndGet());
    }

    ///////////////////////////////////////////////////////////////////////////
    // K
    ///////////////////////////////////////////////////////////////////////////

    @FunctionalInterface
    private interface K<R> {
        Unit k(R result, Throwable ex);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Tokens
    ///////////////////////////////////////////////////////////////////////////

    private interface IToken<S, L> {

    }

    private static class ScopeProcessed<S, L> implements IToken<S, L> {

        private final S previousScope;

        private ScopeProcessed(S previousScope) {
            this.previousScope = previousScope;
        }

        public static <S, L> ScopeProcessed<S, L> of(S previousScope) {
            return new ScopeProcessed<>(previousScope);
        }

        @Override public String toString() {
            return "ScopeProcessed{" + previousScope + "}";
        }

        @Override public boolean equals(Object obj) {
            if(obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            @SuppressWarnings("unchecked") final ScopeProcessed<S, L> other = (ScopeProcessed<S, L>) obj;
            return other.previousScope.equals(previousScope);
        }

        @Override public int hashCode() {
            return previousScope.hashCode();
        }

    }

    private static class ScopeCompleted<S, L> implements IToken<S, L> {

        private final S previousScope;

        private ScopeCompleted(S previousScope) {
            this.previousScope = previousScope;
        }

        public static <S, L> ScopeCompleted<S, L> of(S previousScope) {
            return new ScopeCompleted<>(previousScope);
        }

        @Override public String toString() {
            return "ScopeCompleted{" + previousScope + "}";
        }

        @Override public boolean equals(Object obj) {
            if(obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            @SuppressWarnings("unchecked") final ScopeCompleted<S, L> other = (ScopeCompleted<S, L>) obj;
            return other.previousScope.equals(previousScope);
        }

        @Override public int hashCode() {
            return previousScope.hashCode();
        }

    }

    private static class EdgeCompleted<S, L> implements IToken<S, L> {

        private final Edge<S, L> currentEdge;

        private EdgeCompleted(Edge<S, L> currentEdge) {
            this.currentEdge = currentEdge;
        }

        public static <S, L> EdgeCompleted<S, L> of(Edge<S, L> currentEdge) {
            return new EdgeCompleted<>(currentEdge);
        }

        @Override public String toString() {
            return "EdgeCompleted{" + currentEdge + "}";
        }

        @Override public boolean equals(Object obj) {
            if(obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            @SuppressWarnings("unchecked") final EdgeCompleted<S, L> other = (EdgeCompleted<S, L>) obj;
            return other.currentEdge.equals(currentEdge);
        }

        @Override public int hashCode() {
            return currentEdge.hashCode();
        }

    }

}
