package mb.statix.generator.strategy;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;
import org.metaborg.util.functions.Function2;
import org.metaborg.util.tuple.Tuple2;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.statix.constraints.CUser;
import mb.statix.generator.FocusedSearchState;
import mb.statix.generator.SearchContext;
import mb.statix.generator.SearchState;
import mb.statix.generator.SearchStrategy;
import mb.statix.generator.nodes.SearchNode;
import mb.statix.generator.nodes.SearchNodes;
import mb.statix.generator.util.RandomGenerator;
import mb.statix.generator.util.StreamUtil;
import mb.statix.solver.CriticalEdge;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.spec.ApplyMode;
import mb.statix.spec.ApplyMode.Safety;
import mb.statix.spec.ApplyResult;
import mb.statix.spec.Rule;
import mb.statix.spec.RuleSet;
import mb.statix.spec.RuleUtil;


public final class Expand extends SearchStrategy<FocusedSearchState<CUser>, SearchState> {

    private final Mode mode;
    private final Function2<Rule, Long, Double> ruleWeight;
    @Nullable private final RuleSet rules;

    Expand(Mode mode, Function2<Rule, Long, Double> ruleWeight) {
        this(mode, ruleWeight, null);
    }

    Expand(Mode mode, Function2<Rule, Long, Double> ruleWeight, @Nullable RuleSet rules) {
        this.mode = mode;
        this.ruleWeight = ruleWeight;
        this.rules = rules;
    }

    final Cache<String, java.util.Map<Rule, Double>> cache = CacheBuilder.newBuilder().maximumSize(1000).build();

    @Override protected SearchNodes<SearchState> doApply(SearchContext ctx,
            SearchNode<FocusedSearchState<CUser>> node) {
        final FocusedSearchState<CUser> input = node.output();
        final CUser predicate = input.focus();

        final java.util.Map<Rule, Double> rules = getWeightedRules(ctx, predicate.name());
        // UNSAFE : we assume the resource of spec variables is empty and of state variables non-empty
        final List<Tuple2<Rule, ApplyResult>> results = RuleUtil.applyAll(input.state().unifier(), rules.keySet(),
                predicate.args(), predicate, ApplyMode.RELAXED, Safety.UNSAFE);

        final List<Pair<SearchNode<SearchState>, Double>> newNodes = Lists.newArrayList();
        results.forEach(result -> {
            final Rule rule = result._1();
            final Optional<SearchState> output = updateSearchState(ctx, predicate, result._2(), input);
            if(!output.isPresent()) {
                return;
            }
            final String head = rule.name()
                    + rule.params().stream().map(Object::toString).collect(Collectors.joining(", ", "(", ")"));
            final SearchNode<SearchState> newNode =
                    new SearchNode<>(ctx.nextNodeId(), output.get(), node, "expand(" + head + ")");
            final double weight = rules.get(rule);
            if(weight > 0) {
                newNodes.add(new Pair<>(newNode, weight));
            }
        });
        if(newNodes.isEmpty()) {
            return SearchNodes.failure(node, "expand[no rules for " + predicate.name() + "]");
        }

        final Stream<SearchNode<SearchState>> nodes;
        switch(mode) {
            case ENUM:
                Collections.shuffle(newNodes, ctx.rnd()); // Important!
                nodes = newNodes.stream().map(Pair::getKey);
                break;
            case RND:
                final EnumeratedDistribution<SearchNode<SearchState>> ruleDist =
                        new EnumeratedDistribution<>(new RandomGenerator(ctx.rnd()), newNodes);
                nodes = StreamUtil.generate(ruleDist);
                break;
            default:
                throw new IllegalStateException();
        }

        final String desc = this.toString() + "[" + newNodes.size() + "]";
        return SearchNodes.of(node, () -> desc, nodes);
    }

    /**
     * Return a map with ordered keys mapping rules to their weights.
     */
    private java.util.Map<Rule, Double> getWeightedRules(SearchContext ctx, String name) {
        try {
            return cache.get(name, () -> {
                RuleSet rules = this.rules != null ? this.rules : ctx.spec().rules();
                final ImmutableSet<Rule> rs = rules.getOrderIndependentRules(name);
                final java.util.Map<String, Long> rcs =
                        rs.stream().collect(Collectors.groupingBy(Rule::label, Collectors.counting()));
                // ImmutableMap iterates over keys in insertion-order
                final ImmutableMap.Builder<Rule, Double> ruleWeights = ImmutableMap.builder();
                rs.forEach(r -> {
                    long count = rcs.getOrDefault(r.label(), 1L);
                    double weight = ruleWeight.apply(r, count);
                    ruleWeights.put(r, weight);
                });
                return ruleWeights.build();
            });
        } catch(ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<SearchState> updateSearchState(SearchContext ctx, IConstraint predicate, ApplyResult result,
            SearchState input) {
        final IConstraint applyConstraint = result.body();
        final IState.Immutable applyState = input.state();
        final IUniDisunifier.Immutable applyUnifier = applyState.unifier();

        // update constraints
        final Set.Transient<IConstraint> constraints = input.constraints().asTransient();
        constraints.__insert(applyConstraint);
        constraints.__remove(predicate);

        // update completeness
        final ICompleteness.Transient completeness = input.completeness().melt();
        completeness.add(applyConstraint, ctx.spec(), applyUnifier);
        java.util.Set<CriticalEdge> removedEdges = completeness.remove(predicate, ctx.spec(), applyUnifier);

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
        final SearchState newState =
                input.replace(applyState, constraints.freeze(), delays.freeze(), completeness.freeze());
        return Optional.of(newState);
    }

    @Override public String toString() {
        return "expand";
    }

}