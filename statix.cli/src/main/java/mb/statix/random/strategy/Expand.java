package mb.statix.random.strategy;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Streams;

import mb.statix.constraints.CUser;
import mb.statix.random.FocusedSearchState;
import mb.statix.random.SearchContext;
import mb.statix.random.SearchNode;
import mb.statix.random.SearchState;
import mb.statix.random.SearchStrategy;
import mb.statix.random.util.RuleUtil;
import mb.statix.random.util.WeightedDrawSet;
import mb.statix.spec.Rule;

final class Expand extends SearchStrategy<FocusedSearchState<CUser>, SearchState> {
    private final Map<String, Integer> weights;

    Expand(Map<String, Integer> weights) {
        this.weights = weights;
    }

    @Override protected Stream<SearchNode<SearchState>> doApply(SearchContext ctx,
            FocusedSearchState<CUser> input, SearchNode<?> parent) {
        final CUser predicate = input.focus();
        final Map<Rule, Integer> rules = new HashMap<>();
        for(Rule rule : input.state().spec().rules().get(predicate.name())) {
            rules.put(rule, weights.getOrDefault(rule.label(), 1));
        }
        return WeightedDrawSet.of(rules).enumerate(ctx.rnd()).map(Map.Entry::getKey).flatMap(rule -> {
            return Streams.stream(RuleUtil.apply(input.state(), rule, predicate.args(), predicate))
                    .map(result -> {
                        final SearchState output =
                                input.update(result._1(), input.constraints().__insert(result._2()));
                        final String head = rule.name() + rule.params().stream().map(Object::toString)
                                .collect(Collectors.joining(", ", "(", ")"));
                        return new SearchNode<>(ctx.nextNodeId(), output, parent, "expand(" + head + ")");
                    });
        });
    }

    @Override public String toString() {
        return "expand" + weights.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue())
                .collect(Collectors.joining(", ", "(", ")"));
    }
}