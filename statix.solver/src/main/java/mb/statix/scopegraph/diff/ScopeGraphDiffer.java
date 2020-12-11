package mb.statix.scopegraph.diff;

import java.util.Deque;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;

import org.metaborg.util.functions.Function1;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Map.Transient;
import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.statix.scopegraph.IScopeGraph;

public class ScopeGraphDiffer<S, L, D> {

    private final S s0;
    private final IScopeGraph.Immutable<S, L, D> current;
    private final IScopeGraph.Immutable<S, L, D> previous;
    private final Set.Immutable<L> labels;
    private final ScopeGraphDifferOps<S, D> diffOps;

    private final BiMap.Transient<S> matchedScopes = BiMap.Transient.of();
    private final BiMap.Transient<Edge<S, L>> matchedEdges = BiMap.Transient.of();

    private final Set.Transient<S> seenCurrentScopes = Set.Transient.of();
    private final Set.Transient<Edge<S, L>> seenCurrentEdges = Set.Transient.of();

    private final Set.Transient<S> seenPreviousScopes = Set.Transient.of();
    private final Set.Transient<Edge<S, L>> seenPreviousEdges = Set.Transient.of();

    private final Queue<EdgeMatch> worklist = new PriorityQueue<>();

    private ScopeGraphDiffer(S s0, IScopeGraph.Immutable<S, L, D> current, IScopeGraph.Immutable<S, L, D> previous,
            ScopeGraphDifferOps<S, D> diffOps) {
        this.s0 = s0;
        this.current = current;
        this.previous = previous;
        this.labels = Set.Immutable.union(current.getLabels(), previous.getLabels());
        this.diffOps = diffOps;
    }

    private ScopeGraphDiff<S, L, D> doDiff() {
        if(!matchScopes(BiMap.Immutable.of(s0, s0))) {
            throw new IllegalStateException();
        }
        while(!worklist.isEmpty()) {
            final EdgeMatch m = worklist.remove();
            matchEdge(m.currentEdge, m.previousEdges);
        }

        final Map.Transient<S, D> addedScopes = Map.Transient.of();
        final Set.Transient<Edge<S, L>> addedEdges = Set.Transient.of();
        finishDiff(current, diffOps::getCurrentScopes, seenCurrentScopes, seenCurrentEdges, matchedScopes.keySet(),
                matchedEdges.keySet(), addedScopes, addedEdges);

        final Map.Transient<S, D> removedScopes = Map.Transient.of();
        final Set.Transient<Edge<S, L>> removedEdges = Set.Transient.of();
        finishDiff(previous, diffOps::getPreviousScopes, seenPreviousScopes, seenPreviousEdges,
                matchedScopes.valueSet(), matchedEdges.valueSet(), removedScopes, removedEdges);

        final ScopeGraphDiff<S, L, D> diff = new ScopeGraphDiff<>(matchedScopes.freeze(), matchedEdges.freeze(),
                addedScopes.freeze(), addedEdges.freeze(), removedScopes.freeze(), removedEdges.freeze());
        return diff;
    }

