package mb.statix.generator.strategy;

import org.metaborg.util.functions.Function1;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.statix.generator.SearchContext;
import mb.statix.generator.SearchState;
import mb.statix.generator.SearchStrategy;
import mb.statix.generator.nodes.SearchNode;
import mb.statix.generator.nodes.SearchNodes;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.Completeness;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.spec.Spec;

final class MapConstraints extends SearchStrategy<SearchState, SearchState> {

    private final Function1<IConstraint, IConstraint> f;

    MapConstraints(Spec spec, Function1<IConstraint, IConstraint> f) {
        super(spec);
        this.f = f;
    }

    @Override protected SearchNodes<SearchState> doApply(SearchContext ctx, SearchNode<SearchState> node) {
        final SearchState input = node.output();
        final IState.Immutable state = input.state();
        final Set.Immutable<IConstraint> constraints =
                input.constraints().stream().map(f::apply).collect(CapsuleCollectors.toSet());
        final ICompleteness.Transient completeness = Completeness.Transient.of(spec());
        completeness.addAll(constraints, state.unifier());
        completeness.addAll(input.delays().keySet(), state.unifier());
        final SearchState output = input.replace(state, constraints, input.delays(), completeness.freeze());
        return SearchNodes.of(node, this::toString, new SearchNode<>(ctx.nextNodeId(), output, node, this.toString()));
    }

    @Override public String toString() {
        return "map-constraints";
    }

}