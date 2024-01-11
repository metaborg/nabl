package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Optional;

import org.metaborg.util.collection.ImList;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;
import org.metaborg.util.tuple.Tuple2;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;


import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.solver.tracer.EmptyTracer.Empty;
import mb.statix.spec.Spec;

public class STX_solve_multi_file extends StatixPrimitive {
    private static final ILogger logger = LoggerUtils.logger(STX_solve_multi_file.class);

    @jakarta.inject.Inject @javax.inject.Inject public STX_solve_multi_file() {
        super(STX_solve_multi_file.class.getSimpleName(), 5);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {

        final Spec spec =
                StatixTerms.spec().match(terms.get(0)).orElseThrow(() -> new InterpreterException("Expected spec."));
        reportOverlappingRules(spec);

        @SuppressWarnings("unchecked") final SolverResult<Empty> initial = M.blobValue(SolverResult.class)
                .match(terms.get(1)).orElseThrow(() -> new InterpreterException("Expected solver result."));

        final IDebugContext debug = getDebugContext(terms.get(2));
        final IProgress progress = getProgress(terms.get(3));
        final ICancel cancel = getCancel(terms.get(4));


        final IMatcher<Tuple2<String, IConstraint>> constraintMatcher =
                M.tuple2(M.stringValue(), StatixTerms.constraint(), (t, r, c) -> Tuple2.of(r, c));
        final Function1<Tuple2<String, IConstraint>, ITerm> solveConstraint =
                resource_constraint -> solveConstraint(spec, initial.state().withResource(resource_constraint._1()),
                        resource_constraint._2(), debug, progress, cancel);
        final List<Tuple2<String, IConstraint>> constraints = M.listElems(constraintMatcher).match(term)
                .orElseThrow(() -> new InterpreterException("Expected list of constraints."));
        final double t0 = System.currentTimeMillis();
        final List<ITerm> results =
                constraints.stream().parallel().map(solveConstraint::apply).collect(ImList.Immutable.toImmutableList());
        final double dt = System.currentTimeMillis() - t0;
        logger.info("Files analyzed in {} s", (dt / 1_000d));
        return Optional.of(B.newList(results));
    }

    private ITerm solveConstraint(Spec spec, IState.Immutable state, IConstraint constraint, IDebugContext debug,
            IProgress progress, ICancel cancel) {
        final IsComplete isComplete = (s, l, st) -> {
            return !state.scopes().contains(s);
        };
        final SolverResult<Empty> resultConfig;
        try {
            resultConfig = Solver.solve(spec, state, constraint, isComplete, debug, cancel, progress, 0);
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }
        final ITerm resultTerm = B.newBlob(resultConfig);
        return resultTerm;
    }

}
