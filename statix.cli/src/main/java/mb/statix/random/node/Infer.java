package mb.statix.random.node;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import org.metaborg.core.MetaborgException;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import mb.statix.constraints.Constraints;
import mb.statix.random.SearchNode;
import mb.statix.random.SearchState;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.persistent.SolverResult;

public class Infer extends SearchNode<SearchState, SearchState> {

    private static final ILogger log = LoggerUtils.logger(Infer.class);

    public Infer(Random rnd) {
        super(rnd);
    }

    private final AtomicBoolean fired = new AtomicBoolean();

    @Override protected void doInit() {
        fired.set(false);
    }

    @Override protected Optional<SearchState> doNext() throws MetaborgException, InterruptedException {
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