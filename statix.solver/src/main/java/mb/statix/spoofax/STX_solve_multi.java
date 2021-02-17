package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import mb.nabl2.terms.ITerm;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.IScopeImpl;
import mb.statix.concurrent.p_raffrayi.IUnitResult;
import mb.statix.concurrent.p_raffrayi.impl.Broker;
import mb.statix.concurrent.p_raffrayi.impl.ScopeImpl;
import mb.statix.concurrent.solver.GroupResult;
import mb.statix.concurrent.solver.IStatixProject;
import mb.statix.concurrent.solver.IStatixResult;
import mb.statix.concurrent.solver.ProjectResult;
import mb.statix.concurrent.solver.ProjectTypeChecker;
import mb.statix.concurrent.solver.UnitResult;
import mb.statix.scopegraph.terms.Scope;
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

        // TODO pass previous result from runtime
        final @Nullable IUnitResult<Scope, ITerm, ITerm, ProjectResult> previousResult = null;

        final List<ITerm> results = Lists.newArrayList();
        try {
            logger.info("Analyzing files");

            final double t0 = System.currentTimeMillis();
            final IFuture<IUnitResult<Scope, ITerm, ITerm, ProjectResult>> futureResult = Broker.run(project.resource(),
                    new ProjectTypeChecker(project, spec, debug), scopeImpl, spec.allLabels(), previousResult, cancel);

            final IUnitResult<Scope, ITerm, ITerm, ProjectResult> result = futureResult.asJavaCompletion().get();
            final double dt = System.currentTimeMillis() - t0;

            final List<IUnitResult<Scope, ITerm, ITerm, ?>> unitResults = new ArrayList<>();
            final Map<String, SolverResult> resultMap = flattenResult(spec, result, unitResults);

            //            PRaffrayiUtil.writeStatsCsvFromResult(unitResults, System.out);

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

    private Map<String, SolverResult> flattenResult(Spec spec, IUnitResult<Scope, ITerm, ITerm, ProjectResult> result,
            List<IUnitResult<Scope, ITerm, ITerm, ?>> unitResults) {
        unitResults.add(result);
        final Map<String, SolverResult> resourceResults = new HashMap<>();
        final ProjectResult projectResult = result.analysis();
        if(projectResult != null) {
            final List<SolverResult> groupResults = new ArrayList<>();
            projectResult.groupResults()
                    .forEach((k, gr) -> flattenGroupResult(spec, gr, groupResults, resourceResults, unitResults));
            projectResult.unitResults().forEach((k, ur) -> flattenUnitResult(spec, ur, resourceResults, unitResults));
            SolverResult solveResult = flatSolverResult(spec, result);
            for(SolverResult groupResult : groupResults) {
                solveResult = solveResult.combine(groupResult);
            }
            resourceResults.put(projectResult.resource(), solveResult);
        }
        return resourceResults;
    }

    private void flattenGroupResult(Spec spec, IUnitResult<Scope, ITerm, ITerm, GroupResult> result,
            List<SolverResult> groupResults, Map<String, SolverResult> resourceResults,
            List<IUnitResult<Scope, ITerm, ITerm, ?>> unitResults) {
        unitResults.add(result);
        final GroupResult groupResult = result.analysis();
        if(groupResult != null) {
            groupResult.groupResults()
                    .forEach((k, gr) -> flattenGroupResult(spec, gr, groupResults, resourceResults, unitResults));
            groupResult.unitResults().forEach((k, ur) -> flattenUnitResult(spec, ur, resourceResults, unitResults));
            final SolverResult solveResult = flatSolverResult(spec, result);
            groupResults.add(solveResult);
        }
    }

    private void flattenUnitResult(Spec spec, IUnitResult<Scope, ITerm, ITerm, UnitResult> result,
            Map<String, SolverResult> resourceResults, List<IUnitResult<Scope, ITerm, ITerm, ?>> unitResults) {
        unitResults.add(result);
        final UnitResult unitResult = result.analysis();
        if(unitResult != null) {
            final SolverResult solveResult = flatSolverResult(spec, result);
            resourceResults.put(unitResult.resource(), solveResult);
        }
    }

    private SolverResult flatSolverResult(Spec spec, IUnitResult<Scope, ITerm, ITerm, ? extends IStatixResult> result) {
        final IStatixResult unitResult = result.analysis();
        SolverResult solveResult = Optional.ofNullable(unitResult.solveResult()).orElseGet(() -> SolverResult.of(spec));
        solveResult = solveResult.withState(solveResult.state().withScopeGraph(result.scopeGraph()));
        return solveResult;
    }

}