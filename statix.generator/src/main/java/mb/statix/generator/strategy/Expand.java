package mb.statix.generator.strategy;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;
import org.metaborg.util.functions.Function2;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.nabl2.util.Tuple2;
import mb.statix.constraints.CUser;
import mb.statix.generator.FocusedSearchState;
import mb.statix.generator.SearchContext;
import mb.statix.generator.SearchState;
import mb.statix.generator.SearchStrategy;
import mb.statix.generator.nodes.SearchNode;
import mb.statix.generator.nodes.SearchNodes;
import mb.statix.generator.util.RandomGenerator;
import mb.statix.generator.util.StreamUtil;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.spec.ApplyResult;
import mb.statix.spec.Rule;
import mb.statix.spec.RuleUtil;
import mb.statix.spec.Spec;

final class Expand extends SearchStrategy<FocusedSearchState<CUser>, SearchState> {

    private final Mode mode;
    private final Function2<Rule, Long, Double> ruleWeight;
    private final ListMultimap<String, Rule> rules;

    Expand(Spec spec, Mode mode, Function2<Rule, Long, Double> ruleWeight) {
        this(spec, mode, ruleWeight, spec.rules());
    }

    Expand(Spec spec, Mode mode, Function2<Rule, Long, Double> ruleWeight, ListMultimap<String, Rule> rules) {
        super(spec);
        this.mode = mode;
        this.ruleWeight = ruleWeight;
        this.rules = rules;
    }

    final Cache<String, java.util.Map<Rule, Double>> cache = CacheBuilder.newBuilder().maximumSize(1000).build();

    @Override protected SearchNodes<SearchState> doApply(SearchContext ctx,
            SearchNode<FocusedSearchState<CUser>> node) {
        final FocusedSearchState<CUser> input = node.output();
        final CUser predicate = input.focus();

        final java.util.Map<Rule, Double> rules = getWeightedRules(predicate.name());
        final List<Tuple2<Rule, ApplyResult>> results =
                RuleUtil.applyAll(input.state(), rules.keySet(), predicate.args(), predicate);

        final List<Pair<SearchNode<SearchState>, Double>> newNodes = Lists.newArrayList();
        results.forEach(result -> {
            final Rule rule = result._1();
            final Optional<SearchState> output = updateSearchState(predicate, result._2(), input);
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
                nodes = newNodes.stream().map(p -> p.getKey());
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

    private java.util.Map<Rule, Double> getWeightedRules(String name) {
        try {
            return cache.get(name, () -> {
                final List<Rule> rs = rules.get(name);
                final java.util.Map<String, Long> rcs =
                        rs.stream().collect(Collectors.groupingBy(Rule::label, Collectors.counting()));
                final ImmutableMap.Builder<Rule, Double> ruleWeights = ImmutableMap.builder();
                rs.forEach(r -> {
                    long count = rcs.getOrDefault(r.label(), 1l);
                    double weight = ruleWeight.apply(r, count);
                    ruleWeights.put(r, weight);
                });
                return ruleWeights.build();
            });
        } catch(ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<SearchState> updateSearchState(IConstraint predicate, ApplyResult result, SearchState input) {
        final IConstraint applyConstraint = result.body();
        final IState.Immutable applyState = result.state();
        final IUniDisunifier.Immutable applyUnifier = applyState.unifier();

        // update constraints
        final Set.Transient<IConstraint> constraints = input.constraints().asTransient();
        constraints.__insert(applyConstraint);
        constraints.__remove(predicate);

        // update completeness
        final ICompleteness.Transient completeness = input.completeness().melt();
        completeness.updateAll(result.updatedVars(), applyUnifier);
        completeness.add(applyConstraint, applyUnifier);
        java.util.Set<CriticalEdge> removedEdges = completeness.remove(predicate, applyUnifier);

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