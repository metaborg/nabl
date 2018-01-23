package org.metaborg.meta.nabl2.scopegraph.esop.persistent.all_shortest_paths;

import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IResolutionParameters;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.util.functions.Predicate2;

import com.google.common.annotations.Beta;

class AllShortestPathsNameResolutionBuilder<S extends IScope, L extends ILabel, O extends IOccurrence, V>
        implements IEsopNameResolution.Transient<S, L, O> {

    private IEsopNameResolution.Immutable<S, L, O> solution;

    @Deprecated
    private final Predicate2<S, L> isEdgeClosed;

    AllShortestPathsNameResolutionBuilder(final IEsopNameResolution.Immutable<S, L, O> solution,
            final Predicate2<S, L> isEdgeClosed) {
        this.solution = solution;
        this.isEdgeClosed = isEdgeClosed;
    }

    @Beta
    @Override
    public IResolutionParameters<L> getResolutionParameters() {
        return solution.getResolutionParameters();
    }

    @Beta
    @Override
    public IEsopScopeGraph<S, L, O, ?> getScopeGraph() {
        return solution.getScopeGraph();
    }

    @Override
    public boolean isEdgeClosed(S scope, L label) {
        return isEdgeClosed.test(scope, label);
    }

    @Override
    public java.util.Set<O> getResolvedRefs() {
        return solution.getResolvedRefs();
    }

    @Override
    public Optional<io.usethesource.capsule.Set.Immutable<IResolutionPath<S, L, O>>> resolve(O ref) {
        return solution.resolve(ref);
    }

    @Override
    public void resolveAll(Iterable<? extends O> refs) {
        // no-op: all-shortest-paths algorithm does it anyways

        /**
         * Force re-resolution due to mutable updates. Hack necessary due to
         * assumptions in {@link NameResolutionComponent#update()}.
         */
        IEsopNameResolution.Immutable<S, L, O> mergedNameResolution = IEsopNameResolution
                .builder(this.getResolutionParameters(), this.getScopeGraph(), isEdgeClosed).freeze();

        this.solution = mergedNameResolution;
    }

    @Override
    public Optional<io.usethesource.capsule.Set.Immutable<O>> visible(S scope) {
        return solution.visible(scope);
    }

    @Override
    public Optional<io.usethesource.capsule.Set.Immutable<O>> reachable(S scope) {
        return solution.reachable(scope);
    }

    @Override
    public java.util.Set<Entry<O, io.usethesource.capsule.Set.Immutable<IResolutionPath<S, L, O>>>> resolutionEntries() {
        return solution.resolutionEntries();
    }

    @Override
    public boolean addAll(IEsopNameResolution<S, L, O> that) {
        // throw new UnsupportedOperationException("Not yet implemented.");

        IEsopScopeGraph<S, L, O, V> graph1 = (IEsopScopeGraph<S, L, O, V>) this.getScopeGraph();
        IEsopScopeGraph<S, L, O, V> graph2 = (IEsopScopeGraph<S, L, O, V>) that.getScopeGraph();

        java.util.Set<Entry<O, io.usethesource.capsule.Set.Immutable<IResolutionPath<S, L, O>>>> res1 = this
                .resolutionEntries();
        java.util.Set<Entry<O, io.usethesource.capsule.Set.Immutable<IResolutionPath<S, L, O>>>> res2 = that
                .resolutionEntries();
        boolean isModified = !res1.equals(res2);

        IEsopScopeGraph.Transient<S, L, O, V> builder = IEsopScopeGraph.builder();
        builder.addAll(graph1);
        builder.addAll(graph2);
        IEsopScopeGraph.Immutable<S, L, O, ?> mergedGraphs = builder.freeze();

        assert Objects.equals(this.getResolutionParameters(), that.getResolutionParameters());

        IResolutionParameters<L> mergedResolutionParameters = this.getResolutionParameters();
        // Predicate2<S, L> mergedEdgeClosedPredicate = (s, l) -> true;

        IEsopNameResolution.Immutable<S, L, O> mergedNameResolution = IEsopNameResolution
                .builder(mergedResolutionParameters, mergedGraphs, isEdgeClosed).freeze();

        this.solution = mergedNameResolution;

        return isModified;
    }

    @Override
    public IEsopNameResolution.Immutable<S, L, O> freeze() {
        return solution;
    }

}