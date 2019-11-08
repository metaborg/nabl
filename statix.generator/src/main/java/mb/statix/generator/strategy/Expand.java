package mb.statix.generator.strategy;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;
import org.metaborg.util.functions.Function2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.Diseq;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.ImmutableTuple3;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.Tuple3;
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

    private static ILogger log = LoggerUtils.logger(Expand.class);

    private final Mode mode;
    private final Function2<Rule, Long, Double> ruleWeight;

    Expand(Mode mode, Function2<Rule, Long, Double> ruleWeight) {
        this.mode = mode;
        this.ruleWeight = ruleWeight;
    }

    final Cache<String, List<Tuple2<Rule, Double>>> cache = CacheBuilder.newBuilder().maximumSize(1000).build();

    @Override protected SearchNodes<SearchState> doApply(SearchContext ctx,
            SearchNode<FocusedSearchState<CUser>> node) {
        final FocusedSearchState<CUser> input = node.output();
        final CUser predicate = input.focus();

        final List<Tuple2<Rule, Double>> rules = getRules(input.state().spec(), predicate.name());

        // next block is a almost a copy of the CUser case in the solver and must be kept in sync
        final Iterator<Tuple2<Rule, Double>> it = rules.iterator();
        final Set.Transient<Tuple3<Tuple2<Rule, Double>, ApplyResult, Set<Diseq>>> _results = Set.Transient.of();
        Set.Immutable<Diseq> unguard = Set.Immutable.of();
        while(it.hasNext()) {
            final Tuple2<Rule, Double> ruleAndWeight = it.next();
            final Rule rule = ruleAndWeight._1();
            final ApplyResult applyResult;
            if((applyResult = RuleUtil.apply(input.state(), rule, predicate.args(), predicate).orElse(null)) == null) {
                // ignore
            } else {
                _results.__insert(ImmutableTuple3.of(ruleAndWeight, applyResult, unguard));
                final Optional<Diseq> guard = applyResult.guard();
                if(!guard.isPresent()) {
                    break;
                } else {
                    unguard = unguard.__insert(guard.get());
                }
            }
        }
        final Set.Immutable<Tuple3<Tuple2<Rule, Double>, ApplyResult, Set<Diseq>>> results = _results.freeze();

        final List<Pair<SearchNode<SearchState>, Double>> newNodes = Lists.newArrayList();
        results.forEach(result -> {
            final Rule rule = result._1()._1();
            final Optional<SearchState> output = updateSearchState(predicate, result._2(), result._3(), input);
            if(!output.isPresent()) {
                return;
            }
            final String head = rule.name()
                    + rule.params().stream().map(Object::toString).collect(Collectors.joining(", ", "(", ")"));
            final SearchNode<SearchState> newNode =
                    new SearchNode<>(ctx.nextNodeId(), output.get(), node, "expand(" + head + ")");
            final double weight = result._1()._2();
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

    private List<Tuple2<Rule, Double>> getRules(Spec spec, String name) {
        try {
            return cache.get(name, () -> {
                final List<Rule> rs = spec.rules().get(name);
                final java.util.Map<String, Long> rcs =
                        rs.stream().collect(Collectors.groupingBy(Rule::label, Collectors.counting()));
                return rs.stream().map(r -> {
                    long count = rcs.getOrDefault(r.label(), 1l);
                    double weight = ruleWeight.apply(r, count);
                    return ImmutableTuple2.of(r, weight);
                }).collect(Collectors.toList());
            });
        } catch(ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<SearchState> updateSearchState(IConstraint predicate, ApplyResult result, Set<Diseq> unguard,
            SearchState input) {
        final IConstraint applyConstraint = result.body();

        // add disequalities
        final IUnifier.Transient _applyUnifier = result.state().unifier().melt();
        for(Diseq diseq : unguard) {
            Tuple3<Set<ITermVar>, ITerm, ITerm> _diseq = diseq.toTuple();
            if(!_applyUnifier.disunify(_diseq._1(), _diseq._2(), _diseq._3()).isPresent()) {
                log.warn("Rule seems overlapping with previous rule. This shouldn't really happen.");
                return Optional.empty();
            }
        }
        final IUnifier.Immutable applyUnifier = _applyUnifier.freeze();
        final IState.Immutable applyState = result.state().withUnifier(applyUnifier);

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