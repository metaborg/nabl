package mb.statix.concurrent.solver;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import mb.nabl2.terms.ITerm;
import mb.statix.concurrent.actors.futures.AggregateFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.IIncrementalTypeCheckerContext;
import mb.statix.concurrent.p_raffrayi.IUnitResult;
import mb.statix.concurrent.p_raffrayi.impl.IInitialState;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.Spec;

public class ProjectTypeChecker extends AbstractTypeChecker<ProjectResult> {

    private static final ILogger logger = LoggerUtils.logger(ProjectTypeChecker.class);

    private final IStatixProject project;

    public ProjectTypeChecker(IStatixProject project, Spec spec, IDebugContext debug) {
        super(spec, debug);
        this.project = project;
    }

    @Override public IFuture<ProjectResult> run(IIncrementalTypeCheckerContext<Scope, ITerm, ITerm, ProjectResult> context,
            @SuppressWarnings("unused") List<Scope> rootScopes, IInitialState<Scope, ITerm, ITerm, ProjectResult> initialState) {
        final Scope projectScope = makeSharedScope(context, "s_prj");

        final IFuture<Map<String, IUnitResult<Scope, ITerm, ITerm, GroupResult>>> groupResults =
            runGroups(context, project.groups(), projectScope, projectScope, initialState);

        final IFuture<Map<String, IUnitResult<Scope, ITerm, ITerm, UnitResult>>> unitResults =
            runUnits(context, project.units(), projectScope, projectScope, initialState);

        runLibraries(context, project.libraries(), projectScope);

        context.closeScope(projectScope);

        // @formatter:off
        return context.runIncremental(
            restarted -> {
                return runSolver(context, project.rule(), Arrays.asList(projectScope));
            },
            ProjectResult::solveResult,
            (result, ex) -> {
                return AggregateFuture.apply(groupResults, unitResults).thenApply(e -> {
                    return ProjectResult.of(project.resource(), e._1(), e._2(), result, ex);
                });
            })
            .whenComplete((r, __) -> {
                logger.debug("group {}: returned.", context.id());
            });
        // @formatter:on
    }

}