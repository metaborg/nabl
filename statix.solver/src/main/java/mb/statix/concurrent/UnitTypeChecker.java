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
import mb.p_raffrayi.impl.IInitialState;

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