package mb.statix.concurrent.solver;

import java.util.List;

import javax.annotation.Nullable;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import mb.nabl2.terms.ITerm;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.ITypeCheckerContext;
import mb.statix.concurrent.p_raffrayi.IUnitResult;
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

    @Override public IFuture<UnitResult> run(ITypeCheckerContext<Scope, ITerm, ITerm> context, List<Scope> rootScopes,
    		@Nullable IUnitResult<Scope, ITerm, ITerm, UnitResult> previousResult) {
        return runSolver(context, unit.rule(), rootScopes).handle((r, ex) -> {
            return UnitResult.of(unit.resource(), r, ex);
        }).whenComplete((r, ex) -> {
            logger.debug("unit {}: returned.", context.id());
        });
    }

}