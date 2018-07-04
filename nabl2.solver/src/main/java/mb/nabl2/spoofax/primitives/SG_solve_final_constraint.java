package mb.nabl2.spoofax.primitives;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;
import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.constraints.messages.ImmutableMessageInfo;
import mb.nabl2.constraints.messages.MessageContent;
import mb.nabl2.constraints.messages.MessageKind;
import mb.nabl2.solver.Fresh;
import mb.nabl2.solver.ISolution;
import mb.nabl2.solver.SolverException;
import mb.nabl2.solver.solvers.SemiIncrementalMultiFileSolver;
import mb.nabl2.spoofax.analysis.Actions;
import mb.nabl2.spoofax.analysis.FinalResult;
import mb.nabl2.spoofax.analysis.ImmutableFinalResult;
import mb.nabl2.spoofax.analysis.InitialResult;
import mb.nabl2.spoofax.analysis.UnitResult;
import mb.nabl2.stratego.MessageTerms;
import mb.nabl2.terms.ITerm;

public class SG_solve_final_constraint extends ScopeGraphAnalysisPrimitive {

    private static ILogger logger = LoggerUtils.logger(SG_solve_final_constraint.class);

    public SG_solve_final_constraint() {
        super(SG_solve_final_constraint.class.getSimpleName(), 0);
    }

    @Override protected Optional<? extends ITerm> call(ITerm currentTerm, List<ITerm> argTerms,
            SemiIncrementalMultiFileSolver solver, ICancel cancel, IProgress progress) throws InterpreterException {
        final InitialResult initialResult;
        final List<UnitResult> unitResults;

        final Fresh globalFresh = initialResult.fresh();
        final ISolution initialSolution = initialResult.solution();
        final List<ISolution> unitSolutions =
                unitResults.stream().map(UnitResult::solution).collect(Collectors.toList());

        final ISolution solution;
        try {
            final Function1<String, String> fresh = globalFresh::fresh;
            final IMessageInfo defaultMessage =
                    ImmutableMessageInfo.of(MessageKind.ERROR, MessageContent.of(), Actions.sourceTerm(""));
            ISolution preSolution =
                    solver.solveInter(initialSolution, unitSolutions, defaultMessage, fresh, cancel, progress);
            preSolution = solver.reportUnsolvedConstraints(preSolution);
            solution = preSolution;
        } catch(InterruptedException | SolverException ex) {
            throw new InterpreterException(ex);
        }

        final FinalResult result = ImmutableFinalResult.of(solution);
        final ITerm errors = MessageTerms.toTerms(solution.messages().getErrors(), solution.unifier());
        final ITerm warnings = MessageTerms.toTerms(solution.messages().getWarnings(), solution.unifier());
        final ITerm notes = MessageTerms.toTerms(solution.messages().getNotes(), solution.unifier());
        final ITerm resultTerm = B.newTuple(B.newBlob(result), errors, warnings, notes);
        return Optional.of(resultTerm);
    }

}