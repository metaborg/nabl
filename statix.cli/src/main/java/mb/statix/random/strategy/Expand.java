package mb.statix.random.strategy;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import com.google.common.collect.Streams;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.statix.constraints.CUser;
import mb.statix.random.FocusedSearchState;
import mb.statix.random.SearchContext;
import mb.statix.random.SearchState;
import mb.statix.random.SearchStrategy;
import mb.statix.random.nodes.SearchNode;
import mb.statix.random.nodes.SearchNodes;
import mb.statix.random.util.RuleUtil;
import mb.statix.random.util.WeightedDrawSet;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.spec.Rule;

final class Expand extends SearchStrategy<FocusedSearchState<CUser>, SearchState> {
    private final int defaultWeight;
    private final java.util.Map<String, Integer> weights;

    Expand(int defaultWeight, java.util.Map<String, Integer> weights) {
        this.defaultWeight = defaultWeight;
        this.weights = weights;
    }

    @Override protected SearchNodes<SearchState> doApply(SearchContext ctx, FocusedSearchState<CUser> input,
            SearchNode<?> parent) {
        final CUser predicate = input.focus();
        final java.util.Map<Rule, Integer> rules = new HashMap<>();
        for(Rule rule : input.state().spec().rules().get(predicate.name())) {
            rules.put(rule, weights.getOrDefault(rule.label(), defaultWeight));
        }
        if(rules.isEmpty()) {
            return SearchNodes.failure(parent, "expand[no rules for " + predicate.name() + "]");
        }
        return SearchNodes.of(parent, WeightedDrawSet.of(rules).enumerate(ctx.rnd()).map(Entry::getKey).flatMap(rule -> {
            return Streams.stream(RuleUtil.apply(input.state(), rule, predicate.args(), predicate))
                    .map(resultAndConstraint -> {
                        final SolverResult applyResult = resultAndConstraint._1();
                        final IState.Immutable applyState = applyResult.state();
                        final IConstraint applyConstraint = resultAndConstraint._2();

                        // update constraints
                        final Set.Transient<IConstraint> constraints = input.constraints().asTransient();
                        constraints.__insert(applyConstraint);
                        constraints.__remove(predicate);

                        // update completeness
                        final ICompleteness.Transient completeness = input.completeness().melt();
                        completeness.updateAll(applyResult.updatedVars(), applyState.unifier());
                        completeness.add(applyConstraint, applyState.unifier());
                        java.util.Set<CriticalEdge> removedEdges =
                                completeness.remove(predicate, applyState.unifier());

                        // update delays
                        final Map.Transient<IConstraint, Delay> delays = Map.Transient.of();
                        input.delays().forEach((c, d) -> {
                            if(!Sets.intersection(d.criticalEdges(), removedEdges).isEmpty()) {
                                constraints.__insert(c);
                            } else {
                                delays.__put(c, d);
                            }
                        });

                        // return new state
                        final SearchState output = input.replace(applyState, constraints.freeze(),
                                delays.freeze(), completeness.freeze());
                        final String head = rule.name() + rule.params().stream().map(Object::toString)
                                .collect(Collectors.joining(", ", "(", ")"));
                        return new SearchNode<>(ctx.nextNodeId(), output, parent, "expand(" + head + ")");
                    });
        }));
    }

    @Override public String toString() {
        return "expand" + weights.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue())
                .collect(Collectors.joining(", ", "(", ")"));
    }

}