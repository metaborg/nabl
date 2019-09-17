package mb.statix.random.strategy;

import java.util.stream.Stream;

import org.metaborg.core.MetaborgRuntimeException;

import mb.nabl2.terms.unification.UnifierFormatter;
import mb.statix.constraints.Constraints;
import mb.statix.random.SearchContext;
import mb.statix.random.SearchNode;
import mb.statix.random.SearchState;
import mb.statix.random.SearchStrategy;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.persistent.SolverResult;

final class Infer extends SearchStrategy<SearchState, SearchState> {
    @Override public Stream<SearchNode<SearchState>> doApply(SearchContext ctx, SearchState state,
            SearchNode<?> parent) {
        final SolverResult resultConfig;
        if(state.state().unifier().varSet().stream()
                .anyMatch(v -> state.state().unifier().size(v).match(n -> n.intValue() > 40, () -> true))) {
            @SuppressWarnings("unused") String s = "biggy";
        }
        try {
            resultConfig = Solver.solve(state.state(), Constraints.conjoin(state.constraints()),
                    new NullDebugContext());
        } catch(InterruptedException e) {
            throw new MetaborgRuntimeException(e);
        }
        if(resultConfig.hasErrors()) {
            final String msg = Constraints.toString(resultConfig.errors(),
                    new UnifierFormatter(resultConfig.state().unifier(), 3));
            ctx.addFailed(new SearchNode<>(ctx.nextNodeId(), state, parent, "infer[" + msg + "]"));
            return Stream.empty();
        }
        final SearchState newState = state.update(resultConfig);
        return Stream.of(new SearchNode<>(ctx.nextNodeId(), newState, parent, "infer"));
    }

    @Override public String toString() {
        return "infer";
    }
}