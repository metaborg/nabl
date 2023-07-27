package mb.scopegraph.patching;

import java.util.Map;

import org.metaborg.util.functions.Function2;

import mb.scopegraph.oopsla20.IScopeGraph;
import org.metaborg.util.collection.BiMap;
import mb.scopegraph.oopsla20.reference.ScopeGraph;

public class Patcher<S, L, D> {

    private Patcher(IPatchCollection.Immutable<S> sourcePatches, IPatchCollection.Immutable<S> targetPatches,
            IPatchCollection.Immutable<S> datumScopePatches, IPatchCollection.Immutable<S> datumPatches,
            Function2<D, BiMap.Immutable<S>, D> patchDatum) {
        this.sourcePatches = sourcePatches;
        this.targetPatches = targetPatches;
        this.datumScopePatches = datumScopePatches;
        this.datumPatches = datumPatches;
        this.patchDatum = patchDatum;
    }

    private final IPatchCollection.Immutable<S> sourcePatches;

    private final IPatchCollection.Immutable<S> targetPatches;

    private final IPatchCollection.Immutable<S> datumScopePatches;

    private final IPatchCollection.Immutable<S> datumPatches;

    private final Function2<D, BiMap.Immutable<S>, D> patchDatum;

    public <T> IScopeGraph.Immutable<S, L, D> apply(IScopeGraph.Immutable<S, L, D> scopeGraph,
            EdgeSourcePatchCallback<S, L, D, T> edgeSourceCallback,
            EdgeTargetPatchCallback<S, L, D, T> edgeTargetCallback, DataPatchCallback<S, L, D> dataPatchCallback) {
        if(sourcePatches.isIdentity() && targetPatches.isIdentity() && datumScopePatches.isIdentity()) {
            // Optimization, datums are replaced in-place, so we prevent copying all edges.
            // We need datumScopePatches to be identity as well, to prevent stale data mapping remaining.

            // 1. Invoke callbacks for copied edges
            for(Map.Entry<? extends Map.Entry<S, L>, ? extends Iterable<S>> edges : scopeGraph.getEdges().entrySet()) {
                final S source = edges.getKey().getKey();
                final L label = edges.getKey().getValue();
                final T sourceMeta = edgeSourceCallback.sourcePatched(source, source);
                for(S target : edges.getValue()) {
                    edgeTargetCallback.targetPatched(source, source, label, target, target, sourceMeta);
                }
            }
            if(datumPatches.isIdentity()) {
                // Only Identity patches, apply callbacks and return original input.
                if(dataPatchCallback != DataPatchCallback.noop()) {
                    scopeGraph.getData().forEach((s, d) -> dataPatchCallback.dataPatched(s, s, d, d));
                }

                return scopeGraph;
            }

            // 2. Apply datum patches
            final IScopeGraph.Transient<S, L, D> patchedGraph = scopeGraph.melt();
            applyDatumPatches(patchedGraph, scopeGraph.getData(), dataPatchCallback);
            return patchedGraph.freeze();
        }

        // Some of the non-data scopes is not identity, so create new graph from scratch.
        final ScopeGraph.Transient<S, L, D> patchedGraph = ScopeGraph.Transient.of();

        for(Map.Entry<? extends Map.Entry<S, L>, ? extends Iterable<S>> edges : scopeGraph.getEdges().entrySet()) {
            final S oldSource = edges.getKey().getKey();
            final S newSource = sourcePatches.patch(oldSource);
            final L label = edges.getKey().getValue();
            final T sourceMeta = edgeSourceCallback.sourcePatched(oldSource, newSource);
            for(S oldTarget : edges.getValue()) {
                final S newTarget = targetPatches.patch(oldTarget);
                patchedGraph.addEdge(newSource, label, newTarget);
                edgeTargetCallback.targetPatched(oldSource, newSource, label, oldTarget, newTarget, sourceMeta);
            }
        }
        applyDatumPatches(patchedGraph, scopeGraph.getData(), dataPatchCallback);

        return patchedGraph.freeze();
    }

    private void applyDatumPatches(IScopeGraph.Transient<S, L, D> scopeGraph, Map<S, D> data,
            DataPatchCallback<S, L, D> dataPatchCallback) {
        for(Map.Entry<S, D> datumEntry : scopeGraph.getData().entrySet()) {
            final S oldScope = datumEntry.getKey();
            final D oldDatum = datumEntry.getValue();
            final S newScope = datumScopePatches.patch(oldScope);
            final D newDatum = patchDatum.apply(oldDatum, datumPatches.patches());

            scopeGraph.setDatum(newScope, newDatum);
            dataPatchCallback.dataPatched(oldScope, newScope, oldDatum, newDatum);
        }
    }

    public interface EdgeSourcePatchCallback<S, L, D, T> {

        public T sourcePatched(S oldSource, S newSource);

    }

    public interface EdgeTargetPatchCallback<S, L, D, T> {

        public void targetPatched(S oldSource, S newSource, L label, S oldTarget, S newTarget, T sourceMeta);

    }

    public interface DataPatchCallback<S, L, D> {

        @SuppressWarnings("rawtypes") static DataPatchCallback NOOP = new DataPatchCallback() {

            @Override public void dataPatched(Object oldSource, Object newSource, Object oldDatum, Object newDatum) {

            }

        };

        @SuppressWarnings("unchecked") public static <S, L, D> DataPatchCallback<S, L, D> noop() {
            return NOOP;
        }

        public void dataPatched(S oldSource, S newSource, D oldDatum, D newDatum);

    }

    public static class Builder<S, L, D> {

        private final IPatchCollection.Transient<S> sourcePatches = PatchCollection.Transient.of();

        private final IPatchCollection.Transient<S> targetPatches = PatchCollection.Transient.of();

        private final IPatchCollection.Transient<S> datumScopePatches = PatchCollection.Transient.of();

        private final IPatchCollection.Transient<S> datumPatches = PatchCollection.Transient.of();

        private Function2<D, BiMap.Immutable<S>, D> patchDatum;

        public Builder<S, L, D> patchSources(IPatchCollection<S> patches) throws InvalidPatchCompositionException {
            sourcePatches.putAll(patches);
            return this;
        }

        public Builder<S, L, D> patchEdgeTargets(IPatchCollection<S> patches) throws InvalidPatchCompositionException {
            targetPatches.putAll(patches);
            return this;
        }

        public Builder<S, L, D> patchDatumSources(IPatchCollection<S> patches) throws InvalidPatchCompositionException {
            datumScopePatches.putAll(patches);
            return this;
        }

        public Builder<S, L, D> patchDatums(IPatchCollection<S> patches, Function2<D, BiMap.Immutable<S>, D> patchDatum)
                throws InvalidPatchCompositionException {
            if(this.patchDatum != null) {
                throw new InvalidPatchCompositionException("Cannot add multiple patch functions for data.");
            }
            datumPatches.putAll(patches);
            this.patchDatum = patchDatum;
            return this;
        }

        public Patcher<S, L, D> build() {
            return new Patcher<S, L, D>(sourcePatches.freeze(), targetPatches.freeze(), datumScopePatches.freeze(),
                    datumPatches.freeze(), patchDatum);
        }
    }

}
