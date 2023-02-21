package mb.scopegraph.oopsla20.diff;

import org.metaborg.util.collection.BiMap;
import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.MultiSetMap;
import org.metaborg.util.tuple.Tuple2;

import io.usethesource.capsule.Set;

public abstract class DifferState<S, L, D> {

    public static class Transient<S, L, D> extends DifferState<S, L, D> {

        private final BiMap.Transient<S> matchedScopes;
        private final BiMap.Transient<Edge<S, L>> matchedEdges;

        private final Set.Transient<S> seenCurrentScopes;
        private final Set.Transient<Edge<S, L>> seenCurrentEdges;

        private final Set.Transient<S> seenPreviousScopes;
        private final Set.Transient<Edge<S, L>> seenPreviousEdges;

        private final MultiSetMap.Transient<S, L> edgeDelays;

        /**
         * Matches that need to wait for their target scope (the key) to be activated.
         */
        private final BiMultimap.Transient<S, Tuple2<S, L>> dataDelays;

        private Transient(BiMap.Transient<S> matchedScopes, BiMap.Transient<Edge<S, L>> matchedEdges,
            Set.Transient<S> seenCurrentScopes, Set.Transient<Edge<S, L>> seenCurrentEdges,
            Set.Transient<S> seenPreviousScopes, Set.Transient<Edge<S, L>> seenPreviousEdges, MultiSetMap.Transient<S, L> edgeDelays,
            BiMultimap.Transient<S, Tuple2<S, L>> dataDelays) {
            this.matchedScopes = matchedScopes;
            this.matchedEdges = matchedEdges;
            this.seenCurrentScopes = seenCurrentScopes;
            this.seenCurrentEdges = seenCurrentEdges;
            this.seenPreviousScopes = seenPreviousScopes;
            this.seenPreviousEdges = seenPreviousEdges;
            this.edgeDelays = edgeDelays;
            this.dataDelays = dataDelays;
        }

        public BiMap.Transient<S> matchedScopes() {
            return matchedScopes;
        }

        public void putAllMatchedScopes(BiMap<S> newMatches) {
            matchedScopes.putAll(newMatches);
        }

        public boolean canPutMatchedScope(S currentScope, S previousScope) {
            return matchedScopes.canPut(currentScope, previousScope);
        }

        public boolean containsMatchedScopes(S currentScope, S previousScope) {
            return matchedScopes.containsEntry(currentScope, previousScope);
        }

        public BiMap.Transient<Edge<S, L>> matchedEdges() {
            return matchedEdges;
        }

        public void putMatchedEdge(Edge<S, L> currentEdge, Edge<S, L> previousEdge) {
            matchedEdges.put(currentEdge, previousEdge);
        }

        public Set.Transient<S> seenCurrentScopes() {
            return seenCurrentScopes;
        }

        public Set.Transient<Edge<S, L>> seenCurrentEdges() {
            return seenCurrentEdges;
        }

        public Set.Transient<S> seenPreviousScopes() {
            return seenPreviousScopes;
        }

        public Set.Transient<Edge<S, L>> seenPreviousEdges() {
            return seenPreviousEdges;
        }

        public MultiSetMap.Transient<S, L> edgeDelays() {
            return edgeDelays;
        }

        /**
         * Matching of key is dependent on closing of values
         *
         * @return
         */
        public BiMultimap.Transient<S, Tuple2<S, L>> dataDelays() {
            return dataDelays;
        }

        public Immutable<S, L, D> freeze() {
            return new Immutable<>(matchedScopes.freeze(), matchedEdges.freeze(), seenCurrentScopes.freeze(),
                seenCurrentEdges.freeze(), seenPreviousScopes.freeze(), seenPreviousEdges.freeze(),
                edgeDelays.freeze(), dataDelays.freeze());
        }

        public static <S, L, D> Transient<S, L, D> of() {
            return new Transient<>(BiMap.Transient.of(), BiMap.Transient.of(), CapsuleUtil.transientSet(),
                CapsuleUtil.transientSet(), CapsuleUtil.transientSet(), CapsuleUtil.transientSet(),
                MultiSetMap.Transient.of(), BiMultimap.Transient.of());
        }
    }

    public static class Immutable<S, L, D> extends DifferState<S, L, D> {

        private final BiMap.Immutable<S> matchedScopes;
        private final BiMap.Immutable<Edge<S, L>> matchedEdges;

        private final Set.Immutable<S> seenCurrentScopes;
        private final Set.Immutable<Edge<S, L>> seenCurrentEdges;

        private final Set.Immutable<S> seenPreviousScopes;
        private final Set.Immutable<Edge<S, L>> seenPreviousEdges;

        private final MultiSetMap.Immutable<S, L> edgeDelays;
        private final BiMultimap.Immutable<S, Tuple2<S, L>> dataDelays;

        private Immutable(BiMap.Immutable<S> matchedScopes, BiMap.Immutable<Edge<S, L>> matchedEdges,
            Set.Immutable<S> seenCurrentScopes, Set.Immutable<Edge<S, L>> seenCurrentEdges,
            Set.Immutable<S> seenPreviousScopes, Set.Immutable<Edge<S, L>> seenPreviousEdges,
            MultiSetMap.Immutable<S, L> edgeDelays, BiMultimap.Immutable<S, Tuple2<S, L>> dataDelays) {
            this.matchedScopes = matchedScopes;
            this.matchedEdges = matchedEdges;
            this.seenCurrentScopes = seenCurrentScopes;
            this.seenCurrentEdges = seenCurrentEdges;
            this.seenPreviousScopes = seenPreviousScopes;
            this.seenPreviousEdges = seenPreviousEdges;
            this.edgeDelays = edgeDelays;
            this.dataDelays = dataDelays;
        }

        public BiMap.Immutable<S> matchedScopes() {
            return matchedScopes;
        }

        public boolean containsMatchedScopes(S currentScope, S previousScope) {
            return matchedScopes.containsEntry(currentScope, previousScope);
        }

        public BiMap.Immutable<Edge<S, L>> matchedEdges() {
            return matchedEdges;
        }

        public Set.Immutable<S> seenCurrentScopes() {
            return seenCurrentScopes;
        }

        public Set.Immutable<Edge<S, L>> seenCurrentEdges() {
            return seenCurrentEdges;
        }

        public Set.Immutable<S> seenPreviousScopes() {
            return seenPreviousScopes;
        }

        public Set.Immutable<Edge<S, L>> seenPreviousEdges() {
            return seenPreviousEdges;
        }

        public MultiSetMap.Immutable<S, L> edgeDelays() {
            return edgeDelays;
        }

        public BiMultimap.Immutable<S, Tuple2<S, L>> dataDelays() {
            return dataDelays;
        }

        public Transient<S, L, D> melt() {
            return new Transient<>(matchedScopes.melt(), matchedEdges.melt(), seenCurrentScopes.asTransient(),
                seenCurrentEdges.asTransient(), seenPreviousScopes.asTransient(), seenPreviousEdges.asTransient(),
                edgeDelays.melt(), dataDelays.melt());
        }

        public static <S, L, D> Immutable<S, L, D> of() {
            return new Immutable<>(BiMap.Immutable.of(), BiMap.Immutable.of(), CapsuleUtil.immutableSet(),
                CapsuleUtil.immutableSet(), CapsuleUtil.immutableSet(), CapsuleUtil.immutableSet(),
                MultiSetMap.Immutable.of(), BiMultimap.Immutable.of());
        }

    }

}