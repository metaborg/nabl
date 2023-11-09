package mb.nabl2.spoofax.primitives;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Optional;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.ImList;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.log.PrintlineLogger;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;
import org.metaborg.util.tuple.Tuple2;
import org.spoofax.interpreter.core.InterpreterException;

import io.usethesource.capsule.Set;
import mb.nabl2.constraints.Constraints;
import mb.nabl2.constraints.IConstraint;
import mb.nabl2.constraints.equality.CEqual;
import mb.nabl2.solver.Fresh;
import mb.nabl2.solver.ISolution;
import mb.nabl2.solver.exceptions.SolverException;
import mb.nabl2.solver.solvers.BaseSolution;
import mb.nabl2.solver.solvers.GraphSolution;
import mb.nabl2.solver.solvers.SemiIncrementalMultiFileSolver;
import mb.nabl2.spoofax.analysis.IResult;
import mb.nabl2.spoofax.analysis.MultiInitialResult;
import mb.nabl2.spoofax.analysis.MultiUnitResult;
import mb.nabl2.terms.ITerm;

public class SG_solve_multi_unit_constraint extends ScopeGraphMultiFileAnalysisPrimitive {

    private static final PrintlineLogger log = PrintlineLogger.logger(SG_solve_multi_unit_constraint.class);

    @SuppressWarnings("unused") private static ILogger logger =
            LoggerUtils.logger(SG_solve_multi_unit_constraint.class);

    public SG_solve_multi_unit_constraint() {
        super(SG_solve_multi_unit_constraint.class.getSimpleName(), 0);
    }

    @Override protected Optional<? extends ITerm> call(ITerm currentTerm, List<ITerm> argTerms,
            SemiIncrementalMultiFileSolver solver, ICancel cancel, IProgress progress) throws InterpreterException {
        final Tuple2<MultiInitialResult, IConstraint> input =
                M.tuple2(M.blobValue(MultiInitialResult.class), Constraints.matchConstraintOrList(), (t, ir, C) -> {
                    return Tuple2.of(ir, C);
                }).match(currentTerm)
                        .orElseThrow(() -> new InterpreterException("Current term is not (InitialResult, C)."));
        final MultiInitialResult initialResult = input._1();
        final Set.Immutable<IConstraint> constraints = CapsuleUtil.immutableSet(input._2());

        if(log.enabled()) {
            log.debug("start multi_unit; equality constraints: ");
            constraints.stream()
                .filter(CEqual.class::isInstance)
                .forEach(c -> log.debug("* {}", c));
        }

        final Fresh.Transient unitFresh = Fresh.Transient.of();

        final ISolution solution;
        try {
            BaseSolution baseSolution = BaseSolution.of(initialResult.solution().config(), constraints,
                    initialResult.solution().unifier());
            GraphSolution preSolution = solver.solveGraph(baseSolution, unitFresh::fresh, cancel, progress);
            solution = solver.solveIntra(preSolution, initialResult.globalVars(), initialResult.globalScopes(),
                    unitFresh::fresh, cancel, progress);
        } catch(InterruptedException | SolverException ex) {
            throw new InterpreterException(ex);
        }

        if(log.enabled()) {
            log.debug("finish multi_unit; deferred equality constraints: ");
            solution.constraints().stream()
                .filter(CEqual.class::isInstance)
                .forEach(c -> log.debug("* {}", c));
        }

        final IResult result = MultiUnitResult.of(constraints, solution, Optional.empty(), unitFresh.freeze());
        return Optional.of(B.newBlob(result));
    }

}
