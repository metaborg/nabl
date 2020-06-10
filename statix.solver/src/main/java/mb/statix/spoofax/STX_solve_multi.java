package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.util.Tuple2;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.solver.concurrent.Coordinator;
import mb.statix.solver.concurrent.CoordinatorResult;
import mb.statix.solver.concurrent.ScopeImpl;
import mb.statix.solver.concurrent.StatixTypeChecker;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LoggerDebugContext;
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

        final IDebugContext debug = new LoggerDebugContext(logger); // getDebugContext(terms.get(1));
        final IProgress progress = getProgress(terms.get(2));
        final ICancel cancel = getCancel(terms.get(3));

        final IMatcher<Tuple2<String, IConstraint>> constraintMatcher =
                M.tuple2(M.stringValue(), StatixTerms.constraint(), (t, r, c) -> Tuple2.of(r, c));
        final List<Tuple2<String, IConstraint>> constraints = M.listElems(constraintMatcher).match(term)
                .orElseThrow(() -> new InterpreterException("Expected list of constraints."));

        final Scope root = Scope.of("", "0");
        final ExecutorService executor = Executors.newCachedThreadPool();
        final ScopeImpl<Scope> scopeImpl = new ScopeImpl<Scope>() {
            // @formatter:off
            @Override public Scope make(String resource, String name) { return Scope.of(resource, name); }
            @Override public String resource(Scope scope) { return scope.getResource(); }
            // @formatter:on
        };
        final Coordinator<Scope, ITerm, ITerm> solver = new Coordinator<>(root, spec.allLabels(), scopeImpl, cancel);

        final double t0 = System.currentTimeMillis();

        final Map<String, CompletableFuture<SolverResult>> fileSolvers = Maps.newHashMap();
        for(Tuple2<String, IConstraint> resource_constraint : constraints) {
            final String resource = resource_constraint._1();
            final StatixTypeChecker fileSolver =
                    new StatixTypeChecker(resource, solver, spec, resource_constraint._2(), debug, progress, cancel);
            fileSolvers.put(resource, fileSolver.run(executor));
        }
        final CompletableFuture<CoordinatorResult<Scope, ITerm, ITerm>> solveResult = solver.run(executor);

        final List<ITerm> results = Lists.newArrayList();
        try {
            final List<CompletableFuture<?>> pendingResults = Lists.newArrayList(fileSolvers.values());
            pendingResults.add(solveResult);
            CompletableFuture.allOf(pendingResults.toArray(new CompletableFuture<?>[pendingResults.size()])).get();

            final CoordinatorResult<Scope, ITerm, ITerm> coordinatorResult = solveResult.get();

            final double dt = System.currentTimeMillis() - t0;
            logger.info("Files analyzed in {} s", (dt / 1_000d));

            for(Entry<String, CompletableFuture<SolverResult>> entry : fileSolvers.entrySet()) {
                final SolverResult fileResult = entry.getValue().get();
                final SolverResult updatedFileResult =
                        fileResult.withState(fileResult.state().withScopeGraph(coordinatorResult.scopeGraph()));
                results.add(B.newTuple(B.newString(entry.getKey()), B.newBlob(updatedFileResult)));
            }
        } catch(Throwable e) {
            executor.shutdownNow();
            logger.error("Async solving failed.", e);
            throw new InterpreterException("Async solving failed.", e);
        }

        return Optional.of(B.newList(results));
    }

}