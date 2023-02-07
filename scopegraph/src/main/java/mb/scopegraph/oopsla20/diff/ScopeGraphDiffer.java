package mb.scopegraph.oopsla20.diff;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;

import org.metaborg.util.collection.BiMap;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.tuple.Tuple2;
import org.metaborg.util.unit.Unit;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Streams;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.reference.EdgeOrData;

public class ScopeGraphDiffer<S, L, D> {

    private static final ILogger logger = LoggerUtils.logger(ScopeGraphDiffer.class);

    private final IScopeGraph.Immutable<S, L, D> previous;
    private final ScopeGraphDifferOps<S, D> diffOps;
    private final ScopeGraphStatusOps<S, L> statusOps;

    private ScopeGraphDiffer(IScopeGraph.Immutable<S, L, D> previous, ScopeGraphDifferOps<S, D> diffOps,
        ScopeGraphStatusOps<S, L> statusOps) {
        this.previous = previous;
        this.diffOps = diffOps;
        this.statusOps = statusOps;
    }

    public DifferState.Immutable<S, L, D> initDiff(S s0current, S s0previous) {
        DifferState.Transient<S, L, D> state = DifferState.Transient.of();
        if(!statusOps.closed(s0current, EdgeOrData.data())) {
            throw new IllegalStateException("Current root scope must be closed for data when initializing differ");
        }
        final Optional<BiMap.Immutable<S>> matches = canScopesMatch(state, BiMap.Immutable.of(s0current, s0previous));
        if(!matches.isPresent()) {
            throw new IllegalStateException("Cannot match root scopes");
        }
        state.putAllMatchedScopes(matches.get());
        // Schedule match calculation for all outgoing edges
        statusOps.allLabels(s0current).forEach(lbl -> {
            state.edgeDelays().put(s0current, lbl);
        });
        return state.freeze();
    }

    /**
     * Update the diff state with the new changes in the current scope graph.
     *
     * @param current
     *            The updated current scope graph. Note that the new current graph must be an extension of the previous
     *            current graph.
     * @param initialState
     *            The state of the previous differ run.
     * @return Updated differ state. Will always be an extension of {@code initialState}.
     */
    public DifferState.Immutable<S, L, D> doDiff(IScopeGraph.Immutable<S, L, D> current,
        DifferState.Immutable<S, L, D> initialState, Multimap<S, EdgeOrData<L>> activations) {
        DifferState.Transient<S, L, D> state = initialState.melt();

        final Queue<EdgeMatch> worklist = new LinkedList<>();

        activations.entries().forEach(activation -> {
            S scope = activation.getKey();
            EdgeOrData<L> label = activation.getValue();
            assertClosed(scope, label);
            label.match(() -> {
                state.dataDelays().removeValues(scope).forEach(removedDelay -> {
                    if(!state.dataDelays().containsValue(removedDelay)) {
                        S currentScope = removedDelay._1();
                        if(state.matchedScopes().containsKey(currentScope)) {
                            S previousScope = state.matchedScopes().getKey(currentScope);
                            worklist.addAll(
                                scheduleMatches(current, state, currentScope, previousScope, removedDelay._2()));
                        } else {
                            logger.trace("Activating yet unmatched scope {}", currentScope);
                        }

                    }
                });
                return Unit.unit;
            }, l -> {
                if(state.matchedScopes().containsKey(scope)) {
                    S previousScope = state.matchedScopes().getKey(scope);
                    worklist.addAll(scheduleMatches(current, state, scope, previousScope, l));
                } else {
                    logger.trace("Activating yet unmatched scope {}", scope);
                }
                return Unit.unit;
            });
        });

        while(!worklist.isEmpty()) {
            final EdgeMatch m = worklist.remove();
            logger.trace("Processing match {}", m);
            worklist.addAll(matchEdge(current, state, m.currentEdge, m.previousEdges));
        }
        return state.freeze();
    }

