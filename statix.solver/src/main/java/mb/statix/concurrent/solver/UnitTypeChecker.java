package mb.statix.concurrent.solver;

import mb.nabl2.terms.ITerm;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.ITypeCheckerContext;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.Spec;

public class UnitTypeChecker extends AbstractTypeChecker<UnitResult> {

    private final IStatixUnit unit;

    public UnitTypeChecker(IStatixUnit unit, Spec spec, IDebugContext debug) {
        super(spec, debug);
        this.unit = unit;
    }

    @Override public IFuture<UnitResult> run(ITypeCheckerContext<Scope, ITerm, ITerm> context, Scope root) {
        return runSolver(context, unit.rule(), root).handle((r, ex) -> {
            return UnitResult.of(unit.resource(), r, ex);
        });
    }

}