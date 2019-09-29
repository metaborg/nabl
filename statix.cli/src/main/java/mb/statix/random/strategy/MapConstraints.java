package mb.statix.random.strategy;

import org.metaborg.util.functions.Function1;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.statix.random.SearchContext;
import mb.statix.random.SearchState;
import mb.statix.random.SearchStrategy;
import mb.statix.random.nodes.SearchNode;
import mb.statix.random.nodes.SearchNodes;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState.Immutable;
import mb.statix.solver.completeness.Completeness;
import mb.statix.solver.completeness.ICompleteness;

final class MapConstraints extends SearchStrategy<SearchState, SearchState> {

    private final Function1<IConstraint, IConstraint> f;

    MapConstraints(Function1<IConstraint, IConstraint> f) {
        this.f = f;
    }

    @Override protected SearchNodes<SearchState> doApply(SearchContext ctx, SearchState input, SearchNode<?> parent) {
        final Immutable state = input.state();
        final Set.Immutable<IConstraint> constraints =
                input.constraints().stream().map(f::apply).collect(CapsuleCollectors.toSet());
        final ICompleteness.Transient completeness = Completeness.Transient.of(state.spec());
        completeness.addAll(constraints, state.unifier());
        completeness.addAll(input.delays().keySet(), state.unifier());
        final SearchState output = input.replace(state, constraints, input.delays(), completeness.freeze());
        return SearchNodes.of(parent, this.toString(), new SearchNode<>(ctx.nextNodeId(), output, parent, this.toString()));
    }

    @Override public String toString() {
        return "map-constraints";
    }

}