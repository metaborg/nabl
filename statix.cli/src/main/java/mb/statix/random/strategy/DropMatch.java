package mb.statix.random.strategy;

import org.metaborg.util.functions.Predicate1;
import org.metaborg.util.iterators.Iterables2;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.statix.random.SearchContext;
import mb.statix.random.SearchNode;
import mb.statix.random.SearchNodes;
import mb.statix.random.SearchState;
import mb.statix.random.SearchStrategy;
import mb.statix.solver.IConstraint;

final class DropMatch<C extends IConstraint> extends SearchStrategy<SearchState, SearchState> {
    private final Class<C> clazz;
    private final Predicate1<C> match;

    DropMatch(Class<C> clazz, Predicate1<C> match) {
        this.clazz = clazz;
        this.match = match;
    }

    @Override protected SearchNodes<SearchState> doApply(SearchContext ctx, SearchState input, SearchNode<?> parent) {
        @SuppressWarnings("unchecked") final Set.Immutable<IConstraint> remove = input.constraints().stream()
                .filter(c -> clazz.isInstance(c) && match.test((C) c)).collect(CapsuleCollectors.toSet());
        final SearchState output = input.update(Iterables2.empty(), remove);
        final String desc = "drop(" + clazz.getSimpleName() + ", " + match + ")";
        return SearchNodes.of(new SearchNode<>(ctx.nextNodeId(), output, parent, desc));
    }

    @Override public String toString() {
        return "drop(" + clazz.getSimpleName() + ", " + match + ")";
    }

}