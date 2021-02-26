package mb.statix.concurrent.solver;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import mb.nabl2.terms.ITerm;
import mb.statix.concurrent.actors.futures.AggregateFuture;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.ITypeCheckerContext;
import mb.statix.concurrent.p_raffrayi.IUnitResult;
import mb.statix.concurrent.p_raffrayi.impl.IInitialState;
import mb.statix.scopegraph.terms.Scope;
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
            List<Scope> rootScopes, IInitialState<Scope, ITerm, ITerm, GroupResult> initialState) {
        final Scope projectScope = rootScopes.get(0);
        final Scope parentGrpScope = rootScopes.get(1);
        final Scope thisGroupScope = makeSharedScope(context, "s_grp");
        final IFuture<Map<String, IUnitResult<Scope, ITerm, ITerm, GroupResult>>> groupResults =
                runGroups(context, group.groups(), projectScope, thisGroupScope, initialState);
        final IFuture<Map<String, IUnitResult<Scope, ITerm, ITerm, UnitResult>>> unitResults =
                runUnits(context, group.units(), projectScope, thisGroupScope, initialState);
        context.closeScope(thisGroupScope);

        final IFuture<SolverResult> result = context.confirmQueries().thenCompose(v -> {
            return v ? CompletableFuture.completedFuture(initialState.previousResult().get().analysis().solveResult())
                : runSolver(context, group.rule(), Arrays.asList(projectScope, parentGrpScope, thisGroupScope));
        });

        return AggregateFuture.apply(groupResults, unitResults, result).thenApply(e -> {
            return GroupResult.of(e._1(), e._2(), e._3(), null);
        }).whenComplete((r, ex) -> {
            logger.debug("group {}: returned.", context.id());
        });
    }

}