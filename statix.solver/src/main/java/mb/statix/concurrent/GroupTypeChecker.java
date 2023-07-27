package mb.statix.concurrent;

import java.util.List;
//import java.util.Map;
import java.util.function.Supplier;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.util.collection.ImList;
import org.metaborg.util.future.AggregateFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import io.usethesource.capsule.Map;
import mb.nabl2.terms.ITerm;
import mb.p_raffrayi.IUnitResult;
import mb.p_raffrayi.impl.Result;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.tracer.SolverTracer;
import mb.statix.spec.Rule;
import mb.statix.spec.Spec;
import mb.p_raffrayi.IIncrementalTypeCheckerContext;

public class GroupTypeChecker<TR extends SolverTracer.IResult<TR>> extends AbstractTypeChecker<GroupResult<TR>, TR> {

    private static final ILogger logger = LoggerUtils.logger(GroupTypeChecker.class);

    private final IStatixGroup group;

    public GroupTypeChecker(IStatixGroup group, Spec spec, IDebugContext debug, Supplier<SolverTracer<TR>> tracerFactory, int solverFlags) {
        super(spec, debug, tracerFactory, solverFlags);
        this.group = group;
    }

    @Override public IFuture<GroupResult<TR>> run(IIncrementalTypeCheckerContext<Scope, ITerm, ITerm, GroupResult<TR>, SolverState> context,
            List<Scope> rootScopes) {

        final List<Scope> thisGroupScopes = group.scopeNames().stream().map(name -> makeSharedScope(context, name)).collect(Collectors.toList());
        final IFuture<io.usethesource.capsule.Map.Immutable<String, IUnitResult<Scope, ITerm, ITerm, Result<Scope, ITerm, ITerm, GroupResult<TR>, SolverState>>>>
            groupResults = runGroups(context, group.groups(), thisGroupScopes);
        final IFuture<Map.Immutable<String, IUnitResult<Scope, ITerm, ITerm, Result<Scope, ITerm, ITerm, UnitResult<TR>, SolverState>>>>
            unitResults = runUnits(context, group.units(), thisGroupScopes);
        thisGroupScopes.forEach(context::closeScope);

        final Optional<Rule> rule = group.rule();
        final String resource = group.resource();

        final ImList.Mutable<Scope> scopesBuilder = new ImList.Mutable<>(rootScopes.size() + thisGroupScopes.size());
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
