package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.inject.Inject;

import mb.nabl2.terms.ITerm;
import mb.statix.solver.IConstraint;
import mb.statix.solver.Solver;
import mb.statix.solver.SolverResult;
import mb.statix.solver.State;
import mb.statix.solver.log.IDebugContext;

public class STX_solve_multi_project extends StatixPrimitive {
    private static final ILogger logger = LoggerUtils.logger(STX_solve_multi_project.class);

    @Inject public STX_solve_multi_project() {
        super(STX_solve_multi_project.class.getSimpleName(), 2);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {

        final SolverResult initial = M.blobValue(SolverResult.class).match(terms.get(0))
                .orElseThrow(() -> new InterpreterException("Expected solver result."));

        final IDebugContext debug = getDebugContext(terms.get(1));

        final List<SolverResult> results = M.listElems(M.blobValue(SolverResult.class)).match(term)
                .orElseThrow(() -> new InterpreterException("Expected list of solver results."));

        final Set<IConstraint> constraints = new HashSet<>(initial.delays().keySet());
        final Set<IConstraint> errors = new HashSet<>(initial.errors());
        State state = initial.state();
        for(SolverResult result : results) {
            state = state.add(result.state());
            constraints.addAll(result.delays().keySet());
            errors.addAll(result.errors());
        }

        final SolverResult resultConfig;
        try {
            final double t0 = System.currentTimeMillis();
            resultConfig = Solver.solve(state, constraints, (s, l, st) -> true, debug);
            final double dt = System.currentTimeMillis() - t0;
            logger.info("project analyzed in {} s", (dt / 1_000d));
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }
        errors.addAll(resultConfig.errors());
        final ITerm resultTerm = B.newBlob(resultConfig.withErrors(errors));
        return Optional.of(resultTerm);
    }

}