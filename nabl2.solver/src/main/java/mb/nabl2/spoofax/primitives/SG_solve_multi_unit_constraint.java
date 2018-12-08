package mb.nabl2.spoofax.primitives;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Optional;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.ImmutableList;

import mb.nabl2.constraints.Constraints;
import mb.nabl2.constraints.IConstraint;
import mb.nabl2.solver.Fresh;
import mb.nabl2.solver.ISolution;
import mb.nabl2.solver.SolverException;
import mb.nabl2.solver.solvers.BaseSolver.BaseSolution;
import mb.nabl2.solver.solvers.BaseSolver.GraphSolution;
import mb.nabl2.solver.solvers.ImmutableBaseSolution;
import mb.nabl2.solver.solvers.SemiIncrementalMultiFileSolver;
import mb.nabl2.spoofax.analysis.IResult;
import mb.nabl2.spoofax.analysis.ImmutableMultiUnitResult;
import mb.nabl2.spoofax.analysis.MultiInitialResult;
import mb.nabl2.terms.ITerm;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;

public class SG_solve_multi_unit_constraint extends ScopeGraphMultiFileAnalysisPrimitive {

    private static ILogger logger = LoggerUtils.logger(SG_solve_multi_unit_constraint.class);

    public SG_solve_multi_unit_constraint() {
        super(SG_solve_multi_unit_constraint.class.getSimpleName(), 0);
    }

    @Override protected Optional<? extends ITerm> call(ITerm currentTerm, List<ITerm> argTerms,
            SemiIncrementalMultiFileSolver solver, ICancel cancel, IProgress progress) throws InterpreterException {
        final Tuple2<MultiInitialResult, IConstraint> input =
                M.tuple2(M.blobValue(MultiInitialResult.class), Constraints.matchConstraintOrList(), (t, ir, C) -> {
                    return ImmutableTuple2.of(ir, C);
                }).match(currentTerm)
                        .orElseThrow(() -> new InterpreterException("Current term is not (InitialResult, C)."));
        final MultiInitialResult initialResult = input._1();
        final List<IConstraint> constraints = ImmutableList.of(input._2());

        final Fresh.Transient unitFresh = Fresh.Transient.of();

        final ISolution solution;
        try {
            BaseSolution baseSolution = ImmutableBaseSolution.of(initialResult.solution().config(), constraints,
                    initialResult.solution().unifier());
            GraphSolution preSolution = solver.solveGraph(baseSolution, unitFresh::fresh, cancel, progress);
            solution = solver.solveIntra(preSolution, initialResult.globalVars(), initialResult.globalScopes(),
                    unitFresh::fresh, cancel, progress);
        } catch(InterruptedException | SolverException ex) {
            throw new InterpreterException(ex);
        }

        final IResult result = ImmutableMultiUnitResult.of(constraints, solution, Optional.empty(), unitFresh.freeze());
        return Optional.of(B.newBlob(result));
    }

}