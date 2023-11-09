package mb.scopegraph.pepm16;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import mb.scopegraph.pepm16.esop15.IEsopScopeGraph;
import mb.scopegraph.pepm16.esop15.reference.EsopScopeGraph;

public final class ScopeGraphCommon<S extends IScope, L extends ILabel, O extends IOccurrence, V> {

    private final IEsopScopeGraph<S, L, O, V> scopeGraph;

    public ScopeGraphCommon(IEsopScopeGraph<S, L, O, V> scopeGraph) {
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
        @SuppressWarnings("unchecked") final Set<S> reaches = new HashSet<>(Arrays.asList(scope));
        final Deque<S> worklist = new ArrayDeque<>(Arrays.asList(scope));
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
        @SuppressWarnings("unchecked") final Set<S> reachable = new HashSet<>(Arrays.asList(scope));
        final Deque<S> worklist = new ArrayDeque<>(Arrays.asList(scope));
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

    /**
     * Summarize a scope graph to all reachable scopes and edges
     */
    public IEsopScopeGraph<S, L, O, V> summarize(final S scope) {
        final IEsopScopeGraph.Transient<S, L, O, V> summaryGraph = EsopScopeGraph.Transient.of();
        summarize(scope, summaryGraph, new HashSet<>());
        return summaryGraph.freeze();
    }

    private void summarize(final S scope, IEsopScopeGraph.Transient<S, L, O, V> summaryGraph, Set<Object> visited) {
        if(!visited.contains(scope)) {
            visited.add(scope);
            for(O decl : scopeGraph.getDecls().inverse().get(scope)) {
                summaryGraph.addDecl(scope, decl);
                summarize(decl, summaryGraph, visited);
            }
            for(Entry<L, S> next : scopeGraph.getDirectEdges().get(scope)) {
                summaryGraph.addDirectEdge(scope, next.getKey(), next.getValue());
                summarize(next.getValue(), summaryGraph, visited);
            }
        }
    }

    public IEsopScopeGraph<S, L, O, V> summarize(final O decl) {
        final IEsopScopeGraph.Transient<S, L, O, V> summaryGraph = EsopScopeGraph.Transient.of();
        summarize(decl, summaryGraph, new HashSet<>());
        return summaryGraph.freeze();
    }

    private void summarize(final O decl, IEsopScopeGraph.Transient<S, L, O, V> summaryGraph, Set<Object> visited) {
        if(!visited.contains(decl)) {
            visited.add(decl);
            for(Entry<L, S> next : scopeGraph.getExportEdges().get(decl)) {
                summaryGraph.addExportEdge(decl, next.getKey(), next.getValue());
                summarize(next.getValue(), summaryGraph, visited);
            }
        }
    }

}