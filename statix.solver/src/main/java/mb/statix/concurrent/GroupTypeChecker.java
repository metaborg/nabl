package mb.statix.concurrent;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.metaborg.util.future.AggregateFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import mb.nabl2.terms.ITerm;
import mb.p_raffrayi.IUnitResult;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.Spec;
import mb.p_raffrayi.IIncrementalTypeCheckerContext;

public class GroupTypeChecker extends AbstractTypeChecker<GroupResult> {

    private static final ILogger logger = LoggerUtils.logger(GroupTypeChecker.class);

    private final IStatixGroup group;

    public GroupTypeChecker(IStatixGroup group, Spec spec, IDebugContext debug) {
        super(spec, debug);
        this.group = group;
    }

    @Override public IFuture<GroupResult> run(IIncrementalTypeCheckerContext<Scope, ITerm, ITerm, GroupResult, SolverState> context,
            List<Scope> rootScopes) {
        final Scope parentScope = rootScopes.get(0);
        final Scope thisGroupScope = makeSharedScope(context, "s_grp");
        final IFuture<Map<String, IUnitResult<Scope, ITerm, ITerm, GroupResult, SolverState>>> groupResults =
            runGroups(context, group.groups(), thisGroupScope);
        final IFuture<Map<String, IUnitResult<Scope, ITerm, ITerm, UnitResult, SolverState>>> unitResults =
            runUnits(context, group.units(), thisGroupScope);
        context.closeScope(thisGroupScope);

        // @formatter:off
        return context.runIncremental(
            initialState -> {
                logger.debug("group {}: running. restarted: {}.", group.resource(), initialState.isPresent());
                return runSolver(context, group.rule(), initialState, Arrays.asList(parentScope, thisGroupScope));
            },
            GroupResult::solveResult,
            this::patch,
            (result, ex) -> {
                logger.debug("group {}: combining.", group.resource());
                return AggregateFuture.apply(groupResults, unitResults).thenApply(e -> {
                    logger.debug("group {}: returning.", group.resource());
                    return GroupResult.of(group.resource(), e._1(), e._2(), result, ex);
                });
            })
            .whenComplete((r, __) -> {
                logger.debug("group {}: returned.", context.id());
            });
        // @formatter:on
    }

}