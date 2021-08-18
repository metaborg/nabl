package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.metaborg.util.future.IFuture;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;
import org.metaborg.util.unit.Unit;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import mb.nabl2.terms.ITerm;
import mb.p_raffrayi.IResult;
import mb.p_raffrayi.IScopeImpl;
import mb.p_raffrayi.IUnitResult;
import mb.p_raffrayi.IUnitResult.TransitionTrace;
import mb.p_raffrayi.PRaffrayiSettings;
import mb.p_raffrayi.APRaffrayiSettings.ConfirmationMode;
import mb.p_raffrayi.impl.Broker;
import mb.statix.concurrent.GroupResult;
import mb.statix.concurrent.IStatixProject;
import mb.statix.concurrent.IStatixResult;
import mb.statix.concurrent.ProjectResult;
import mb.statix.concurrent.ProjectTypeChecker;
import mb.statix.concurrent.SolverState;
import mb.statix.concurrent.UnitResult;
import mb.statix.concurrent.nameresolution.ScopeImpl;
import mb.statix.concurrent.InputMatchers;
import mb.statix.constraints.CFalse;
import mb.statix.constraints.messages.IMessage;
import mb.statix.constraints.messages.Message;
import mb.statix.constraints.messages.MessageKind;
import mb.statix.constraints.messages.TextPart;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.spec.Spec;

public class STX_solve_multi extends StatixPrimitive {
    private static final ILogger logger = LoggerUtils.logger(STX_solve_multi.class);

    @Inject public STX_solve_multi() {
        super(STX_solve_multi.class.getSimpleName(), 5);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        final Spec spec =
                StatixTerms.spec().match(terms.get(1)).orElseThrow(() -> new InterpreterException("Expected spec."));
        reportOverlappingRules(spec);

        final IDebugContext debug = getDebugContext(terms.get(2));
        final IProgress progress = getProgress(terms.get(3));
        final ICancel cancel = getCancel(terms.get(4));

        final IStatixProject project =
                InputMatchers.project().match(term).orElseThrow(() -> new InterpreterException("Expected project."));

        final IScopeImpl<Scope, ITerm> scopeImpl = new ScopeImpl();

        final List<ITerm> results = Lists.newArrayList();
        try {
            logger.info("Analyzing files");

            final SolverMode solverMode = getSolverMode(terms.get(0));
            final PRaffrayiSettings settings = solverModeToSettings(solverMode);

            final double t0 = System.currentTimeMillis();

            final IFuture<IUnitResult<Scope, ITerm, ITerm, ProjectResult, SolverState>> futureResult =
                    Broker.run(project.resource(), settings, new ProjectTypeChecker(project, spec, debug), scopeImpl,
                            spec.allLabels(), project.changed(), project.previousResult(), cancel);

            final IUnitResult<Scope, ITerm, ITerm, ProjectResult, SolverState> result =
                    futureResult.asJavaCompletion().get();
            final double dt = System.currentTimeMillis() - t0;

            final List<IUnitResult<Scope, ITerm, ITerm, ?, SolverState>> unitResults = new ArrayList<>();
            final Map<String, ITerm> resultMap = flattenResult(spec, result, unitResults);
            // PRaffrayiUtil.writeStatsCsvFromResult(result, System.out);

            logger.info("Files analyzed in {} s", (dt / 1_000d));
            if(settings.incremental()) {
                logger.info("* Initially changed units : {}",
                        flattenTransitions(unitResults, TransitionTrace.INITIALLY_STARTED));
                logger.info("* Restarted units         : {}",
                        flattenTransitions(unitResults, TransitionTrace.RESTARTED));
                logger.info("* Released units          : {}",
                        flattenTransitions(unitResults, TransitionTrace.RELEASED));
            }

            for(Entry<String, ITerm> entry : resultMap.entrySet()) {
                results.add(B.newTuple(B.newString(entry.getKey()), entry.getValue()));
            }
        } catch(InterruptedException ie) {
            logger.info("Async solving interrupted");
        } catch(ExecutionException ee) {
            Throwable c = ee.getCause();
            if(c instanceof InterruptedException) {
                logger.info("Async solving interrupted");
            } else {
                logger.error("Async solving failed.", c);
                throw new InterpreterException("Async solving failed."/*, c*/);
            }
        } catch(Throwable e) {
            logger.error("Async solving failed.", e);
            throw new InterpreterException("Async solving failed."/*, e*/);
        }
        return Optional.of(B.newList(results));
    }

