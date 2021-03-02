package mb.statix.concurrent.p_raffrayi.diff;

import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.unit.Unit;

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

    private final IScopeGraphDifferContext<S, L, D> context;
    private final CompletableFuture<ScopeGraphDiff<S, L, D>> complete = new CompletableFuture<>();

    // Intermediate match results

    private final BiMap.Transient<S> matchedScopes = BiMap.Transient.of();
    private final BiMap.Transient<Edge<S, L>> matchedEdges = BiMap.Transient.of();

    private final Map.Transient<S, Optional<D>> addedScopes = Map.Transient.of();
    private final Map.Transient<S, Optional<D>> removedScopes = Map.Transient.of();

    private final MultiSetMap.Transient<S, Edge<S, L>> addedEdges = MultiSetMap.Transient.of();
    private final MultiSetMap.Transient<S, Edge<S, L>> removedEdges = MultiSetMap.Transient.of();

    // Observations
    // TODO: are these needed? (Maybe for finalization)

    private final Set.Transient<S> seenCurrentScopes = Set.Transient.of();
    private final IRelation3.Transient<S, L, S> seenCurrentEdges = HashTrieRelation3.Transient.of();

    private final Set.Transient<S> seenPreviousScopes = Set.Transient.of();
    private final IRelation3.Transient<S, L, S> seenPreviousEdges = HashTrieRelation3.Transient.of();

    // Delays

    /**
     * Delays to be fired when the previous scope key is completed.
     */
    private final MultiSetMap.Transient<S, ICompletable<Unit>> delays_ps = MultiSetMap.Transient.of();

    /**
     * Delays to be fired when edge is matched/added
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

    private void fixedpoint() {
        logger.debug("Calculating fixpoint");
        fixedPointNesting.incrementAndGet();

        try {
            try {
                while(!edgeMatches.isEmpty()) {
                    EdgeMatch m = edgeMatches.remove();
                    matchEdge(m.currentEdge, m.previousEdges);
                }

                logger.debug("Reached fixpoint. Nesting level: {}, Pending: {}", fixedPointNesting.get(),
                    pendingResults.get());
            } finally {
                fixedPointNesting.decrementAndGet();
            }

            Set.Transient<Edge<S, L>> addedEdges = CapsuleUtil.transientSet();
            this.addedEdges.entrySet().stream().forEach(x -> x.getValue().forEach(addedEdges::__insert));
            Set.Transient<Edge<S, L>> removedEdges = CapsuleUtil.transientSet();
            this.removedEdges.entrySet().stream().forEach(x -> x.getValue().forEach(removedEdges::__insert));

            if(fixedPointNesting.get() == 0 && pendingResults.get() == 0 && typeCheckerFinished.get()) {
                // @formatter:off
                ScopeGraphDiff<S, L, D> result = new ScopeGraphDiff<S, L, D>(
                    matchedScopes.freeze(),
                    matchedEdges.freeze(),
                    addedScopes.freeze(),
                    addedEdges.freeze(),
                    removedScopes.freeze(),
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
        logger.debug("Continuing");
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
        logger.debug("Finished continuation");
    }

    private Unit matchEdge(Edge<S, L> currentEdge, Immutable<Edge<S, L>, BiMap.Immutable<S>> previousEdges) {
        logger.debug("Matching edge {} with candidates {}", currentEdge, previousEdges);
        if(previousEdges.isEmpty()) {
            return added(currentEdge);
        }
        final Entry<Edge<S, L>, BiMap.Immutable<S>> previousEdge = previousEdges.entrySet().iterator().next();

        if(matchScopes(previousEdge.getValue())) {
            logger.debug("Matching {} with {} succeeded.", currentEdge, previousEdge);
            return match(currentEdge, previousEdge.getKey());
        } else {
            logger.debug("Matching {} with {} failed, queueing match for remainder.", currentEdge, previousEdge);
            return queue(new EdgeMatch(currentEdge, previousEdges.__remove(previousEdge.getKey())));
        }
    }

    public boolean matchScopes(BiMap.Immutable<S> scopes) {
        logger.debug("Matching scopes {}", scopes);
        seenCurrentScopes.__insertAll(scopes.keySet());
        seenPreviousScopes.__insertAll(scopes.valueSet());

        final BiMap<S> newMatches;
        if((newMatches = canScopesMatch(scopes).orElse(null)) == null) {
            logger.debug("Scopes cannot match");
            return false;
        }

        logger.debug("Matching succeeded.");
        for(Map.Entry<S, S> entry : newMatches.entrySet()) {
            match(entry.getKey(), entry.getValue());
        }
        return true;
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
    private void scheduleEdgeMatches(S currentSource, S previousSource, L label) {
        logger.trace("Scheduling edge matches for {} ~ {} and label {}", currentSource, previousSource, label);
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

            scheduleRemovedEdges(currentEdges, previousEdges);

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
    }

    private void scheduleRemovedEdges(Set.Immutable<Edge<S, L>> currentEdges, Set.Immutable<Edge<S, L>> previousEdges) {
        aggregateAll(currentEdges, edge -> {
            ICompletableFuture<Unit> future = new CompletableFuture<>();
            delays_ce.put(edge, future);
            return future;
        }).thenAccept(__ -> {
            previousEdges.forEach(edge -> {
                if(!matchedEdges.containsValue(edge)) {
                    removed(edge);
                }
            });
        });
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

        CompletableFuture<Boolean> result = new CompletableFuture<>();
        if(context.ownScope(currentScope)) {
            AggregateFuture.apply(context.currentDatum(currentScope), context.previousDatum(previousScope))
                .thenAccept(r -> {
                    final Optional<D> currentData = r._1();
                    final Optional<D> previousData = r._2();
                    if(currentData.isPresent() != previousData.isPresent()) {
                        result.complete(false);
                    } else if(currentData.isPresent() && previousData.isPresent()) {
                        context
                            .matchDatums(currentData.get(), previousData.get(),
                                (leftScope, rightScope) -> scopeMatch(leftScope, rightScope, req))
                            .thenAccept(result::complete);
                    } else {
                        result.complete(true);
                    }
                });
        } else {
            // We do not own the scope, hence ask owner to which current scope it is matched.
            context.externalMatch(previousScope).thenAccept(m -> {
                if(m.isPresent()) {
                    // Insert new remote match
                    match(m.get(), previousScope);
                    result.complete(m.get().equals(currentScope));
                } else {
                    result.complete(false);
                }
            });
        }
        return result;
    }

    // Result processing
    private Unit match(S currentScope, S previousScope) {
        matchedScopes.put(currentScope, previousScope);

        logger.trace("Scheduling edge matches for {} ~ {}", currentScope, previousScope);

        if(context.ownOrSharedScope(currentScope)) {
            // We can only own edges from scopes that we own, or that are shared with us.
            final IFuture<Iterable<L>> labels = context.labels(currentScope)
                .whenComplete((l, __) -> logger.trace("Labels for {}: {}", currentScope, l));
            final K<Iterable<L>> scheduleMatches = (lbls) -> {
                logger.trace("Received labels for {}, scheduling edge matches.");
                lbls.forEach(lbl -> {
                    scheduleEdgeMatches(currentScope, previousScope, lbl);
                });
                return Unit.unit;
            };
            future(labels, scheduleMatches);
        }

        logger.trace("Scheduling scope observations for {} ~ {}", currentScope, previousScope);

        // Collect all scopes in data term of current scope
        IFuture<Set.Immutable<S>> cd = context.currentDatum(currentScope).thenApply((Optional<D> d) -> {
            logger.trace("Data for {} complete: ", currentScope, d);
            return d.map(context::getCurrentScopes).orElse(Set.Immutable.of());
        });
        K<Set.Immutable<S>> insertCS = (dataScopes) -> {
            logger.trace("Scopes observed in datum of {}: {}", currentScope, dataScopes);
            seenCurrentScopes.__insertAll(dataScopes);
            return Unit.unit;
        };
        future(cd, insertCS);

        // Collect all scopes in data term of previous scope
        IFuture<Set.Immutable<S>> pd = context.previousDatum(previousScope).thenApply((Optional<D> d) -> {
            logger.trace("Data for {} complete: ", previousScope, d);
            return d.map(context::getPreviousScopes).orElse(Set.Immutable.of());
        });
        K<Set.Immutable<S>> insertPS = (dataScopes) -> {
            logger.trace("Scopes observed in datum of {}: {}", previousScope, dataScopes);
            seenPreviousScopes.__insertAll(dataScopes);
            return Unit.unit;
        };
        future(pd, insertPS);

        return Unit.unit;
    }

    private Unit match(Edge<S, L> current, Edge<S, L> previous) {
        logger.debug("Matching {} ~ {}", current, previous);
        matchedEdges.put(current, previous);
        // TODO activate delays
        return Unit.unit;
    }

    private Unit added(Edge<S, L> edge) {
        logger.debug("Added {}", edge);
        addedEdges.put(edge.source, edge);

        delays_ce.get(edge).forEach(c -> c.complete(Unit.unit));

        return Unit.unit;
    }

    private Unit removed(Edge<S, L> edge) {
        logger.debug("Removed {}", edge);
        removedEdges.put(edge.source, edge);

        return Unit.unit;
    }

    private Unit queue(EdgeMatch match) {
        logger.debug("Queuing delayed match {}", match);
        edgeMatches.add(match);

        // Todo

        return Unit.unit;
    }

    private <R> Unit future(IFuture<R> future, K<R> k) {
        pendingResults.incrementAndGet();
        future.handle((r, ex) -> {
            pendingResults.decrementAndGet();
            diffK(k, r, ex);
            return Unit.unit;
        });
        return Unit.unit;
    }

    // Queries

    @Override public IFuture<Optional<S>> match(S previousScope) {
        if(removedScopes.containsKey(previousScope)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        if(matchedScopes.containsValue(previousScope)) {
            S currentScope = matchedScopes.getValue(previousScope);
            return CompletableFuture.completedFuture(Optional.of(currentScope));
        }
        final ICompletableFuture<Unit> result = new CompletableFuture<>();
        delays_ps.put(previousScope, result);
        return result.thenApply(__ -> {
            // When previousScope is released, it should be either in matchedScopes
            // or in removedScopes.
            if(!matchedScopes.containsValue(previousScope) && !removedScopes.containsKey(previousScope)) {
                logger.error("Scope {} is completed, but not added to matched or removed set.", previousScope);
                throw new IllegalStateException(
                    "Scope " + previousScope + " is completed, but not added to matched or removed set.");
            }
            // If null, it is in removed scopes, so returning is safe.
            return Optional.ofNullable(matchedScopes.getValue(previousScope));
        });
    }

    @Override public void typeCheckerFinished() {
        typeCheckerFinished.set(true);
        fixedpoint();
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
