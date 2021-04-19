package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

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
import mb.p_raffrayi.IScopeImpl;
import mb.p_raffrayi.IUnitResult;
import mb.p_raffrayi.impl.Broker;
import mb.statix.concurrent.GroupResult;
import mb.statix.concurrent.IStatixProject;
import mb.statix.concurrent.IStatixResult;
import mb.statix.concurrent.ProjectResult;
import mb.statix.concurrent.ProjectTypeChecker;
import mb.statix.concurrent.UnitResult;
import mb.statix.concurrent.nameresolution.ScopeImpl;
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
        super(STX_solve_multi.class.getSimpleName(), 4);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        final Spec spec =
                StatixTerms.spec().match(terms.get(0)).orElseThrow(() -> new InterpreterException("Expected spec."));
        reportOverlappingRules(spec);

        final IDebugContext debug = getDebugContext(terms.get(1));
        final IProgress progress = getProgress(terms.get(2));
        final ICancel cancel = getCancel(terms.get(3));

        final IStatixProject project =
                IStatixProject.matcher().match(term).orElseThrow(() -> new InterpreterException("Expected project."));

        final IScopeImpl<Scope, ITerm> scopeImpl = new ScopeImpl();

        final List<ITerm> results = Lists.newArrayList();
        try {
            logger.info("Analyzing files");

            final double t0 = System.currentTimeMillis();
            final IFuture<IUnitResult<Scope, ITerm, ITerm, ProjectResult>> futureResult = Broker.run(project.resource(),
                    new ProjectTypeChecker(project, spec, debug), scopeImpl, spec.allLabels(), cancel);

            final IUnitResult<Scope, ITerm, ITerm, ProjectResult> result = futureResult.asJavaCompletion().get();
            final double dt = System.currentTimeMillis() - t0;

            final Map<String, SolverResult> resultMap = flattenResult(spec, result);
            // PRaffrayiUtil.writeStatsCsvFromResult(result, System.out);

            logger.info("Files analyzed in {} s", (dt / 1_000d));

            for(Entry<String, SolverResult> entry : resultMap.entrySet()) {
                results.add(B.newTuple(B.newString(entry.getKey()), B.newBlob(entry.getValue())));
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

    private Map<String, SolverResult> flattenResult(Spec spec, IUnitResult<Scope, ITerm, ITerm, ProjectResult> result) {
        final Map<String, SolverResult> resourceResults = new HashMap<>();
        final ProjectResult projectResult = result.analysis();
        if(projectResult != null) {
            final List<SolverResult> groupResults = new ArrayList<>();
            projectResult.libraryResults().forEach((k, ur) -> flattenLibraryResult(spec, ur));
            projectResult.groupResults()
                    .forEach((k, gr) -> flattenGroupResult(spec, gr, groupResults, resourceResults));
            projectResult.unitResults().forEach((k, ur) -> flattenUnitResult(spec, ur, resourceResults));
            SolverResult solveResult = flatSolverResult(spec, result);
            for(SolverResult groupResult : groupResults) {
                solveResult = solveResult.combine(groupResult);
            }
            resourceResults.put(projectResult.resource(), solveResult);
        } else {
            logger.error("Missing result for project {}", result.id());
        }
        return resourceResults;
    }

    private void flattenLibraryResult(Spec spec, IUnitResult<Scope, ITerm, ITerm, Unit> result) {
    }

    private void flattenGroupResult(Spec spec, IUnitResult<Scope, ITerm, ITerm, GroupResult> result,
            List<SolverResult> groupResults, Map<String, SolverResult> resourceResults) {
        final GroupResult groupResult = result.analysis();
        if(groupResult != null) {
            groupResult.groupResults().forEach((k, gr) -> flattenGroupResult(spec, gr, groupResults, resourceResults));
            groupResult.unitResults().forEach((k, ur) -> flattenUnitResult(spec, ur, resourceResults));
            final SolverResult solveResult = flatSolverResult(spec, result);
            groupResults.add(solveResult);
        } else {
            logger.error("Missing result for group {}", result.id());
        }
    }

    private void flattenUnitResult(Spec spec, IUnitResult<Scope, ITerm, ITerm, UnitResult> result,
            Map<String, SolverResult> resourceResults) {
        final UnitResult unitResult = result.analysis();
        if(unitResult != null) {
            final SolverResult solveResult = flatSolverResult(spec, result);
            resourceResults.put(unitResult.resource(), solveResult);
        } else {
            logger.error("Missing result for unit {}", result.id());
        }
    }

    private SolverResult flatSolverResult(Spec spec, IUnitResult<Scope, ITerm, ITerm, ? extends IStatixResult> result) {
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

}