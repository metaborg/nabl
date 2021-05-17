package mb.scopegraph.oopsla20.diff;

import java.util.Map.Entry;

import org.metaborg.util.collection.CapsuleUtil;

import java.util.Optional;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;

public class ScopeGraphDiff<S, L, D> {

    private final BiMap.Immutable<S> matchedScopes;
    private final BiMap.Immutable<Edge<S, L>> matchedEdges;
    private final Changes<S, L, D> added;
    private final Changes<S, L, D> removed;

    public ScopeGraphDiff(BiMap.Immutable<S> matchedScopes, BiMap.Immutable<Edge<S, L>> matchedEdges,
        Map.Immutable<S, Optional<D>> addedScopes, Set.Immutable<Edge<S, L>> addedEdges,
        Map.Immutable<S, Optional<D>> removedScopes, Set.Immutable<Edge<S, L>> removedEdges) {
        this.matchedScopes = matchedScopes;
        this.matchedEdges = matchedEdges;
        this.added = new Changes<>(addedScopes, addedEdges);
        this.removed = new Changes<>(removedScopes, removedEdges);
    }

    public BiMap.Immutable<S> matchedScopes() {
        return matchedScopes;
    }

    public BiMap.Immutable<Edge<S, L>> matchedEdges() {
        return matchedEdges;
    }

    public Changes<S, L, D> added() {
        return added;
    }

    public Changes<S, L, D> removed() {
        return removed;
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ScopeGraphDiff:\n");

        sb.append("  matched scopes:\n");
        for(Map.Entry<S, S> entry : matchedScopes.entrySet()) {
            sb.append("    ").append(entry.getKey()).append(" ~ ").append(entry.getValue()).append("\n");
        }
        /*
         * sb.append("  matched edges:\n"); for(Map.Entry<Edge<S, L>, Edge<S, L>> entry : matchedEdges.entrySet()) {
         * sb.append("    ").append(entry.getKey()).append(" ~ ").append(entry.getValue()).append("\n"); }
         */

        sb.append("  added:\n");
        sb.append(added);

        sb.append("  removed:\n");
        sb.append(removed);

        return sb.toString();
    }

    public static class Changes<S, L, D> {

        private final Map.Immutable<S, Optional<D>> scopes;
        private final Set.Immutable<Edge<S, L>> edges;

        public Changes(Map.Immutable<S, Optional<D>> scopes, Set.Immutable<Edge<S, L>> edges) {
            this.scopes = scopes;
            this.edges = edges;
        }

        public Map.Immutable<S, Optional<D>> scopes() {
            return scopes;
        }

        public Set.Immutable<Edge<S, L>> edges() {
            return edges;
        }

        @Override public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("    scopes:\n");
            for(Entry<S, Optional<D>> entry : scopes.entrySet()) {
                sb.append("    + ").append(entry.getKey());
                if(entry.getValue().isPresent()) {
                    sb.append(" : ").append(entry.getValue());
                }
                sb.append("\n");
            }

            sb.append("    edges:\n");
            for(Edge<S, L> edge : edges) {
                sb.append("    + ").append(edge).append("\n");
            }

            return sb.toString();
        }

    }

    public static <S, L, D> ScopeGraphDiff<S, L, D> empty() {
        return new ScopeGraphDiff<>(BiMap.Immutable.of(), BiMap.Immutable.of(), CapsuleUtil.immutableMap(),
                CapsuleUtil.immutableSet(), CapsuleUtil.immutableMap(), CapsuleUtil.immutableSet());
    }

}