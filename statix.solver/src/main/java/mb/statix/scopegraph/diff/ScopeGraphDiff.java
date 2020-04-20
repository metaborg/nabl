package mb.statix.scopegraph.diff;

import java.util.Map.Entry;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;

public class ScopeGraphDiff<S, L, D> {

    private final BiMap.Immutable<S> matchedScopes;
    private final Map.Immutable<S, D> addedScopes;
    private final Map.Immutable<S, D> removedScopes;

    private final BiMap.Immutable<Edge<S, L>> matchedEdges;
    private final Set.Immutable<Edge<S, L>> addedEdges;
    private final Set.Immutable<Edge<S, L>> removedEdges;

    public ScopeGraphDiff(BiMap.Immutable<S> matchedScopes, Map.Immutable<S, D> addedScopes,
            Map.Immutable<S, D> removedScopes, BiMap.Immutable<Edge<S, L>> matchedEdges,
            Set.Immutable<Edge<S, L>> addedEdges, Set.Immutable<Edge<S, L>> removedEdges) {
        this.matchedScopes = matchedScopes;
        this.addedScopes = addedScopes;
        this.removedScopes = removedScopes;
        this.matchedEdges = matchedEdges;
        this.addedEdges = addedEdges;
        this.removedEdges = removedEdges;
    }

    public BiMap.Immutable<S> matchedScopes() {
        return matchedScopes;
    }

    public Map.Immutable<S, D> addedScopes() {
        return addedScopes;
    }

    public Map.Immutable<S, D> removedScopes() {
        return removedScopes;
    }

    public BiMap.Immutable<Edge<S, L>> matchedEdges() {
        return matchedEdges;
    }

    public Set.Immutable<Edge<S, L>> addedEdges() {
        return addedEdges;
    }

    public Set.Immutable<Edge<S, L>> removedEdges() {
        return removedEdges;
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ScopeGraphDiff:\n");

        sb.append("  matched scopes:\n");
        for(Map.Entry<S, S> entry : matchedScopes.entrySet()) {
            sb.append("    ").append(entry.getKey()).append(" ~ ").append(entry.getValue()).append("\n");
        }
        sb.append("  added scopes:\n");
        for(Entry<S, D> entry : addedScopes.entrySet()) {
            sb.append("  + ").append(entry.getKey());
            if(entry.getValue() != null) {
                sb.append(" : ").append(entry.getValue());
            }
            sb.append("\n");
        }
        sb.append("  removed scopes:\n");
        for(Entry<S, D> entry : removedScopes.entrySet()) {
            sb.append("  - ").append(entry.getKey());
            if(entry.getValue() != null) {
                sb.append(" : ").append(entry.getValue());
            }
            sb.append("\n");
        }

        /*
        sb.append("  matched edges:\n");
        for(Map.Entry<Edge<S, L>, Edge<S, L>> entry : matchedEdges.entrySet()) {
            sb.append("    ").append(entry.getKey()).append(" ~ ").append(entry.getValue()).append("\n");
        }
        */
        sb.append("  added edges:\n");
        for(Edge<S, L> edge : addedEdges) {
            sb.append("  + ").append(edge).append("\n");
        }
        sb.append("  removed edges:\n");
        for(Edge<S, L> edge : removedEdges) {
            sb.append("  - ").append(edge).append("\n");
        }

        return sb.toString();
    }

}