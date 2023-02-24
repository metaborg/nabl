package mb.statix.concurrent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.util.collection.ImList;
import org.metaborg.util.future.AggregateFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import mb.nabl2.terms.ITerm;
import mb.p_raffrayi.IUnitResult;
import mb.p_raffrayi.impl.Result;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.Rule;
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

        final List<Scope> thisGroupScopes = group.scopeNames().stream().map(name -> makeSharedScope(context, name)).collect(Collectors.toList());
        final IFuture<Map<String, IUnitResult<Scope, ITerm, ITerm, Result<Scope, ITerm, ITerm, GroupResult, SolverState>>>> groupResults =
            runGroups(context, group.groups(), thisGroupScopes);
        final IFuture<Map<String, IUnitResult<Scope, ITerm, ITerm, Result<Scope, ITerm, ITerm, UnitResult, SolverState>>>> unitResults =
            runUnits(context, group.units(), thisGroupScopes);
        thisGroupScopes.forEach(context::closeScope);

        final Optional<Rule> rule = group.rule();
        final String resource = group.resource();

        final ImList.Transient<Scope> scopesBuilder = new ImList.Transient<>(rootScopes.size() + thisGroupScopes.size());
        scopesBuilder.addAll(rootScopes);
        scopesBuilder.addAll(thisGroupScopes);
        final ImList.Immutable<Scope> scopes = scopesBuilder.freeze();

        // @formatter:off
        return context.runIncremental(
            initialState -> {
                logger.debug("group {}: running. restarted: {}.", resource, initialState.isPresent());
                return runSolver(context, rule, initialState, scopes);
            },
            GroupResult::solveResult,
            this::patch,
            (result, ex) -> {
                logger.debug("group {}: combining.", resource);
                return AggregateFuture.apply(groupResults, unitResults).thenApply(e -> {
                    logger.debug("group {}: returning.", resource);
                    return GroupResult.of(resource, e._1(), e._2(), result, ex);
                });
            })
            .whenComplete((r, __) -> {
                logger.debug("group {}: returned.", context.id());
            });
        // @formatter:on
    }

}
