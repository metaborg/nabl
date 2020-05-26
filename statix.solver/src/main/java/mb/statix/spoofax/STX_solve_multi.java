package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.metaborg.util.functions.Function2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.util.Tuple2;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.solver.concurrent.Coordinator;
import mb.statix.solver.concurrent.StatixTypeChecker;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.Spec;

public class STX_solve_multi extends StatixPrimitive {
    private static final ILogger logger = LoggerUtils.logger(STX_solve_multi.class);

    @Inject public STX_solve_multi() {
        super(STX_solve_multi.class.getSimpleName(), 3);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {

        final Spec spec =
                StatixTerms.spec().match(terms.get(0)).orElseThrow(() -> new InterpreterException("Expected spec."));
        reportOverlappingRules(spec);

        final IDebugContext debug = getDebugContext(terms.get(1));

        final IMatcher<Tuple2<String, IConstraint>> constraintMatcher =
                M.tuple2(M.stringValue(), StatixTerms.constraint(), (t, r, c) -> Tuple2.of(r, c));
        final List<Tuple2<String, IConstraint>> constraints = M.listElems(constraintMatcher).match(term)
                .orElseThrow(() -> new InterpreterException("Expected list of constraints."));

        final ExecutorService executor = Executors.newCachedThreadPool();
        final Function2<String, Integer, Scope> newScope = (resource, n) -> Scope.of(resource, n.toString());
        final Coordinator<Scope, ITerm, ITerm> solver = new Coordinator<>(newScope);

        final double t0 = System.currentTimeMillis();

        final Map<String, CompletableFuture<Object>> fileSolvers = Maps.newHashMap();
        for(Tuple2<String, IConstraint> resource_constraint : constraints) {
            final String resource = resource_constraint._1();
            final StatixTypeChecker fileSolver =
                    new StatixTypeChecker(resource, solver, spec, resource_constraint._2(), debug);
            fileSolvers.put(resource, fileSolver.run(executor));
        }
        final CompletableFuture<Object> solveResult = solver.run(executor);

        try {
            final List<CompletableFuture<?>> pendingResults = Lists.newArrayList(fileSolvers.values());
            pendingResults.add(solveResult);
            CompletableFuture.allOf(pendingResults.toArray(new CompletableFuture<?>[pendingResults.size()])).get();
        } catch(InterruptedException | ExecutionException e) {
            throw new InterpreterException(e);
        }

        final double dt = System.currentTimeMillis() - t0;
        logger.info("Files analyzed in {} s", (dt / 1_000d));

        // TODO Collect and combine results
        final List<ITerm> results = ImmutableList.of();
        return Optional.of(B.newList(results));
    }

}