    /**
     * Match the scopes and schedule resulting edge matches.
     */
    private Tuple2<Boolean, Queue<EdgeMatch>> matchScopes(IScopeGraph.Immutable<S, L, D> current,
        DifferState.Transient<S, L, D> state, BiMap.Immutable<S> scopes) {
        state.seenCurrentScopes().__insertAll(scopes.keySet());
        state.seenPreviousScopes().__insertAll(scopes.valueSet());
        final BiMap<S> newMatches;
        if((newMatches = canScopesMatch(state, scopes).orElse(null)) == null) {
            return Tuple2.of(false, new LinkedList<EdgeMatch>());
        }
        state.putAllMatchedScopes(newMatches);
        final Queue<EdgeMatch> newEdgeMatches = new LinkedList<>();
        for(Map.Entry<S, S> entry : newMatches.entrySet()) {
            final S currentScope = entry.getKey();
            final S previousScope = entry.getValue();
            for(L label : statusOps.allLabels(currentScope)) {
                if(statusOps.closed(currentScope, EdgeOrData.edge(label))) {
                    newEdgeMatches.addAll(scheduleMatches(current, state, currentScope, previousScope, label));
                } else {
                    logger.trace("Delaying matching of ({}, {})", currentScope, label);
                    state.edgeDelays().put(currentScope, label);
                }
            }
        }
        logger.trace("Matching {} succeeded, sceduling {}", scopes, newEdgeMatches);
        return Tuple2.of(true, newEdgeMatches);
    }

    private Queue<EdgeMatch> scheduleMatches(IScopeGraph.Immutable<S, L, D> current,
        DifferState.Transient<S, L, D> state, final S currentScope, final S previousScope, L label) {
        // current scope is matched, which can only happen if it is closed wrt $
        assertClosed(currentScope, EdgeOrData.data());

        final Optional<D> currentData = current.getData(currentScope);
        currentData.ifPresent(d -> state.seenCurrentScopes().__insertAll(diffOps.getCurrentScopes(d)));
        final Optional<D> previousData = previous.getData(previousScope);
        previousData.ifPresent(d -> state.seenPreviousScopes().__insertAll(diffOps.getPreviousScopes(d)));

        assertClosed(currentScope, EdgeOrData.edge(label));
        return scheduleEdgeMatches(current, state, currentScope, previousScope, label);
    }

    /**
     * Check if scopes can match under the current match, and return the new matches it would introduce.
     */
    private Optional<BiMap.Immutable<S>> canScopesMatch(DifferState.Transient<S, L, D> state,
        BiMap.Immutable<S> scopes) {
        final BiMap.Transient<S> newMatches = BiMap.Transient.of();
        for(Map.Entry<S, S> entry : scopes.entrySet()) {
            final S currentScope = entry.getKey();
            final S previousScope = entry.getValue();
            if(!diffOps.isMatchAllowed(currentScope, previousScope)) {
                return Optional.empty();
            } else if(!state.canPutMatchedScope(currentScope, previousScope)) {
                return Optional.empty();
            } else if(state.containsMatchedScopes(currentScope, previousScope)) {
                // skip this pair as it was already matched
            } else {
                newMatches.put(currentScope, previousScope);
            }
        }
        return Optional.of(newMatches.freeze());
    }

    /**
     * Schedule edge matches from the given source scopes.
     */
    private Queue<EdgeMatch> scheduleEdgeMatches(IScopeGraph.Immutable<S, L, D> current,
        DifferState.Transient<S, L, D> state, S currentSource, S previousSource, L label) {

        final Set.Immutable<Edge<S, L>> currentEdges = Streams.stream(current.getEdges(currentSource, label))
            .map(currentTarget -> new Edge<>(currentSource, label, currentTarget)).collect(CapsuleCollectors.toSet());
        state.seenCurrentEdges().__insertAll(currentEdges);

        Collection<S> scopeDelays =
            currentEdges.stream().filter(edge -> !statusOps.closed(edge.target, EdgeOrData.data()))
                .map(edge -> edge.target).collect(Collectors.toSet());

        if(!scopeDelays.isEmpty()) {
            logger.trace("Delay matching of ({}, {}) because some target data is not yet set", currentSource, label);
            scopeDelays.forEach(s -> state.dataDelays().put(s, Tuple2.of(currentSource, label)));
            return new LinkedList<>();
        }

        final Set.Immutable<Edge<S, L>> previousEdges = Streams.stream(previous.getEdges(previousSource, label))
            .map(previousTarget -> new Edge<>(previousSource, label, previousTarget))
            .collect(CapsuleCollectors.toSet());
        state.seenPreviousEdges().__insertAll(previousEdges);

        Queue<EdgeMatch> newMatches = new LinkedList<>();

        for(Edge<S, L> currentEdge : currentEdges) {
            final Map.Transient<Edge<S, L>, BiMap.Immutable<S>> matchingPreviousEdges = Map.Transient.of();
            for(Edge<S, L> previousEdge : previousEdges) {
                final BiMap.Immutable<S> req;
                if((req = matchScopes(current, currentEdge.target, previousEdge.target).orElse(null)) != null) {
                    if(!canScopesMatch(state, req).isPresent()) {
                        // already discard options that are not possible under current matches
                        continue;
                    }
                    matchingPreviousEdges.__put(previousEdge, req);
                }
            }
            final EdgeMatch match = new EdgeMatch(currentEdge, matchingPreviousEdges.freeze());
            logger.trace("Scheduling {}", match);
            newMatches.add(match);
        }
        return newMatches;
    }

