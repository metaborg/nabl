package mb.nabl2.spoofax.primitives;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.Optional;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.ImmutableList;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.solver.Fresh;
import mb.nabl2.solver.ISolution;
import mb.nabl2.solver.SolverException;
import mb.nabl2.solver.solvers.BaseSolver.BaseSolution;
import mb.nabl2.solver.solvers.BaseSolver.GraphSolution;
import mb.nabl2.solver.solvers.ImmutableBaseSolution;
import mb.nabl2.solver.solvers.SemiIncrementalMultiFileSolver;
import mb.nabl2.spoofax.analysis.ImmutableUnitResult;
import mb.nabl2.spoofax.analysis.InitialResult;
import mb.nabl2.spoofax.analysis.UnitResult;
import mb.nabl2.terms.ITerm;

public class SG_solve_unit_constraint extends ScopeGraphAnalysisPrimitive {

    private static ILogger logger = LoggerUtils.logger(SG_solve_unit_constraint.class);

    public SG_solve_unit_constraint() {
        super(SG_solve_unit_constraint.class.getSimpleName(), 0);
    }

    @Override protected Optional<? extends ITerm> call(ITerm currentTerm, List<ITerm> argTerms,
            SemiIncrementalMultiFileSolver solver, ICancel cancel, IProgress progress) throws InterpreterException {
        final InitialResult initialResult;
        final IConstraint C;

        final Fresh unitFresh = new Fresh();

        final ISolution solution;
        try {
            BaseSolution baseSolution = ImmutableBaseSolution.of(initialResult.solution().config(), ImmutableList.of(C),
                    initialResult.solution().unifier());
            GraphSolution preSolution = solver.solveGraph(baseSolution, unitFresh::fresh, cancel, progress);
            preSolution = solver.reportUnsolvedGraphConstraints(preSolution);
            solution = solver.solveIntra(preSolution, initialResult.globalVars(), initialResult.globalScopes(),
                    unitFresh::fresh, cancel, progress);
        } catch(InterruptedException | SolverException ex) {
            throw new InterpreterException(ex);
        }

        final UnitResult result = ImmutableUnitResult.of(C, solution);
        return Optional.of(B.newBlob(result));
    }

}