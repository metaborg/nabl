package mb.statix.random.strategy;

import org.metaborg.util.functions.Function1;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.statix.random.SearchContext;
import mb.statix.random.SearchNode;
import mb.statix.random.SearchNodes;
import mb.statix.random.SearchState;
import mb.statix.random.SearchStrategy;
import mb.statix.solver.IConstraint;

final class Transform extends SearchStrategy<SearchState, SearchState> {

    private final Function1<IConstraint, IConstraint> f;

    Transform(Function1<IConstraint, IConstraint> f) {
        this.f = f;
    }

    @Override protected SearchNodes<SearchState> doApply(SearchContext ctx, SearchState input, SearchNode<?> parent) {
        final Set.Immutable<IConstraint> constraints =
                input.constraints().stream().map(f::apply).collect(CapsuleCollectors.toSet());
        final SearchState output = input.update(input.state(), constraints);
        return SearchNodes.of(new SearchNode<>(ctx.nextNodeId(), output, parent, "transform"));
    }

    @Override public String toString() {
        return "transform";
    }

}