    private Queue<EdgeMatch> matchEdge(IScopeGraph.Immutable<S, L, D> current, DifferState.Transient<S, L, D> state,
        Edge<S, L> currentEdge, Map.Immutable<Edge<S, L>, BiMap.Immutable<S>> previousEdges) {
        for(Entry<Edge<S, L>, BiMap.Immutable<S>> previousEdge : previousEdges.entrySet()) {
            // Match current edge eagerly to first eligible previous edges
            Tuple2<Boolean, Queue<EdgeMatch>> matchResult = matchScopes(current, state, previousEdge.getValue());
            if(matchResult._1()) {
                state.putMatchedEdge(currentEdge, previousEdge.getKey());
                return matchResult._2();
            }
        }
        return new LinkedList<>();
    }

    /**
     * Compute the patch required to match two scopes and their data.
     */
    private Optional<BiMap.Immutable<S>> matchScopes(IScopeGraph.Immutable<S, L, D> current, S currentScope,
        S previousScope) {
        final BiMap.Transient<S> _matches = BiMap.Transient.of();
        if(!scopeMatch(current, currentScope, previousScope, _matches)) {
            return Optional.empty();
        }
        final BiMap.Immutable<S> matches = _matches.freeze();
        return Optional.of(matches);
    }

    private boolean scopeMatch(IScopeGraph.Immutable<S, L, D> current, S currentScope, S previousScope,
        BiMap.Transient<S> req) {
        if(!diffOps.isMatchAllowed(currentScope, previousScope)) {
            return false;
        }
        if(!req.canPut(currentScope, previousScope)) {
            return false;
        }
        if(req.containsEntry(currentScope, previousScope)) {
            return true;
        }
        req.put(currentScope, previousScope);
        final Optional<D> currentData = current.getData(currentScope);
        final Optional<D> previousData = previous.getData(previousScope);
        if(currentData.isPresent() != previousData.isPresent()) {
            return false;
        }
        if(currentData.isPresent() && previousData.isPresent()) {
            if(!diffOps.matchDatums(currentData.get(), previousData.get(),
                (leftScope, rightScope) -> scopeMatch(current, leftScope, rightScope, req))) {
                // data cannot be matched
                return false;
            }
        }
        return true;
    }

    public ScopeGraphDiff<S, L, D> finalize(IScopeGraph.Immutable<S, L, D> current,
        DifferState.Immutable<S, L, D> initialState) {
        final DifferState.Transient<S, L, D> state = initialState.melt();
        final Map.Transient<S, D> addedScopes = Map.Transient.of();
        final Set.Transient<Edge<S, L>> addedEdges = Set.Transient.of();
        finishDiff(current, diffOps::getCurrentScopes, state.seenCurrentScopes(), state.seenCurrentEdges(),
            state.matchedScopes().keySet(), state.matchedEdges().keySet(), addedScopes, addedEdges);

        final Map.Transient<S, D> removedScopes = Map.Transient.of();
        final Set.Transient<Edge<S, L>> removedEdges = Set.Transient.of();
        finishDiff(previous, diffOps::getPreviousScopes, state.seenPreviousScopes(), state.seenPreviousEdges(),
            state.matchedScopes().valueSet(), state.matchedEdges().valueSet(), removedScopes, removedEdges);

        final ScopeGraphDiff<S, L, D> diff =
            new ScopeGraphDiff<>(state.matchedScopes().freeze(), state.matchedEdges().freeze(), addedScopes.freeze(),
                addedEdges.freeze(), removedScopes.freeze(), removedEdges.freeze());
        return diff;
    }

