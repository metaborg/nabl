package mb.statix.random.strategy;

import org.metaborg.core.MetaborgRuntimeException;

import mb.nabl2.terms.unification.UnifierFormatter;
import mb.statix.constraints.Constraints;
import mb.statix.random.SearchContext;
import mb.statix.random.SearchState;
import mb.statix.random.SearchStrategy;
import mb.statix.random.nodes.SearchNode;
import mb.statix.random.nodes.SearchNodes;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.persistent.SolverResult;

final class Infer extends SearchStrategy<SearchState, SearchState> {

    @Override public SearchNodes<SearchState> doApply(SearchContext ctx, SearchState state, SearchNode<?> parent) {
        final SolverResult resultConfig;
        try {
            resultConfig = Solver.solve(state.state(), state.constraints(), state.delays(), state.completeness(),
                    new NullDebugContext());
        } catch(InterruptedException e) {
            throw new MetaborgRuntimeException(e);
        }
        if(resultConfig.hasErrors()) {
            final String msg = Constraints.toString(resultConfig.errors(),
                    new UnifierFormatter(resultConfig.state().unifier(), 3));
            return SearchNodes.failure(parent, "infer[" + msg + "]");
        }
        final SearchState newState = state.replace(resultConfig);
        return SearchNodes.of(parent, new SearchNode<>(ctx.nextNodeId(), newState, parent, this.toString()));
    }

    @Override public String toString() {
        return "infer";
    }

}