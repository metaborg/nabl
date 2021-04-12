package mb.statix.concurrent.solver;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.metaborg.util.future.AggregateFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import mb.nabl2.terms.ITerm;
import mb.statix.concurrent.p_raffrayi.ITypeCheckerContext;
import mb.statix.concurrent.p_raffrayi.IUnitResult;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.spec.Spec;

public class GroupTypeChecker extends AbstractTypeChecker<GroupResult> {

    private static final ILogger logger = LoggerUtils.logger(GroupTypeChecker.class);

    private final IStatixGroup group;

    public GroupTypeChecker(IStatixGroup group, Spec spec, IDebugContext debug) {
        super(spec, debug);
        this.group = group;
    }

    @Override public IFuture<GroupResult> run(ITypeCheckerContext<Scope, ITerm, ITerm> context,
            List<Scope> rootScopes) {
        final Scope parentScope = rootScopes.get(0);
        final Scope thisGroupScope = makeSharedScope(context, "s_grp");
        final IFuture<Map<String, IUnitResult<Scope, ITerm, ITerm, GroupResult>>> groupResults =
                runGroups(context, group.groups(), thisGroupScope);
        final IFuture<Map<String, IUnitResult<Scope, ITerm, ITerm, UnitResult>>> unitResults =
                runUnits(context, group.units(), thisGroupScope);
        context.closeScope(thisGroupScope);
        final IFuture<SolverResult> result =
                runSolver(context, group.rule(), Arrays.asList(parentScope, thisGroupScope));
        return AggregateFuture.apply(groupResults, unitResults, result).thenApply(e -> {
            return GroupResult.of(e._1(), e._2(), e._3(), null);
        }).whenComplete((r, ex) -> {
            logger.debug("group {}: returned.", context.id());
        });
    }

}