    private void finishDiff(IScopeGraph<S, L, D> scopeGraph, Function1<D, java.util.Set<S>> getScopes,
        Collection<S> seenScopes, Collection<Edge<S, L>> seenEdges, java.util.Set<S> matchedScopes,
        java.util.Set<Edge<S, L>> matchedEdges, Map.Transient<S, D> missedScopes,
        Set.Transient<Edge<S, L>> missedEdges) {

        final Deque<S> scopeList = new ArrayDeque<>(seenScopes);
        final Deque<Edge<S, L>> edgeList = new ArrayDeque<>(seenEdges);

        while(!scopeList.isEmpty() || !edgeList.isEmpty()) {
            while(!scopeList.isEmpty()) {
                final S scope = scopeList.pop();
                if(matchedScopes.contains(scope) || missedScopes.containsKey(scope)) {
                    continue;
                }
                final Optional<D> datum = scopeGraph.getData(scope);
                missedScopes.__put(scope, datum.orElse(null));
                if(datum.isPresent()) {
                    scopeList.addAll(getScopes.apply(datum.get()));
                }
                for(L label : scopeGraph.getLabels()) {
                    for(S target : scopeGraph.getEdges(scope, label)) {
                        final Edge<S, L> edge = new Edge<>(scope, label, target);
                        edgeList.add(edge);
                    }
                }
            }
            while(!edgeList.isEmpty()) {
                final Edge<S, L> edge = edgeList.pop();
                if(matchedEdges.contains(edge) || missedEdges.contains(edge)) {
                    continue;
                }
                missedEdges.__insert(edge);
                scopeList.add(edge.target);
            }
        }
    }

    private void assertClosed(S scope, EdgeOrData<L> label) {
        if(!statusOps.closed(scope, label)) {
            throw new IllegalStateException(
                "Activating (" + scope + ", " + label + ") while it has not been closed yet");
        }
    }

    public static <S, L, D> ScopeGraphDiff<S, L, D> fullDiff(S s0current, S s0previous,
        IScopeGraph.Immutable<S, L, D> current, IScopeGraph.Immutable<S, L, D> previous,
        ScopeGraphDifferOps<S, D> diffOps) {
        final ScopeGraphStatusOps<S, L> statusOps =
            new FinalizedStatusOps<>(Set.Immutable.union(current.getLabels(), previous.getLabels()));
        final ScopeGraphDiffer<S, L, D> differ = new ScopeGraphDiffer<>(previous, diffOps, statusOps);
        final DifferState.Immutable<S, L, D> initialState = differ.initDiff(s0current, s0previous);

        final Multimap<S, EdgeOrData<L>> initialActivations = ArrayListMultimap.create();
        initialState.edgeDelays().forEach((s, l) -> initialActivations.put(s, EdgeOrData.edge(l)));

        final DifferState.Immutable<S, L, D> state = differ.doDiff(current, initialState, initialActivations);
        return differ.finalize(current, state);
    }

    public static <S, L, D> ScopeGraphDiffer<S, L, D> of(IScopeGraph.Immutable<S, L, D> previous, ScopeGraphDifferOps<S, D> diffOps,
        ScopeGraphStatusOps<S, L> statusOps) {
        return new ScopeGraphDiffer<S, L, D>(previous, diffOps, statusOps);
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

    private static class FinalizedStatusOps<S, L> implements ScopeGraphStatusOps<S, L> {

        private final Set.Immutable<L> labels;

        public FinalizedStatusOps(Set.Immutable<L> labels) {
            this.labels = labels;
        }

        @Override public boolean closed(S scope, EdgeOrData<L> label) {
            return true;
        }

        @Override public Collection<L> allLabels(S scope) {
            return labels;
        }
    }
}