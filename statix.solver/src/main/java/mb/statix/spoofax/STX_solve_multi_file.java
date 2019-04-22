package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.Predicate3;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.inject.Inject;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.statix.solver.IConstraint;
import mb.statix.solver.Solver;
import mb.statix.solver.SolverResult;
import mb.statix.solver.State;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.Spec;

public class STX_solve_multi_file extends StatixPrimitive {
    private static final ILogger logger = LoggerUtils.logger(STX_solve_multi_file.class);

    @Inject public STX_solve_multi_file() {
        super(STX_solve_multi_file.class.getSimpleName(), 2);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {

        final SolverResult initial = M.blobValue(SolverResult.class).match(terms.get(0))
                .orElseThrow(() -> new InterpreterException("Expected solver result."));
        final Spec spec = initial.state().spec();

        final IDebugContext debug = getDebugContext(terms.get(1));

        final IMatcher<Tuple2<String, Set<IConstraint>>> constraintMatcher = M.tuple2(M.stringValue(),
                StatixTerms.constraints(spec.labels()), (t, r, c) -> ImmutableTuple2.of(r, c));
        final Function1<Tuple2<String, Set<IConstraint>>, ITerm> solveConstraint =
                resource_constraints -> solveConstraint(initial.state().withResource(resource_constraints._1()),
                        resource_constraints._2(), debug);
        final List<Tuple2<String, Set<IConstraint>>> constraints = M.listElems(constraintMatcher).match(term)
                .orElseThrow(() -> new InterpreterException("Expected list of constraints."));
        final double t0 = System.currentTimeMillis();
        final List<ITerm> results =
                constraints.stream().parallel().map(solveConstraint::apply).collect(Collectors.toList());
        final double dt = System.currentTimeMillis() - t0;
        logger.info("Files analyzed in {} s", (dt / 1_000d));
        return Optional.of(B.newList(results));
    }

    private ITerm solveConstraint(State state, Set<IConstraint> constraints, IDebugContext debug) {
        final Predicate3<ITerm, ITerm, State> isComplete = (s, l, st) -> !state.scopes().contains(s);
        final SolverResult resultConfig;
        try {
            resultConfig = Solver.solve(state, constraints, isComplete, debug);
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }
        final ITerm resultTerm = B.newBlob(resultConfig);
        return resultTerm;
    }

}