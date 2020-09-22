package mb.statix.generator;

import com.google.common.collect.ImmutableList;

import mb.statix.generator.nodes.SearchNode;
import mb.statix.generator.nodes.SearchNodes;
import mb.statix.solver.IConstraint;
import mb.statix.solver.persistent.State;
import mb.statix.spec.Spec;

public class RandomTermGenerator {

    private final SearchState initState;
    private final SearchStrategy<SearchState, SearchState> strategy;

    private final SearchLogger<SearchState, SearchState> log;
    private final Spec spec;

    public RandomTermGenerator(Spec spec, IConstraint constraint, SearchStrategy<SearchState, SearchState> strategy,
            SearchLogger<SearchState, SearchState> log) {
        this.spec = spec;
        this.initState = SearchState.of(spec, State.of(spec), ImmutableList.of(constraint));
        this.strategy = strategy;
        this.log = log;
    }

    public SearchNodes<SearchState> apply() {
        final long seed = System.currentTimeMillis();
        log.init(seed, strategy, initState.constraintsAndDelays());

        final SearchContext ctx = new DefaultSearchContext(spec, seed) {
            @Override public void failure(SearchNodes<?> nodes) {
                log.failure(nodes);
            }
        };
        return strategy.apply(ctx, new SearchNode<>(ctx.nextNodeId(), initState, null, "init"));
    }

}