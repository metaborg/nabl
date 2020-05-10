package mb.nabl2.spoofax.primitives;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;
import org.metaborg.util.time.AggregateTimer;
import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.constraints.messages.MessageContent;
import mb.nabl2.constraints.messages.MessageInfo;
import mb.nabl2.constraints.messages.MessageKind;
import mb.nabl2.scopegraph.esop.bottomup.BUNameResolution;
import mb.nabl2.scopegraph.path.IResolutionPath;
import mb.nabl2.scopegraph.terms.Label;
import mb.nabl2.scopegraph.terms.Occurrence;
import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.solver.Fresh;
import mb.nabl2.solver.ISolution;
import mb.nabl2.solver.exceptions.SolverException;
import mb.nabl2.solver.messages.IMessages;
import mb.nabl2.solver.solvers.SemiIncrementalMultiFileSolver;
import mb.nabl2.spoofax.analysis.Actions;
import mb.nabl2.spoofax.analysis.IResult;
import mb.nabl2.spoofax.analysis.MultiFinalResult;
import mb.nabl2.spoofax.analysis.MultiInitialResult;
import mb.nabl2.spoofax.analysis.MultiUnitResult;
import mb.nabl2.stratego.MessageTerms;
import mb.nabl2.terms.ITerm;
import mb.nabl2.util.Tuple2;

public class SG_solve_multi_final_constraint extends ScopeGraphMultiFileAnalysisPrimitive {

    @SuppressWarnings("unused") private static ILogger logger =
            LoggerUtils.logger(SG_solve_multi_final_constraint.class);

    public SG_solve_multi_final_constraint() {
        super(SG_solve_multi_final_constraint.class.getSimpleName(), 0);
    }

    @Override protected Optional<? extends ITerm> call(ITerm currentTerm, List<ITerm> argTerms,
            SemiIncrementalMultiFileSolver solver, ICancel cancel, IProgress progress) throws InterpreterException {
        final Tuple2<MultiInitialResult, List<MultiUnitResult>> input = M.tuple2(M.blobValue(MultiInitialResult.class),
                M.listElems(M.blobValue(MultiUnitResult.class)), (t, ir, urs) -> {
                    return Tuple2.of(ir, urs);
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
                    MessageInfo.of(MessageKind.ERROR, MessageContent.of(), Actions.sourceTerm(""));
            ISolution preSolution =
                    solver.solveInter(initialSolution, unitSolutions, defaultMessage, fresh, cancel, progress);
            solution = preSolution;
        } catch(InterruptedException | SolverException ex) {
            throw new InterpreterException(ex);
        }

        {
            logger.info("resolving with bottom-up resolution");
            AggregateTimer timer = new AggregateTimer(false);
            try {
                final BUNameResolution<Scope, Label, Occurrence> nameResolution =
                        BUNameResolution.of(solution.scopeGraph(), solution.config().getResolutionParams());
                timer.start();
                logger.info("verifying {} resolve entries", solution.nameResolutionCache().resolutionEntries().size());
                for(Entry<Occurrence, Collection<IResolutionPath<Scope, Label, Occurrence>>> entry : solution
                        .nameResolutionCache().resolutionEntries().entrySet()) {
                    final Collection<IResolutionPath<Scope, Label, Occurrence>> result =
                            nameResolution.resolve(entry.getKey());
                    if(result.size() != entry.getValue().size()) {
                        logger.warn("resolve {}, got {}, expected {}", entry.getKey(), result.size(),
                                entry.getValue().size());
                    }
                }
                logger.info("verifying {} visible entries", solution.nameResolutionCache().visibilityEntries().size());
                for(Entry<Scope, Collection<Occurrence>> entry : solution.nameResolutionCache().visibilityEntries()
                        .entrySet()) {
                    final Collection<Occurrence> result = nameResolution.visible(entry.getKey());
                    if(result.size() != entry.getValue().size()) {
                        logger.warn("visible {}, got {}, expected {}", entry.getKey(), result.size(),
                                entry.getValue().size());
                    }
                }
                logger.info("verifying {} reachable entries",
                        solution.nameResolutionCache().reachabilityEntries().size());
                for(Entry<Scope, Collection<Occurrence>> entry : solution.nameResolutionCache().reachabilityEntries()
                        .entrySet()) {
                    final Collection<Occurrence> result = nameResolution.reachable(entry.getKey());
                    if(result.size() != entry.getValue().size()) {
                        logger.warn("reachable {}, got {}, expected {}", entry.getKey(), result.size(),
                                entry.getValue().size());
                    }
                }
            } catch(Exception e) {
                logger.error("bottom-up resolution failed", e);
            } finally {
                timer.stop();
            }
            logger.info("bottom-up resolution took {} s",
                    TimeUnit.SECONDS.convert(timer.total(), TimeUnit.NANOSECONDS));
        }

        final List<IConstraint> constraints = Stream.concat(initialResult.constraints().stream(),
                unitResults.stream().flatMap(ur -> ur.constraints().stream())).collect(Collectors.toList());
        final IResult result = MultiFinalResult.of(constraints, solution, Optional.empty(), globalFresh.freeze());
        final IMessages.Immutable messages = solution.messagesAndUnsolvedErrors();
        final ITerm errors = MessageTerms.toTerms(messages.getErrors(), solution.unifier());
        final ITerm warnings = MessageTerms.toTerms(messages.getWarnings(), solution.unifier());
        final ITerm notes = MessageTerms.toTerms(messages.getNotes(), solution.unifier());
        final ITerm resultTerm = B.newTuple(B.newBlob(result), errors, warnings, notes);
        return Optional.of(resultTerm);
    }

}