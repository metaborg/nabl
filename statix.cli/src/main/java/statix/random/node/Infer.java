package statix.random.node;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import org.metaborg.core.MetaborgException;

import mb.statix.constraints.Constraints;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.persistent.SolverResult;
import statix.random.SearchNode;
import statix.random.SearchState;

public class Infer extends SearchNode<SearchState, SearchState> {

    public Infer(Random rnd) {
        super(rnd);
    }

    private final AtomicBoolean fired = new AtomicBoolean();

    @Override protected void doInit() {
        fired.set(false);
    }

    @Override protected Optional<SearchState> doNext() throws MetaborgException {
        if(fired.getAndSet(true)) {
            return Optional.empty();
        }
        final SolverResult resultConfig;
        try {
            resultConfig =
                    Solver.solve(input.state(), Constraints.conjoin(input.constraints()), new NullDebugContext());
        } catch(InterruptedException e) {
            throw new MetaborgException(e);
        }
        if(resultConfig.hasErrors()) {
            return Optional.empty();
        }
        final SearchState newState = input.from(resultConfig);
        return Optional.of(newState);
    }

    @Override public String toString() {
        return "inference";
    }

}