    private Map<String, ITerm> flattenResult(Spec spec,
            IUnitResult<Scope, ITerm, ITerm, ProjectResult, SolverState> result,
            List<IUnitResult<Scope, ITerm, ITerm, ?, SolverState>> unitResults) {
        unitResults.add(result);
        final Map<String, ITerm> resourceResults = new HashMap<>();
        final @Nullable ProjectResult projectResult = result.analysis();
        if(projectResult != null) {
            final String resource = projectResult.resource();
            final List<SolverResult> groupResults = new ArrayList<>();
            projectResult.libraryResults().forEach((k, ur) -> flattenLibraryResult(spec, ur));
            projectResult.groupResults().forEach((k, gr) -> flattenGroupResult(spec, resource + "/" + k, gr,
                    groupResults, resourceResults, unitResults));
            projectResult.unitResults().forEach((k, ur) -> flattenUnitResult(spec, ur, resourceResults, unitResults));

            SolverResult solveResult = flatSolverResult(spec, result);
            for(SolverResult groupResult : groupResults) {
                solveResult = solveResult.combine(groupResult);
            }
            resourceResults.put(resource, B.newAppl("ProjectResult", B.newBlob(solveResult), B.newBlob(result)));
        } else {
            logger.error("Missing result for project {}", result.id());
        }
        return resourceResults;
    }

    private void flattenLibraryResult(Spec spec, IUnitResult<Scope, ITerm, ITerm, IResult.Empty<Scope, ITerm, ITerm>, Unit> result) {
    }

    private void flattenGroupResult(Spec spec, String groupId,
            IUnitResult<Scope, ITerm, ITerm, GroupResult, SolverState> result, List<SolverResult> groupResults,
            Map<String, ITerm> resourceResults, List<IUnitResult<Scope, ITerm, ITerm, ?, SolverState>> unitResults) {
        unitResults.add(result);
        final GroupResult groupResult = result.analysis();
        if(groupResult != null) {
            groupResult.groupResults()
                    .forEach((k, gr) -> flattenGroupResult(spec, groupResult.resource(),
                            (IUnitResult<Scope, ITerm, ITerm, GroupResult, SolverState>) gr, groupResults,
                            resourceResults, unitResults));
            groupResult.unitResults().forEach((k, ur) -> flattenUnitResult(spec, ur, resourceResults, unitResults));
            final SolverResult solveResult = flatSolverResult(spec, result);
            groupResults.add(solveResult);
            resourceResults.put(groupId, B.newAppl("GroupResult", B.newBlob(solveResult), B.newBlob(result)));
        } else {
            logger.error("Missing result for group {}", result.id());
        }
    }

    private void flattenUnitResult(Spec spec, IUnitResult<Scope, ITerm, ITerm, UnitResult, SolverState> result,
            Map<String, ITerm> resourceResults, List<IUnitResult<Scope, ITerm, ITerm, ?, SolverState>> unitResults) {
        unitResults.add(result);
        final UnitResult unitResult = result.analysis();
        if(unitResult != null) {
            final SolverResult solveResult = flatSolverResult(spec, result);
            resourceResults.put(unitResult.resource(),
                    B.newAppl("FileResult", B.newBlob(solveResult), B.newBlob(result)));
        } else {
            logger.error("Missing result for unit {}", result.id());
        }
    }

    private SolverResult flatSolverResult(Spec spec,
            IUnitResult<Scope, ITerm, ITerm, ? extends IStatixResult, SolverState> result) {
        final IStatixResult unitResult = result.analysis();
        SolverResult solveResult = Optional.ofNullable(unitResult.solveResult()).orElseGet(() -> SolverResult.of(spec));

        solveResult = solveResult.withState(solveResult.state().withScopeGraph(result.scopeGraph()));

        final ImmutableMap.Builder<IConstraint, IMessage> messages =
                ImmutableMap.<IConstraint, IMessage>builder().putAll(solveResult.messages());
        if(result.analysis().exception() != null) {
            final Message message = new Message(MessageKind.ERROR,
                    ImmutableList.of(new TextPart("Exception: " + result.analysis().exception().getMessage())),
                    B.newTuple());
            messages.put(new CFalse(message), message);
        }
        for(Throwable failure : result.failures()) {
            final Message message = new Message(MessageKind.ERROR,
                    ImmutableList.of(new TextPart("Exception: " + failure.getMessage())), B.newTuple());
            messages.put(new CFalse(message), message);
        }
        solveResult = solveResult.withMessages(messages.build());

        return solveResult;
    }

    private String flattenTransitions(List<IUnitResult<Scope, ITerm, ITerm, ?, SolverState>> unitResults,
            TransitionTrace flow) {
        return unitResults.stream().filter(r -> r.stateTransitionTrace() == flow).map(IUnitResult::id)
                .collect(Collectors.joining(", "));
    }

    private SolverMode getSolverMode(ITerm term) throws InterpreterException {
        return M.blobValue(SolverMode.class).match(term)
                .orElseThrow(() -> new InterpreterException("Expected project condiguration, got " + term));
    }

    private PRaffrayiSettings solverModeToSettings(SolverMode mode) throws InterpreterException {
        if(!mode.concurrent) {
            throw new InterpreterException("Cannot create concurrent settings for solver mode " + mode);
        }
        // @formatter:off
        return PRaffrayiSettings.builder()
            .incrementalDeadlock(mode.deadlock)
            .scopeGraphDiff(mode.sgDiff)
            .confirmationMode(mode.confirmation ? ConfirmationMode.SIMPLE_ENVIRONMENT : ConfirmationMode.TRIVIAL)
            .build();
        // @formatter:on
    }

}