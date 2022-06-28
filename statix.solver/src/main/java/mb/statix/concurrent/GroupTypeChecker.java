package mb.statix.concurrent;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.metaborg.util.future.AggregateFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import mb.nabl2.terms.ITerm;
import mb.p_raffrayi.IUnitResult;
import mb.p_raffrayi.impl.Result;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.tracer.SolverTracer;
import mb.statix.spec.Spec;
import mb.p_raffrayi.IIncrementalTypeCheckerContext;

public class GroupTypeChecker<TR extends SolverTracer.IResult<TR>> extends AbstractTypeChecker<GroupResult<TR>, TR> {

    private static final ILogger logger = LoggerUtils.logger(GroupTypeChecker.class);

    private final IStatixGroup group;

    public GroupTypeChecker(IStatixGroup group, Spec spec, IDebugContext debug, Supplier<SolverTracer<TR>> tracerFactory) {
        super(spec, debug, tracerFactory);
        this.group = group;
    }

    @Override public IFuture<GroupResult<TR>> run(IIncrementalTypeCheckerContext<Scope, ITerm, ITerm, GroupResult<TR>, SolverState> context,
            List<Scope> rootScopes) {

        final List<Scope> thisGroupScopes = group.scopeNames().stream().map(name -> makeSharedScope(context, name)).collect(Collectors.toList());
        final IFuture<Map<String, IUnitResult<Scope, ITerm, ITerm, Result<Scope, ITerm, ITerm, GroupResult<TR>, SolverState>>>> groupResults =
            runGroups(context, group.groups(), thisGroupScopes);
        final IFuture<Map<String, IUnitResult<Scope, ITerm, ITerm, Result<Scope, ITerm, ITerm, UnitResult<TR>, SolverState>>>> unitResults =
            runUnits(context, group.units(), thisGroupScopes);
        thisGroupScopes.forEach(context::closeScope);

        // @formatter:off
        return context.runIncremental(
            initialState -> {
                logger.debug("group {}: running. restarted: {}.", group.resource(), initialState.isPresent());
                return runSolver(context, group.rule(), initialState, ImmutableList.copyOf(Iterables.concat(rootScopes, thisGroupScopes)));
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
