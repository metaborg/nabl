package mb.statix.generator.strategy;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;
import org.metaborg.util.functions.Function2;

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

final class Expand extends SearchStrategy<FocusedSearchState<CUser>, SearchState> {

    private final Mode mode;
    private final Function2<Rule, Long, Double> ruleWeight;

    Expand(Mode mode, Function2<Rule, Long, Double> ruleWeight) {
        this.mode = mode;
        this.ruleWeight = ruleWeight;
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
                    List<ITermVar> lefts = Lists.newArrayList();
                    List<ITerm> rights = Lists.newArrayList();
                    guard.forEach((v, t) -> {
                        lefts.add(v);
                        rights.add(t);
                    });
                    final IConstraint _unguard = new CInequal(B.newTuple(lefts), B.newTuple(rights), predicate);
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
            long count = ruleCounts.getOrDefault(rule.label(), 1l);
            final double weight = ruleWeight.apply(rule, count);
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
        return "expand";
    }

}