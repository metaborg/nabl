package mb.statix.random.strategy;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.statix.constraints.CUser;
import mb.statix.random.FocusedSearchState;
import mb.statix.random.SearchContext;
import mb.statix.random.SearchState;
import mb.statix.random.SearchStrategy;
import mb.statix.random.nodes.SearchNode;
import mb.statix.random.nodes.SearchNodes;
import mb.statix.random.util.RandomGenerator;
import mb.statix.random.util.RuleUtil;
import mb.statix.random.util.StreamUtil;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.spec.Rule;

final class Expand extends SearchStrategy<FocusedSearchState<CUser>, SearchState> {
    private final double defaultWeight;
    private final java.util.Map<String, Double> weights;

    Expand(double defaultWeight, java.util.Map<String, Double> weights) {
        this.defaultWeight = defaultWeight;
        this.weights = weights;
    }

    @Override protected SearchNodes<SearchState> doApply(SearchContext ctx,
            SearchNode<FocusedSearchState<CUser>> node) {
        final FocusedSearchState<CUser> input = node.output();
        final CUser predicate = input.focus();
        final List<Pair<SearchNode<SearchState>, Double>> newNodes = Lists.newArrayList();

        final List<Rule> allRules = input.state().spec().rules().get(predicate.name());
        final java.util.Map<String, Long> ruleCounts =
                allRules.stream().collect(Collectors.groupingBy(Rule::label, Collectors.counting()));
        for(Rule rule : allRules) {
            apply(rule, predicate, input).ifPresent(output -> {
                final String head = rule.name()
                        + rule.params().stream().map(Object::toString).collect(Collectors.joining(", ", "(", ")"));
                final SearchNode<SearchState> newNode =
                        new SearchNode<>(ctx.nextNodeId(), output, node, "expand(" + head + ")");
                final double weight;
                if(weights.containsKey(rule.label())) {
                    double count = ruleCounts.getOrDefault(rule.label(), 1l);
                    weight = weights.get(rule.label()) / count;
                } else {
                    weight = defaultWeight;
                }
                if(weight > 0) {
                    newNodes.add(new Pair<>(newNode, weight));
                }
            });
        }
        if(newNodes.isEmpty()) {
            return SearchNodes.failure(node, "expand[no rules for " + predicate.name() + "]");
        }

        final EnumeratedDistribution<SearchNode<SearchState>> ruleDist =
                new EnumeratedDistribution<>(new RandomGenerator(ctx.rnd()), newNodes);
        final Stream<SearchNode<SearchState>> nodes = StreamUtil.generate(ruleDist);

        final String desc = this.toString() + "[" + newNodes.size() + "]";
        return SearchNodes.of(node, () -> desc, nodes);
    }

    private Optional<SearchState> apply(Rule rule, CUser predicate, SearchState input) {
        return RuleUtil.apply(input.state(), rule, predicate.args(), predicate).map(resultAndConstraint -> {
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
            java.util.Set<CriticalEdge> removedEdges = completeness.remove(predicate, applyState.unifier());

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
            return input.replace(applyState, constraints.freeze(), delays.freeze(), completeness.freeze());
        });

    }

    @Override public String toString() {
        return "expand" + weights.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue())
                .collect(Collectors.joining(", ", "(", ")"));
    }

}