    private static <S, L, D> void finishDiff(IScopeGraph<S, L, D> scopeGraph, Function1<D, java.util.Set<S>> getScopes,
            Iterable<S> seenScopes, Iterable<Edge<S, L>> seenEdges, java.util.Set<S> matchedScopes,
            java.util.Set<Edge<S, L>> matchedEdges, Transient<S, D> missedScopes,
            Set.Transient<Edge<S, L>> missedEdges) {

        final Deque<S> scopeList = Lists.newLinkedList(seenScopes);
        final Deque<Edge<S, L>> edgeList = Lists.newLinkedList(seenEdges);

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

    /**
     * Match the scopes and schedule resulting edge matches.
     */
    private boolean matchScopes(BiMap.Immutable<S> scopes) {
        seenCurrentScopes.__insertAll(scopes.keySet());
        seenPreviousScopes.__insertAll(scopes.valueSet());
        final BiMap<S> newMatches;
        if((newMatches = canScopesMatch(scopes).orElse(null)) == null) {
            return false;
        }
        matchedScopes.putAll(newMatches);
        for(Map.Entry<S, S> entry : newMatches.entrySet()) {
            final S currentScope = entry.getKey();
            final S previousScope = entry.getValue();

            final Optional<D> currentData = current.getData(currentScope);
            currentData.ifPresent(d -> seenCurrentScopes.__insertAll(diffOps.getCurrentScopes(d)));
            final Optional<D> previousData = previous.getData(previousScope);
            previousData.ifPresent(d -> seenPreviousScopes.__insertAll(diffOps.getPreviousScopes(d)));
            for(L label : labels) {
                scheduleEdgeMatches(currentScope, previousScope, label);
            }
        }
        return true;
    }

    /**
     * Check if scopes can match under the current match, and return the new matches it would introduce.
     */
    private Optional<BiMap.Immutable<S>> canScopesMatch(BiMap.Immutable<S> scopes) {
        final BiMap.Transient<S> newMatches = BiMap.Transient.of();
        for(Map.Entry<S, S> entry : scopes.entrySet()) {
            final S currentScope = entry.getKey();
            final S previousScope = entry.getValue();
            if(!diffOps.isMatchAllowed(currentScope, previousScope)) {
                return Optional.empty();
            } else if(!matchedScopes.canPut(currentScope, previousScope)) {
                return Optional.empty();
            } else if(matchedScopes.containsEntry(currentScope, previousScope)) {
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
    private void scheduleEdgeMatches(S currentSource, S previousSource, L label) {

        final Set.Immutable<Edge<S, L>> currentEdges = Streams.stream(current.getEdges(currentSource, label))
                .map(currentTarget -> new Edge<>(currentSource, label, currentTarget))
                .collect(CapsuleCollectors.toSet());
        this.seenCurrentEdges.__insertAll(currentEdges);
        final Set.Immutable<Edge<S, L>> previousEdges = Streams.stream(previous.getEdges(previousSource, label))
                .map(previousTarget -> new Edge<>(previousSource, label, previousTarget))
                .collect(CapsuleCollectors.toSet());
        this.seenPreviousEdges.__insertAll(previousEdges);

        for(Edge<S, L> currentEdge : currentEdges) {
            final Map.Transient<Edge<S, L>, BiMap.Immutable<S>> matchingPreviousEdges = Map.Transient.of();
            for(Edge<S, L> previousEdge : previousEdges) {
                final BiMap.Immutable<S> req;
                if((req = matchScopes(currentEdge.target, previousEdge.target).orElse(null)) != null) {
                    if(!canScopesMatch(req).isPresent()) {
                        // already discard options that are not possible under current matches
                        continue;
                    }
                    matchingPreviousEdges.__put(previousEdge, req);
                }
            }
            final EdgeMatch match = new EdgeMatch(currentEdge, matchingPreviousEdges.freeze());
            worklist.add(match);
        }
    }

    private void matchEdge(Edge<S, L> currentEdge, Map.Immutable<Edge<S, L>, BiMap.Immutable<S>> previousEdges) {
        for(Entry<Edge<S, L>, BiMap.Immutable<S>> previousEdge : previousEdges.entrySet()) {
            if(matchScopes(previousEdge.getValue())) {
                matchedEdges.put(currentEdge, previousEdge.getKey());
                return;
            }
        }
    }

    /**
     * Compute the patch required to match two scopes and their data.
     */
    private Optional<BiMap.Immutable<S>> matchScopes(S currentScope, S previousScope) {
        final BiMap.Transient<S> _matches = BiMap.Transient.of();
        if(!scopeMatch(currentScope, previousScope, _matches)) {
            return Optional.empty();
        }
        final BiMap.Immutable<S> matches = _matches.freeze();
        return Optional.of(matches);
    }

    private boolean scopeMatch(S currentScope, S previousScope, BiMap.Transient<S> req) {
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
                    (leftScope, rightScope) -> scopeMatch(leftScope, rightScope, req))) {
                // data cannot be matched
                return false;
            }
        }
        return true;
    }

    public static <S, L, D> ScopeGraphDiff<S, L, D> diff(S s0, IScopeGraph.Immutable<S, L, D> current,
            IScopeGraph.Immutable<S, L, D> previous, ScopeGraphDifferOps<S, D> diffOps) {
        return new ScopeGraphDiffer<>(s0, current, previous, diffOps).doDiff();
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

}