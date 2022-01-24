package mb.statix.concurrent;

import java.util.List;

import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.Spec;
import mb.p_raffrayi.IIncrementalTypeCheckerContext;

public class UnitTypeChecker extends AbstractTypeChecker<UnitResult> {

    private static final ILogger logger = LoggerUtils.logger(UnitTypeChecker.class);

    private final IStatixUnit unit;

    public UnitTypeChecker(IStatixUnit unit, Spec spec, IDebugContext debug) {
        super(spec, debug);
        this.unit = unit;
    }

    @Override public IFuture<UnitResult> run(IIncrementalTypeCheckerContext<Scope, ITerm, ITerm, UnitResult, SolverState> context, List<Scope> rootScopes) {
        // @formatter:off
        return context.runIncremental(
            initialState -> {
                logger.debug("unit {}: running. restarted: {}.", context.id(), initialState.isPresent());
                return runSolver(context, unit.rule(), initialState, rootScopes);
            },
            UnitResult::solveResult,
            this::patch,
            (result, ex) -> {
                logger.debug("unit {}: building final result.", context.id());
                return CompletableFuture.completedFuture(UnitResult.of(unit.resource(), result, ex));
            })
            .whenComplete((r, ex) -> {
                logger.debug("unit {}: returned.", context.id());
            });
        // @formatter:on
    }

}
