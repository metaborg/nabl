package mb.statix.concurrent;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.metaborg.util.future.AggregateFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.unit.Unit;


import mb.nabl2.terms.ITerm;
import mb.p_raffrayi.IUnitResult;
import mb.p_raffrayi.impl.Result;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.tracer.SolverTracer;
import mb.statix.spec.Spec;
import mb.p_raffrayi.IIncrementalTypeCheckerContext;

public class ProjectTypeChecker<TR extends SolverTracer.IResult<TR>> extends AbstractTypeChecker<ProjectResult<TR>, TR> {

    private static final ILogger logger = LoggerUtils.logger(ProjectTypeChecker.class);

    private final IStatixProject<TR> project;

    public ProjectTypeChecker(IStatixProject<TR> project, Spec spec, IDebugContext debug, Supplier<SolverTracer<TR>> tracerFactory) {
        this(project, spec, debug, tracerFactory, 0);
    }

    public ProjectTypeChecker(IStatixProject<TR> project, Spec spec, IDebugContext debug, Supplier<SolverTracer<TR>> tracerFactory, int solverFlags) {
        super(spec, debug, tracerFactory, solverFlags);
        this.project = project;
    }

    @Override public IFuture<ProjectResult<TR>> run(IIncrementalTypeCheckerContext<Scope, ITerm, ITerm, ProjectResult<TR>, SolverState> context,
            @SuppressWarnings("unused") List<Scope> rootScopes) {
        final Scope projectScope = makeSharedScope(context, "s_prj");

        final IFuture<io.usethesource.capsule.Map.Immutable<String, IUnitResult<Scope, ITerm, ITerm, Unit>>> libraryResults =
            runLibraries(context, project.libraries(), projectScope);

        final IFuture<io.usethesource.capsule.Map.Immutable<String, IUnitResult<Scope, ITerm, ITerm, Result<Scope, ITerm, ITerm, GroupResult<TR>, SolverState>>>> groupResults =
            runGroups(context, project.groups(), Arrays.asList(projectScope));

        final IFuture<io.usethesource.capsule.Map.Immutable<String, IUnitResult<Scope, ITerm, ITerm, Result<Scope, ITerm, ITerm, UnitResult<TR>, SolverState>>>> unitResults =
            runUnits(context, project.units(), Arrays.asList(projectScope));

        context.closeScope(projectScope);

        // @formatter:off
        return context.runIncremental(
            initialState -> {
                return runSolver(context, project.rule(), initialState, Arrays.asList(projectScope));
            },
            ProjectResult::solveResult,
            this::patch,
            (result, ex) -> {
                return AggregateFuture.apply(libraryResults, groupResults, unitResults).thenApply(e -> {
                    return ProjectResult.of(project.resource(), projectScope, e._1(), e._2(), e._3(), result, ex);
                });
            })
            .whenComplete((r, __) -> {
                logger.debug("project {}: returned.", context.id());
            });
        // @formatter:on
    }
}
