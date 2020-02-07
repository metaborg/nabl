package mb.statix.generator;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.ImmutableList;

import mb.statix.generator.nodes.SearchNode;
import mb.statix.generator.nodes.SearchNodes;
import mb.statix.solver.IConstraint;
import mb.statix.solver.persistent.State;
import mb.statix.spec.Spec;

public class RandomTermGenerator {

    private final SearchState initState;
    private final SearchStrategy<SearchState, SearchState> strategy;

    private final SearchLogger log;
    private final Spec spec;

    public RandomTermGenerator(Spec spec, IConstraint constraint, SearchStrategy<SearchState, SearchState> strategy,
            SearchLogger log) {
        this.spec = spec;
        this.initState = SearchState.of(spec, State.of(spec), ImmutableList.of(constraint));
        this.strategy = strategy;
        this.log = log;
    }

    public SearchNodes<SearchState> apply() {
        final long seed = System.currentTimeMillis();
        log.init(seed, strategy, initState.constraintsAndDelays());

        final AtomicInteger nodeId = new AtomicInteger();
        final Random rnd = new Random(seed);
        final SearchContext ctx = new SearchContext() {

            @Override
            public Spec spec() {
                return spec;
            }

            @Override public Random rnd() {
                return rnd;
            }

            @Override public int nextNodeId() {
                return nodeId.incrementAndGet();
            }

            @Override public void failure(SearchNodes<?> nodes) {
                log.failure(nodes);
            }

        };
        return strategy.apply(ctx, new SearchNode<>(ctx.nextNodeId(), initState, null, "init"));
    }

}