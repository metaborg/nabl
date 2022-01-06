package mb.p_raffrayi.impl.diagnostics;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import org.metaborg.util.functions.Action4;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import mb.p_raffrayi.impl.diff.IDifferOps;
import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.diff.BiMap;

public class AmbigousEdgeMatch<S, L, D> {

    private final IScopeGraph.Immutable<S, L, D> scopeGraph;
    private final Collection<S> rootScopes;
    private final IDifferOps<S, L, D> differOps;

    public AmbigousEdgeMatch(IScopeGraph.Immutable<S, L, D> scopeGraph, Collection<S> rootScopes,
            IDifferOps<S, L, D> differOps) {
        this.scopeGraph = scopeGraph;
        this.rootScopes = rootScopes;
        this.differOps = differOps;
    }

    public Report<S, L, D> analyze() {
        final Multimap<Edge<S, L, D>, Edge<S, L, D>> ambiguousMatches = HashMultimap.create();

        for(S root : rootScopes) {
            analyzeScope(BiMap.Immutable.of(), root, (src, lbl, tgt1, tgt2) -> {
                final D d1 = scopeGraph.getData(tgt1).orElse(null);
                final D d2 = scopeGraph.getData(tgt2).orElse(null);
                ambiguousMatches.put(new Edge<>(src, lbl, tgt1, d1), new Edge<>(src, lbl, tgt2, d2));
            });
        }

        return new Report<>(ambiguousMatches);
    }

    private void analyzeScope(BiMap.Immutable<S> matches, S scope, Action4<S, L, S, S> onAmbiguousEdge) {
        if(matches.containsKey(scope)) {
            // break cycles.
            return;
        }
        final BiMap.Immutable<S> newMatches = matchScopes(matches, scope, scope).orElseThrow(() -> {
            return new IllegalStateException("BUG: appears scope cannot be matched to itself.");
        });

        for(L label : scopeGraph.getLabels()) {
            analyzeEdge(newMatches, scope, label, onAmbiguousEdge);
        }

        scopeGraph.getData(scope).ifPresent(d -> {
            for(S innerScope: differOps.getScopes(d)) {
                analyzeScope(matches.put(scope, scope), innerScope, onAmbiguousEdge);
            }
        });

    }

    private void analyzeEdge(BiMap.Immutable<S> matches, S scope, L label, Action4<S, L, S, S> onAmbiguousEdge) {
        final List<S> targets = Lists.newArrayList(scopeGraph.getEdges(scope, label));
        while(!targets.isEmpty()) {
            final S target = targets.remove(0);
            for(S other : targets) {
                matchScopes(matches, target, other).ifPresent(__ -> {
                    onAmbiguousEdge.apply(scope, label, target, other);
                });
            }
            analyzeScope(matches, target, onAmbiguousEdge);
        }
    }

    private Optional<BiMap.Immutable<S>> matchScopes(BiMap.Immutable<S> matches, S scope1, S scope2) {
        // Check if consistent with current matches
        if(matches.containsEntry(scope1, scope2)) {
            return Optional.of(matches);
        }
        if(!matches.canPut(scope1, scope2)) {
            return Optional.empty();
        }

        // Check if match obeys ownership rules
        if(!differOps.isMatchAllowed(scope1, scope2)) {
            return Optional.empty();
        }
        if(!differOps.ownScope(scope1)) {
            return scope1.equals(scope2) ? Optional.of(matches.put(scope1, scope2)) : Optional.empty();
        }

        // Check if scope data can be matched.
        final Optional<D> d1 = scopeGraph.getData(scope1);
        final Optional<D> d2 = scopeGraph.getData(scope2);

        if(d1.isPresent() && d2.isPresent()) {
            return canMatchData(matches.put(scope1, scope2), d1.get(), d2.get());
        } else if(!d1.isPresent() && !d2.isPresent()) {
            return Optional.of(matches.put(scope1, scope2));
        }
        return Optional.empty();
    }

    private Optional<BiMap.Immutable<S>> canMatchData(BiMap.Immutable<S> matches, D d1, D d2) {
        return differOps.matchDatums(d1, d2).flatMap(newMatches -> {
            BiMap.Immutable<S> allMatches = matches;

            for(Map.Entry<S, S> match : newMatches.entrySet()) {
                if(!allMatches.canPut(match.getKey(), match.getValue())) {
                    return Optional.empty();
                }
                final Optional<BiMap.Immutable<S>> transMatches =
                        matchScopes(allMatches, match.getKey(), match.getValue());
                if(!transMatches.isPresent()) {
                    return Optional.empty();
                }
                allMatches = transMatches.get();
            }
            return Optional.of(allMatches);
        });
    }

    public static class Edge<S, L, D> {

        private final S source;
        private final L label;
        private final S target;
        private final @Nullable D targetData;

        public Edge(S source, L label, S target, @Nullable D targetData) {
            this.source = source;
            this.label = label;
            this.target = target;
            this.targetData = targetData;
        }

        public S getSource() {
            return source;
        }

        public L getLabel() {
            return label;
        }

        public S getTarget() {
            return target;
        }

        public D getTargetData() {
            return targetData;
        }

        @Override public String toString() {
            return source + " -" + label + "-> " + target + (targetData != null ? " : " + targetData : "");
        }

        @Override public boolean equals(Object obj) {
            if(this == obj) {
                return true;
            }
            if(obj == null) {
                return false;
            }
            if(obj.getClass() != this.getClass()) {
                return false;
            }

            @SuppressWarnings("unchecked") final Edge<S, L, D> other = (Edge<S, L, D>) obj;
            return Objects.equals(source, other.source) && Objects.equals(label, other.label)
                    && Objects.equals(target, other.target) && Objects.equals(targetData, other.targetData);

        }

        @Override public int hashCode() {
            int hash = 7;
            hash = hash + 11 * source.hashCode();
            hash = hash + 13 * target.hashCode();
            hash = hash + 17 * label.hashCode();
            if(targetData != null) {
                hash = hash + 19 * targetData.hashCode();
            }
            return hash;
        }

    }

    public static class Report<S, L, D> {

        private final Multimap<Edge<S, L, D>, Edge<S, L, D>> ambiguousMatches;

        public Report(Multimap<Edge<S, L, D>, Edge<S, L, D>> ambiguousMatches) {
            this.ambiguousMatches = ambiguousMatches;
        }

        public Multimap<Edge<S, L, D>, Edge<S, L, D>> getAmbiguousMatches() {
            return ambiguousMatches;
        }

        @Override public String toString() {
            final StringBuilder sb = new StringBuilder("Report{");
            for(Entry<Edge<S, L, D>, Edge<S, L, D>> entry : ambiguousMatches.entries()) {
                sb.append("  ");
                sb.append(entry.getKey());
                sb.append(" ~ ");
                sb.append(entry.getValue());
            }
            sb.append("}");
            return sb.toString();
        }

    }

}
