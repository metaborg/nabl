package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Optional;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.statix.solver.IConstraint;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.solver.persistent.State;
import mb.statix.taico.util.TDebug;
import mb.statix.taico.util.TOverrides;
import mb.statix.taico.util.TTimings;

public class STX_solve_multi_file extends StatixPrimitive {
    private static final ILogger logger = LoggerUtils.logger(STX_solve_multi_file.class);

    @Inject public STX_solve_multi_file() {
        super(STX_solve_multi_file.class.getSimpleName(), 2);
    }
    
    @Override
    protected Optional<? extends ITerm> _call(IContext env, ITerm term, List<ITerm> terms) throws InterpreterException {
        TTimings.startPhase("STX_solve_multi_file", "Settings: " + TOverrides.print(), "Debug: " + TDebug.print(), "Input: " + term.toString());
        
        try {
            return super._call(env, term, terms);
        } finally {
            TTimings.endPhase("STX_solve_multi_file");
        }
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {

        TTimings.startPhase("init");
        final SolverResult initial = M.blobValue(SolverResult.class).match(terms.get(0))
                .orElseThrow(() -> new InterpreterException("Expected solver result."));

        final IDebugContext debug = getDebugContext(terms.get(1));

        final IMatcher<Tuple2<String, IConstraint>> constraintMatcher =
                M.tuple2(M.stringValue(), StatixTerms.constraint(), (t, r, c) -> ImmutableTuple2.of(r, c));
        final Function1<Tuple2<String, IConstraint>, ITerm> solveConstraint =
                resource_constraint -> solveConstraint(initial.state().withResource(resource_constraint._1()),
                        resource_constraint._2(), debug);
        
        TTimings.startPhase("constraint matching");
        final List<Tuple2<String, IConstraint>> constraints = M.listElems(constraintMatcher).match(term)
                .orElseThrow(() -> new InterpreterException("Expected list of constraints."));
        TTimings.endPhase("constraint matching");
        TTimings.endPhase("init");
        
        TTimings.startPhase("solving");
        final double t0 = System.currentTimeMillis();
        final List<ITerm> results =
                constraints.stream().parallel().map(solveConstraint::apply).collect(ImmutableList.toImmutableList());
        TTimings.endPhase("solving");
        final double dt = System.currentTimeMillis() - t0;
        logger.info("Files analyzed in {} s", (dt / 1_000d));
        return Optional.of(B.newList(results));
    }

    private ITerm solveConstraint(State state, IConstraint constraint, IDebugContext debug) {
        final IsComplete isComplete = (s, l, st) -> {
            return !state.scopes().contains(s);
        };
        final SolverResult resultConfig;
        try {
            resultConfig = Solver.solve(state, constraint, isComplete, debug);
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }
        final ITerm resultTerm = B.newBlob(resultConfig);
        return resultTerm;
    }

}
