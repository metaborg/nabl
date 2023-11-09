package mb.nabl2.spoofax.primitives;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.metaborg.util.collection.CapsuleUtil;
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
import mb.nabl2.solver.SolverConfig;
import mb.nabl2.solver.exceptions.SolverException;
import mb.nabl2.solver.solvers.BaseSolution;
import mb.nabl2.solver.solvers.GraphSolution;
import mb.nabl2.solver.solvers.SemiIncrementalMultiFileSolver;
import mb.nabl2.spoofax.analysis.IResult;
import mb.nabl2.spoofax.analysis.MultiInitialResult;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import static mb.nabl2.terms.matching.Transform.T;
import mb.nabl2.terms.unification.Unifiers;
import mb.scopegraph.pepm16.terms.Scope;

public class SG_solve_multi_initial_constraint extends ScopeGraphMultiFileAnalysisPrimitive {

    private static final PrintlineLogger log = PrintlineLogger.logger(SG_solve_multi_initial_constraint.class);

    @SuppressWarnings("unused") private static ILogger logger =
            LoggerUtils.logger(SG_solve_multi_initial_constraint.class);

    public SG_solve_multi_initial_constraint() {
        super(SG_solve_multi_initial_constraint.class.getSimpleName(), 1);
    }

    @Override protected Optional<? extends ITerm> call(ITerm currentTerm, List<ITerm> argTerms,
            SemiIncrementalMultiFileSolver solver, ICancel cancel, IProgress progress) throws InterpreterException {
        final SolverConfig solverConfig = SolverConfig.matcher().match(argTerms.get(0))
                .orElseThrow(() -> new InterpreterException("Term argument is not a solver config."));
        final Tuple2<ITerm, IConstraint> input =
                M.tuple2(M.term(), Constraints.matchConstraintOrList(), (t, params, C) -> {
                    return Tuple2.of(params, C);
                }).match(currentTerm).orElseThrow(() -> new InterpreterException("Current term is not (params, C)."));
        final ITerm params = input._1();
        final Set.Immutable<IConstraint> constraints = CapsuleUtil.immutableSet(input._2());

        if(log.enabled()) {
            log.debug("start multi_initial; equality constraints: ");
            constraints.stream()
                .filter(CEqual.class::isInstance)
                .forEach(c -> log.debug("* {}", c));
        }

        final Set.Immutable<ITermVar> globalVars = params.getVars();
        final Fresh.Transient globalFresh = Fresh.Transient.of();

        final ISolution solution;
        try {
            BaseSolution baseSolution = BaseSolution.of(solverConfig, constraints, Unifiers.Immutable.of());
            GraphSolution preSolution = solver.solveGraph(baseSolution, globalFresh::fresh, cancel, progress);
            solution = solver.solveIntra(preSolution, globalVars, null, globalFresh::fresh, cancel, progress);
        } catch(InterruptedException | SolverException ex) {
            throw new InterpreterException(ex);
        }

        final Collection<Scope> globalScopes =
                T.collecttd(t -> Scope.matcher().match(t, solution.unifier())).apply(params);

        if(log.enabled()) {
            log.debug("finish multi_init; deferred equality constraints: ");
            solution.constraints().stream()
                .filter(CEqual.class::isInstance)
                .forEach(c -> log.debug("* {}", c));
        }

        final IResult result = MultiInitialResult.of(constraints, solution, Optional.empty(), globalVars, globalScopes,
                globalFresh.freeze());
        return Optional.of(B.newBlob(result));
    }

}
