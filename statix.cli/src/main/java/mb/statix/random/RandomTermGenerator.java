package mb.statix.random;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.ImmutableList;

import mb.statix.random.nodes.SearchElement;
import mb.statix.random.nodes.SearchNodes;
import mb.statix.solver.IConstraint;
import mb.statix.solver.persistent.State;
import mb.statix.spec.Spec;

public class RandomTermGenerator {

    private final SearchState initState;
    private final SearchStrategy<SearchState, SearchState> strategy;

    private final SearchLogger log;

    public RandomTermGenerator(Spec spec, IConstraint constraint, SearchStrategy<SearchState, SearchState> strategy,
            SearchLogger log) {
        this.initState = SearchState.of(State.of(spec), ImmutableList.of(constraint));
        this.strategy = strategy;
        this.log = log;
    }

    public SearchNodes<SearchState> apply() {
        final long seed = System.currentTimeMillis();
        log.init(seed, strategy, initState.constraintsAndDelays());

        final AtomicInteger nodeId = new AtomicInteger();
        final Random rnd = new Random(seed);
        final SearchContext ctx = new SearchContext() {

            @Override public Random rnd() {
                return rnd;
            }

            @Override public int nextNodeId() {
                return nodeId.incrementAndGet();
            }

            @Override public void failure(SearchElement node) {
                log.failure(node);
            }

        };
        return strategy.apply(ctx, initState, null);
    }

}