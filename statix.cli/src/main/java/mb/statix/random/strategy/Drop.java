package mb.statix.random.strategy;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.statix.random.SearchContext;
import mb.statix.random.SearchNode;
import mb.statix.random.SearchState;
import mb.statix.random.SearchStrategy;
import mb.statix.solver.IConstraint;

final class Drop extends SearchStrategy<SearchState, SearchState> {
    private final ImmutableSet<Class<? extends IConstraint>> classes;

    @SafeVarargs Drop(Class<? extends IConstraint>... classes) {
        this.classes = ImmutableSet.copyOf(classes);
    }

    @Override protected Stream<SearchNode<SearchState>> doApply(SearchContext ctx, SearchState input,
            SearchNode<?> parent) {
        final Set.Immutable<IConstraint> constraints = input.constraints().stream()
                .filter(c -> !classes.contains(c.getClass())).collect(CapsuleCollectors.toSet());
        final SearchState output = input.update(input.state(), constraints);
        final String desc =
                "drop" + classes.stream().map(Class::getSimpleName).collect(Collectors.joining(", ", "(", ")"));
        return Stream.of(new SearchNode<>(ctx.nextNodeId(), output, parent, desc));
    }

    @Override public String toString() {
        return "drop" + classes.stream().map(Class::getSimpleName).collect(Collectors.joining(", ", "(", ")"));
    }
}