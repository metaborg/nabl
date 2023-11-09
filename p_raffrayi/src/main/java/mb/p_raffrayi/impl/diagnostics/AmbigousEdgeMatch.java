package mb.p_raffrayi.impl.diagnostics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.annotation.Nullable;

import org.metaborg.util.functions.Action4;

import mb.p_raffrayi.collection.HashSetMultiTable;
import mb.p_raffrayi.collection.MultiTable;
import mb.p_raffrayi.impl.diff.IDifferOps;
import mb.scopegraph.oopsla20.IScopeGraph;
import org.metaborg.util.collection.BiMap;

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
        final MultiTable<S, L, Match<S, D>> ambiguousMatches = HashSetMultiTable.create();

        for(S root : rootScopes) {
            analyzeScope(BiMap.Immutable.of(), root, (src, lbl, tgt1, tgt2) -> {
                final D d1 = scopeGraph.getData(tgt1).filter(d -> !differOps.embed(tgt1).equals(d)).orElse(null);
                final D d2 = scopeGraph.getData(tgt2).filter(d -> !differOps.embed(tgt2).equals(d)).orElse(null);
                ambiguousMatches.put(src, lbl, new Match<>(tgt1, d1, tgt2, d2));
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
            for(S innerScope : differOps.getScopes(d)) {
                analyzeScope(matches.put(scope, scope), innerScope, onAmbiguousEdge);
            }
        });

    }

    private void analyzeEdge(BiMap.Immutable<S> matches, S scope, L label, Action4<S, L, S, S> onAmbiguousEdge) {
        final List<S> targets = new ArrayList<>(scopeGraph.getEdges(scope, label));
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

    public static class Match<S, D> {

        private final S scope1;
        private final S scope2;

        private final @Nullable D datum1;
        private final @Nullable D datum2;

        public Match(S scope1, D datum1, S scope2, D datum2) {
            this.scope1 = scope1;
            this.datum1 = datum1;
            this.scope2 = scope2;
            this.datum2 = datum2;
        }

        public S getScope1() {
            return scope1;
        }

        public S getScope2() {
            return scope2;
        }

        public Optional<D> getDatum1() {
            return Optional.ofNullable(datum1);
        }

        public Optional<D> getDatum2() {
            return Optional.ofNullable(datum2);
        }

        @Override public String toString() {
            return scope1 + (datum1 != null ? " : " + datum1 : "") + " ~ " + scope2
                    + (datum2 != null ? " : " + datum2 : "");
        }

        @Override public boolean equals(Object obj) {
            if(obj == this) {
                return true;
            }
            if(obj == null) {
                return false;
            }
            if(!getClass().equals(obj.getClass())) {
                return false;
            }
            @SuppressWarnings("unchecked") final Match<S, D> other = (Match<S, D>) obj;
            return Objects.equals(scope1, other.scope1) && Objects.equals(datum1, other.datum1)
                    && Objects.equals(scope2, other.scope2) && Objects.equals(datum2, other.datum2);
        }

        @Override public int hashCode() {
            return Objects.hash(scope1, datum1, scope2, datum2);
        }

    }

    public static class Report<S, L, D> {

        private MultiTable<S, L, Match<S, D>> ambiguousMatches;

        public Report(MultiTable<S, L, Match<S, D>> ambiguousMatches) {
            this.ambiguousMatches = ambiguousMatches;
        }

        public Set<S> scopes() {
            return ambiguousMatches.rowKeySet();
        }

        public Set<L> labels(S scope) {
            return ambiguousMatches.row(scope).keySet();
        }

        public Collection<Match<S, D>> matches(S scope, L label) {
            return ambiguousMatches.get(scope, label);
        }

        public int size() {
            return ambiguousMatches.cellSet().size();
        }

        public boolean isEmpty() {
            return ambiguousMatches.cellSet().isEmpty();
        }

        public boolean contains(S source, L label, Match<S, D> match) {
            return ambiguousMatches.get(source, label).contains(match);
        }

        @Override public String toString() {
            final StringBuilder sb = new StringBuilder("Report {\n");
            for(S scope : ambiguousMatches.rowKeySet()) {
                sb.append("  ");
                sb.append(scope);
                sb.append(" {\n");
                for(Map.Entry<L, Collection<Match<S, D>>> entry : ambiguousMatches.row(scope).entrySet()) {
                    sb.append("    ");
                    sb.append(entry.getKey());
                    sb.append(" : \n");
                    for(Match<S, D> match : entry.getValue()) {
                        sb.append("    - ");
                        sb.append(match);
                        sb.append("\n");
                    }
                }
                sb.append("  }\n");
            }
            sb.append("}\n");
            return sb.toString();
        }

    }

}
