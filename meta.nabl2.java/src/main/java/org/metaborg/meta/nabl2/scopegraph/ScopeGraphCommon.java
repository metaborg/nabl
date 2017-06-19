package org.metaborg.meta.nabl2.scopegraph;

import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.Queues;
import com.google.common.collect.Sets;

public final class ScopeGraphCommon<S extends IScope, L extends ILabel, O extends IOccurrence> {

    private final IScopeGraph<S, L, O> scopeGraph;

    public ScopeGraphCommon(IScopeGraph<S, L, O> scopeGraph) {
        this.scopeGraph = scopeGraph;
    }

    /**
     * Returns the set of references that directly reach the given scope.
     */
    public Set<O> reachingReferences(final S scope) {
        return reachingScopes(scope).stream().flatMap(s -> scopeGraph.getRefs().inverse().get(s).stream())
                .collect(Collectors.toSet());
    }

    /**
     * Returns the set of scopes that directly reach the given scope.
     */
    public Set<S> reachingScopes(final S scope) {
        @SuppressWarnings("unchecked") final Set<S> reaches = Sets.newHashSet(scope);
        final Deque<S> worklist = Queues.newArrayDeque(Iterables2.singleton(scope));
        while(!worklist.isEmpty()) {
            final S current = worklist.pop();
            scopeGraph.getDirectEdges().inverse().get(current).stream().map(Map.Entry::getValue)
                    .filter(next -> !reaches.contains(next)).forEach(next -> {
                        reaches.add(next);
                        worklist.add(next);
                    });
        }
        return reaches;
    }

    /**
     * Returns the set of declarations that are directly reachable from given scope.
     */
    public Set<O> reachableDecls(final S scope) {
        return reachableScopes(scope).stream().flatMap(s -> scopeGraph.getDecls().inverse().get(s).stream())
                .collect(Collectors.toSet());
    }

    /**
     * Returns the set of scopes that are directly reachable from given scope.
     */
    public Set<S> reachableScopes(final S scope) {
        @SuppressWarnings("unchecked") final Set<S> reachable = Sets.newHashSet(scope);
        final Deque<S> worklist = Queues.newArrayDeque(Iterables2.singleton(scope));
        while(!worklist.isEmpty()) {
            final S current = worklist.pop();
            scopeGraph.getDirectEdges().get(current).stream().map(Map.Entry::getValue)
                    .filter(next -> !reachable.contains(next)).forEach(next -> {
                        reachable.add(next);
                        worklist.add(next);
                    });
        }
        return reachable;
    }

}