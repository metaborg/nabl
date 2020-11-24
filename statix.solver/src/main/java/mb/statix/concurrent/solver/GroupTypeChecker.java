package mb.statix.concurrent.solver;

import java.util.Map;

import mb.nabl2.terms.ITerm;
import mb.statix.concurrent.actors.futures.AggregateFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.ITypeCheckerContext;
import mb.statix.concurrent.p_raffrayi.IUnitResult;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.spec.Spec;

public class GroupTypeChecker extends AbstractTypeChecker<GroupResult> {

    private final IStatixGroup group;

    public GroupTypeChecker(IStatixGroup group, Spec spec, IDebugContext debug) {
        super(spec, debug);
        this.group = group;
    }

    @Override public IFuture<GroupResult> run(ITypeCheckerContext<Scope, ITerm, ITerm> context, Scope root) {
        final Scope groupScope = makeSharedScope(context, "s_grp");
        final IFuture<Map<String, IUnitResult<Scope, ITerm, ITerm, GroupResult>>> groupResults =
                runGroups(context, group.groups(), groupScope);
        final IFuture<Map<String, IUnitResult<Scope, ITerm, ITerm, UnitResult>>> unitResults =
                runUnits(context, group.units(), groupScope);
        context.closeScope(groupScope);
        final IFuture<SolverResult> result = runSolver(context, group.rule(), root, groupScope);
        return AggregateFuture.apply(groupResults, unitResults, result).thenApply(e -> {
            return GroupResult.of(e._1(), e._2(), e._3(), null);
        });
    }

}