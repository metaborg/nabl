package mb.statix.concurrent.solver;

import java.util.Arrays;
import java.util.List;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import mb.nabl2.terms.ITerm;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.IIncrementalTypeCheckerContext;
import mb.statix.concurrent.p_raffrayi.impl.IInitialState;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.Spec;

public class UnitTypeChecker extends AbstractTypeChecker<UnitResult> {

    private static final ILogger logger = LoggerUtils.logger(UnitTypeChecker.class);

    private final IStatixUnit unit;

    public UnitTypeChecker(IStatixUnit unit, Spec spec, IDebugContext debug) {
        super(spec, debug);
        this.unit = unit;
    }

    @Override public IFuture<UnitResult> run(IIncrementalTypeCheckerContext<Scope, ITerm, ITerm, UnitResult> context, List<Scope> rootScopes,
            IInitialState<Scope, ITerm, ITerm, UnitResult> initialState) {
        // @formatter:off
        return context.runIncremental(
            restarted -> {
                return runSolver(context, unit.rule(), rootScopes);
            },
            UnitResult::solveResult,
            (result, ex) -> {
                return CompletableFuture.completedFuture(UnitResult.of(unit.resource(), result, ex));
            })
            .whenComplete((r, ex) -> {
                logger.debug("unit {}: returned.", context.id());
            });
        // @formatter:on
    }

}