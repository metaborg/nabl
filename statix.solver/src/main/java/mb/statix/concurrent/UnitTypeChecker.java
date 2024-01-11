package mb.statix.concurrent;

import java.util.List;
import java.util.function.Supplier;

import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.solver.tracer.SolverTracer;
import mb.statix.spec.Spec;
import mb.p_raffrayi.IIncrementalTypeCheckerContext;

public class UnitTypeChecker<TR extends SolverTracer.IResult<TR>> extends AbstractTypeChecker<UnitResult<TR>, TR> {

    private static final ILogger logger = LoggerUtils.logger(UnitTypeChecker.class);

    private final IStatixUnit unit;

    public UnitTypeChecker(IStatixUnit unit, Spec spec, IDebugContext debug, Supplier<SolverTracer<TR>> tracerFactory, int solverFlags) {
        super(spec, debug, tracerFactory, solverFlags);
        this.unit = unit;
    }

    @Override public IFuture<UnitResult<TR>> run(IIncrementalTypeCheckerContext<Scope, ITerm, ITerm, UnitResult<TR>, SolverState> context, List<Scope> rootScopes) {
        // @formatter:off
        return context.<SolverResult<TR>>runIncremental(
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
