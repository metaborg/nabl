package mb.statix.concurrent.p_raffrayi.diff;

import java.util.Deque;
import java.util.List;
import java.util.Map.Entry;
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
import mb.nabl2.util.Tuple2;
import mb.statix.concurrent.actors.futures.AggregateFuture;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.scopegraph.diff.BiMap;
import mb.statix.scopegraph.diff.Edge;
import mb.statix.scopegraph.diff.ScopeGraphDiff;

public class ScopeGraphDiffer<S, L, D> implements IScopeGraphDiffer<S, L, D> {
    
    private static final ILogger logger = LoggerUtils.logger(ScopeGraphDiffer.class);
    
    private final IScopeGraphDifferContext<S, L, D> context;
    private final CompletableFuture<ScopeGraphDiff<S, L, D>> complete = new CompletableFuture<>();

    private final BiMap.Transient<S> matchedScopes = BiMap.Transient.of();
    private final BiMap.Transient<Edge<S, L>> matchedEdges = BiMap.Transient.of();

    private final Set.Transient<S> seenCurrentScopes = Set.Transient.of();
    private final Set.Transient<Edge<S, L>> seenCurrentEdges = Set.Transient.of();

    private final Set.Transient<S> seenPreviousScopes = Set.Transient.of();
    private final Set.Transient<Edge<S, L>> seenPreviousEdges = Set.Transient.of();
    
    private final Set.Transient<S> addedScopes = Set.Transient.of();
    private final Set.Transient<S> removedScopes = Set.Transient.of();
    
    private final Set.Transient<Edge<S, L>> addedEdges = Set.Transient.of();
    private final Set.Transient<Edge<S, L>> removedEdges = Set.Transient.of();
    
    private final AtomicInteger pendingResults = new AtomicInteger(0);
    private final AtomicInteger fixedPointNesting = new AtomicInteger(0);

    private final Queue<EdgeMatch> edgeMatches = new PriorityQueue<>();

    public ScopeGraphDiffer(IScopeGraphDifferContext<S, L, D> context) {
        this.context = context;
    }
    
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
        } catch (Throwable ex) {
            complete.completeExceptionally(ex);
        }
        return complete;
    }

    private void fixedpoint() {
        logger.debug("Calculating fixpoint");
        fixedPointNesting.incrementAndGet();
        
        while(!edgeMatches.isEmpty()) {
            EdgeMatch m = edgeMatches.remove();
            matchEdge(m.currentEdge, m.previousEdges);
        }
        
        logger.debug("Reached fixpoint. Nesting level: {}, Pending: {}", pendingResults.get(), fixedPointNesting.get());
        
        if(pendingResults.get() == 0 && fixedPointNesting.decrementAndGet() == 0) {
            
        }
    }
    
    private <R> void diffK(K<R> k, R r, Throwable ex) {
        logger.debug("Continuing");
        try {
            k.k(r, ex);
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
    
    private boolean matchScopes(BiMap.Immutable<S> scopes) {
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
                logger.trace("Matching {} with {} is not allowed: one or both is already matched.", currentScope, previousScope);
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
        
        K<Tuple2<Set.Immutable<Edge<S, L>>, Set.Immutable<Edge<S, L>>>> k = (res, ex) -> {
            Set.Immutable<Edge<S, L>> currentEdges = res._1();
            Set.Immutable<Edge<S, L>> previousEdges = res._2();
            
            this.seenPreviousEdges.__insertAll(previousEdges);
            this.seenCurrentEdges.__insertAll(currentEdges);

            for(Edge<S, L> currentEdge : currentEdges) {
                IFuture<List<Tuple2<Edge<S, L>, Optional<BiMap.Immutable<S>>>>> matchesFuture = aggregateAll(previousEdges, previousEdge -> {
                    return matchScopes(currentEdge.target, previousEdge.target)
                        .thenApply(matchedScopes -> Tuple2.of(previousEdge, matchedScopes));
                });
                
                K<List<Tuple2<Edge<S, L>, Optional<BiMap.Immutable<S>>>>> k2 = (r, ex2) -> {
                    final Map.Transient<Edge<S, L>, BiMap.Immutable<S>> matchingPreviousEdges = Map.Transient.of();
                    r.stream().filter(x -> x._2().isPresent())
                        .map(x -> Tuple2.of(x._1(), x._2().get()))
                        .map(x -> Tuple2.of(x._1(), canScopesMatch(x._2())))
                        .filter(x -> x._2().isPresent())
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
        context.currentDatum(currentScope).thenAccept(currentData -> {
            context.previousDatum(previousScope).thenAccept(previousData -> {
                if(currentData.isPresent() != previousData.isPresent()) {
                    result.complete(false);
                } else if(currentData.isPresent() && previousData.isPresent()) {
                    context.matchDatums(currentData.get(), previousData.get(), (leftScope, rightScope) -> scopeMatch(leftScope, rightScope, req))
                        .thenAccept(result::complete);
                }
            });
        });
        
        return result;
    }
    
    // Result processing
    
    private Unit match(S currentScope, S previousScope) {
        matchedScopes.put(currentScope, previousScope);

        logger.trace("Scheduling edge matches for {} ~ {}", currentScope, previousScope);
        
        final IFuture<Iterable<L>> labels = context.labels(currentScope).whenComplete((l, ex) -> logger.trace("Labels for {}: {}", currentScope, l));
        final K<Iterable<L>> scheduleMatches = (lbls, ex) -> {
            logger.trace("Received labels for {}, scheduling edge matches.");
            lbls.forEach(lbl -> {
                scheduleEdgeMatches(currentScope, previousScope, lbl);
            });
            return Unit.unit;
        };
        future(labels, scheduleMatches);
        
        logger.trace("Scheduling scope observations for {} ~ {}", currentScope, previousScope);

        // Collect all scopes in data term of current scope
        IFuture<Set.Immutable<S>> cd = context.currentDatum(currentScope).thenApply((Optional<D> d) -> {
            logger.trace("Data for {} complete: ", currentScope, d);
            return d.map(context::getCurrentScopes).orElse(Set.Immutable.of());
        });
        K<Set.Immutable<S>> insertCS = (dataScopes, ex) -> {
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
        K<Set.Immutable<S>> insertPS = (dataScopes, ex) -> {
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
        addedEdges.__insert(edge);
        
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
    
    // Result finalizing

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
        Unit k(R result, Throwable ex);
    }
}