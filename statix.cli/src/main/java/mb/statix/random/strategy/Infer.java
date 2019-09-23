package mb.statix.random.strategy;

import org.metaborg.core.MetaborgRuntimeException;

import mb.statix.constraints.Constraints;
import mb.statix.random.SearchContext;
import mb.statix.random.SearchNode;
import mb.statix.random.SearchNodes;
import mb.statix.random.SearchState;
import mb.statix.random.SearchStrategy;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.persistent.SolverResult;

final class Infer extends SearchStrategy<SearchState, SearchState> {

    @Override public SearchNodes<SearchState> doApply(SearchContext ctx, SearchState state, SearchNode<?> parent) {
        final SolverResult resultConfig;
        try {
            resultConfig =
                    Solver.solve(state.state(), Constraints.conjoin(state.constraints()), new NullDebugContext());
        } catch(InterruptedException e) {
            throw new MetaborgRuntimeException(e);
        }
        if(resultConfig.hasErrors()) {
            // final String msg = Constraints.toString(resultConfig.errors(),
            //   new UnifierFormatter(resultConfig.state().unifier(), 3));
            // ctx.addFailed(new SearchNode<>(ctx.nextNodeId(), state, parent, "infer[" + msg + "]"));
            return SearchNodes.of();
        }
        final SearchState newState = state.update(resultConfig);
        return SearchNodes.of(new SearchNode<>(ctx.nextNodeId(), newState, parent, "infer"));
    }

    @Override public String toString() {
        return "infer";
    }

}