package mb.nabl2.spoofax.primitives;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;
import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.constraints.messages.ImmutableMessageInfo;
import mb.nabl2.constraints.messages.MessageContent;
import mb.nabl2.constraints.messages.MessageKind;
import mb.nabl2.solver.Fresh;
import mb.nabl2.solver.ISolution;
import mb.nabl2.solver.SolverException;
import mb.nabl2.solver.messages.IMessages;
import mb.nabl2.solver.solvers.SemiIncrementalMultiFileSolver;
import mb.nabl2.spoofax.analysis.Actions;
import mb.nabl2.spoofax.analysis.IResult;
import mb.nabl2.spoofax.analysis.ImmutableMultiFinalResult;
import mb.nabl2.spoofax.analysis.MultiInitialResult;
import mb.nabl2.spoofax.analysis.MultiUnitResult;
import mb.nabl2.stratego.MessageTerms;
import mb.nabl2.terms.ITerm;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;

public class SG_solve_multi_final_constraint extends ScopeGraphMultiFileAnalysisPrimitive {

    private static ILogger logger = LoggerUtils.logger(SG_solve_multi_final_constraint.class);

    public SG_solve_multi_final_constraint() {
        super(SG_solve_multi_final_constraint.class.getSimpleName(), 0);
    }

    @Override protected Optional<? extends ITerm> call(ITerm currentTerm, List<ITerm> argTerms,
            SemiIncrementalMultiFileSolver solver, ICancel cancel, IProgress progress) throws InterpreterException {
        final Tuple2<MultiInitialResult, List<MultiUnitResult>> input = M.tuple2(M.blobValue(MultiInitialResult.class),
                M.listElems(M.blobValue(MultiUnitResult.class)), (t, ir, urs) -> {
                    return ImmutableTuple2.of(ir, urs);
                }).match(currentTerm)
                .orElseThrow(() -> new InterpreterException("Current term is not (InitialResult, [UnitResult])."));
        final MultiInitialResult initialResult = input._1();
        final List<MultiUnitResult> unitResults = input._2();

        final Fresh.Transient globalFresh = initialResult.fresh().melt();
        final ISolution initialSolution = initialResult.solution();
        final List<ISolution> unitSolutions =
                unitResults.stream().map(MultiUnitResult::solution).collect(Collectors.toList());

        final ISolution solution;
        try {
            final Function1<String, String> fresh = globalFresh::fresh;
            final IMessageInfo defaultMessage =
                    ImmutableMessageInfo.of(MessageKind.ERROR, MessageContent.of(), Actions.sourceTerm(""));
            ISolution preSolution =
                    solver.solveInter(initialSolution, unitSolutions, defaultMessage, fresh, cancel, progress);
            solution = preSolution;
        } catch(InterruptedException | SolverException ex) {
            throw new InterpreterException(ex);
        }

        final List<IConstraint> constraints = Stream.concat(initialResult.constraints().stream(),
                unitResults.stream().flatMap(ur -> ur.constraints().stream())).collect(Collectors.toList());
        final IResult result =
                ImmutableMultiFinalResult.of(constraints, solution, Optional.empty(), globalFresh.freeze());
        final IMessages.Immutable messages = solution.messagesAndUnsolvedErrors();
        final ITerm errors = MessageTerms.toTerms(messages.getErrors(), solution.unifier());
        final ITerm warnings = MessageTerms.toTerms(messages.getWarnings(), solution.unifier());
        final ITerm notes = MessageTerms.toTerms(messages.getNotes(), solution.unifier());
        final ITerm resultTerm = B.newTuple(B.newBlob(result), errors, warnings, notes);
        return Optional.of(resultTerm);
    }

}