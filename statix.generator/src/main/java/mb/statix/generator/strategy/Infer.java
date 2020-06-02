package mb.statix.generator.strategy;

import org.metaborg.util.task.NullCancel;
import org.metaborg.util.task.NullProgress;

import mb.statix.constraints.Constraints;
import mb.statix.generator.SearchContext;
import mb.statix.generator.SearchState;
import mb.statix.generator.SearchStrategy;
import mb.statix.generator.nodes.SearchNode;
import mb.statix.generator.nodes.SearchNodes;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.persistent.SolverResult;

public final class Infer extends SearchStrategy<SearchState, SearchState> {

    @Override public SearchNodes<SearchState> doApply(SearchContext ctx, SearchNode<SearchState> node) {
        final SearchState state = node.output();
        final SolverResult resultConfig;
        try {
            resultConfig = Solver.solve(ctx.spec(), state.state(), state.constraints(), state.delays(),
                    state.completeness(), new NullDebugContext(), new NullProgress(), new NullCancel());
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }
        if(resultConfig.hasErrors()) {
            final String msg =
                    Constraints.toString(resultConfig.messages().keySet(), resultConfig.state().unifier()::toString);
            return SearchNodes.failure(node, "infer[" + msg + "]");
        }
        final SearchState newState = state.replace(resultConfig);
        return SearchNodes.of(node, this::toString,
                new SearchNode<>(ctx.nextNodeId(), newState, node, this.toString()));
    }

    @Override public String toString() {
        return "infer";
    }

}