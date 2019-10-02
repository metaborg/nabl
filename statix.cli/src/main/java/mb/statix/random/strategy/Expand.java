package mb.statix.random.strategy;

import static mb.statix.constraints.Constraints.conjoin;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.ImmutableTuple3;
import mb.nabl2.util.Tuple3;
import mb.statix.constraints.CConj;
import mb.statix.constraints.CInequal;
import mb.statix.constraints.CTrue;
import mb.statix.constraints.CUser;
import mb.statix.random.FocusedSearchState;
import mb.statix.random.SearchContext;
import mb.statix.random.SearchState;
import mb.statix.random.SearchStrategy;
import mb.statix.random.nodes.SearchNode;
import mb.statix.random.nodes.SearchNodes;
import mb.statix.random.util.RandomGenerator;
import mb.statix.random.util.StreamUtil;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.spec.ApplyResult;
import mb.statix.spec.Rule;
import mb.statix.spec.RuleUtil;

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

        final List<Rule> rules = input.state().spec().rules().get(predicate.name());
        final java.util.Map<String, Long> ruleCounts =
                rules.stream().collect(Collectors.groupingBy(Rule::label, Collectors.counting()));

        // next block is a almost a copy of the CUser case in the solver and must be kept in sync
        final Iterator<Rule> it = rules.iterator();
        final Set.Transient<Tuple3<Rule, ApplyResult, IConstraint>> _results = Set.Transient.of();
        IConstraint unguard = new CTrue(predicate); // we build this iteratively for all previous rules we ignored
        while(it.hasNext()) {
            final Rule rule = it.next();
            final ApplyResult applyResult;
            if((applyResult = RuleUtil.apply(input.state(), rule, predicate.args(), predicate).orElse(null)) == null) {
                // ignore
            } else {
                _results.__insert(ImmutableTuple3.of(rule, applyResult, unguard));
                final ImmutableMap<ITermVar, ITerm> guard = applyResult.guard();
                if(guard.isEmpty()) {
                    break;
                } else {
                    final IConstraint _unguard = conjoin(guard.entrySet().stream()
                            .map(e -> new CInequal(e.getKey(), e.getValue(), predicate)).collect(Collectors.toList()));
                    unguard = new CConj(unguard, _unguard, predicate);
                }
            }
        }
        final Set.Immutable<Tuple3<Rule, ApplyResult, IConstraint>> results = _results.freeze();

        final List<Pair<SearchNode<SearchState>, Double>> newNodes = Lists.newArrayList();
        results.forEach(result -> {
            final Rule rule = result._1();
            final SearchState output = updateSearchState(predicate, result._2(), result._3(), input);
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
        if(newNodes.isEmpty()) {
            return SearchNodes.failure(node, "expand[no rules for " + predicate.name() + "]");
        }

        final EnumeratedDistribution<SearchNode<SearchState>> ruleDist =
                new EnumeratedDistribution<>(new RandomGenerator(ctx.rnd()), newNodes);
        final Stream<SearchNode<SearchState>> nodes = StreamUtil.generate(ruleDist);

        final String desc = this.toString() + "[" + newNodes.size() + "]";
        return SearchNodes.of(node, () -> desc, nodes);
    }

    private SearchState updateSearchState(IConstraint predicate, ApplyResult result, IConstraint unguard,
            SearchState input) {
        final IState.Immutable applyState = result.state();
        final IUnifier.Immutable applyUnifier = applyState.unifier();
        final IConstraint applyConstraint = new CConj(unguard, result.body(), predicate);

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
        return input.replace(applyState, constraints.freeze(), delays.freeze(), completeness.freeze());
    }

    @Override public String toString() {
        return "expand" + weights.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue())
                .collect(Collectors.joining(", ", "(", ")"));
    }

}