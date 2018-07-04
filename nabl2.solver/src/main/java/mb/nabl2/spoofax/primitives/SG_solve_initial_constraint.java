package mb.nabl2.spoofax.primitives;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Collection;
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
import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.solver.Fresh;
import mb.nabl2.solver.ISolution;
import mb.nabl2.solver.SolverConfig;
import mb.nabl2.solver.SolverException;
import mb.nabl2.solver.solvers.BaseSolver.BaseSolution;
import mb.nabl2.solver.solvers.BaseSolver.GraphSolution;
import mb.nabl2.solver.solvers.ImmutableBaseSolution;
import mb.nabl2.solver.solvers.SemiIncrementalMultiFileSolver;
import mb.nabl2.spoofax.analysis.ImmutableInitialResult;
import mb.nabl2.spoofax.analysis.InitialResult;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.Transform.T;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;

public class SG_solve_initial_constraint extends ScopeGraphAnalysisPrimitive {

    private static ILogger logger = LoggerUtils.logger(SG_solve_initial_constraint.class);

    public SG_solve_initial_constraint() {
        super(SG_solve_initial_constraint.class.getSimpleName(), 1);
    }

    @Override protected Optional<? extends ITerm> call(ITerm currentTerm, List<ITerm> argTerms,
            SemiIncrementalMultiFileSolver solver, ICancel cancel, IProgress progress) throws InterpreterException {
        final SolverConfig solverConfig = SolverConfig.matcher().match(argTerms.get(0))
                .orElseThrow(() -> new InterpreterException("Term argument is not a solver config."));

        final Tuple2<ITerm, IConstraint> input =
                M.tuple2(M.term(), Constraints.matchConstraintOrList(), (t, params, C) -> {
                    return ImmutableTuple2.of(params, C);
                }).match(currentTerm).orElseThrow(() -> new InterpreterException("Current term is not (params, C)."));
        final ITerm params = input._1();
        final IConstraint C = input._2();

        final Collection<ITermVar> globalVars = params.getVars();
        final Fresh globalFresh = new Fresh();

        final ISolution solution;
        try {
            BaseSolution baseSolution =
                    ImmutableBaseSolution.of(solverConfig, ImmutableList.of(C), PersistentUnifier.Immutable.of());
            GraphSolution preSolution = solver.solveGraph(baseSolution, globalFresh::fresh, cancel, progress);
            preSolution = solver.reportUnsolvedGraphConstraints(preSolution);
            solution = solver.solveIntra(preSolution, globalVars, null, globalFresh::fresh, cancel, progress);
        } catch(InterruptedException | SolverException ex) {
            throw new InterpreterException(ex);
        }

        final Collection<Scope> globalScopes =
                T.collecttd(t -> Scope.matcher().match(t, solution.unifier())).apply(params);

        final InitialResult result = ImmutableInitialResult.of(C, solution, globalVars, globalScopes, globalFresh);
        return Optional.of(B.newBlob(result));
    }

}