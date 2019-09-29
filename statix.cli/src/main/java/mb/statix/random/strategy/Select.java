package mb.statix.random.strategy;

import org.metaborg.util.functions.Predicate1;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.statix.random.FocusedSearchState;
import mb.statix.random.SearchContext;
import mb.statix.random.SearchState;
import mb.statix.random.SearchStrategy;
import mb.statix.random.nodes.SearchNode;
import mb.statix.random.nodes.SearchNodes;
import mb.statix.random.util.WeightedDrawSet;
import mb.statix.solver.IConstraint;

final class Select<C extends IConstraint> extends SearchStrategy<SearchState, FocusedSearchState<C>> {
    private final Class<C> cls;
    private final Predicate1<C> include;

    Select(Class<C> cls, Predicate1<C> include) {
        this.cls = cls;
        this.include = include;
    }

    @Override protected SearchNodes<FocusedSearchState<C>> doApply(SearchContext ctx, SearchState input,
            SearchNode<?> parent) {
        @SuppressWarnings("unchecked") final Set.Immutable<C> candidates =
                input.constraints().stream().filter(c -> cls.isInstance(c)).map(c -> (C) c).filter(include::test)
                        .collect(CapsuleCollectors.toSet());
        if(candidates.isEmpty()) {
            return SearchNodes.failure(parent, this.toString() + "[no candidates]");
        }
        return SearchNodes.of(parent, WeightedDrawSet.of(candidates).enumerate(ctx.rnd()).map(c -> {
            final FocusedSearchState<C> output = FocusedSearchState.of(input, c.getKey());
            return new SearchNode<>(ctx.nextNodeId(), output, parent,
                    "select(" + c.getKey().toString(t -> input.state().unifier().toString(t)) + ")");
        }));
    }

    @Override public String toString() {
        return "select(" + cls.getSimpleName() + ", " + include.toString() + ")";
    }

}