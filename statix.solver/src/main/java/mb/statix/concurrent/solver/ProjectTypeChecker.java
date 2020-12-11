package mb.statix.concurrent.solver;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import mb.nabl2.terms.ITerm;
import mb.statix.concurrent.actors.futures.AggregateFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.ITypeCheckerContext;
import mb.statix.concurrent.p_raffrayi.IUnitResult;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.spec.Spec;

public class ProjectTypeChecker extends AbstractTypeChecker<ProjectResult> {

    private static final ILogger logger = LoggerUtils.logger(ProjectTypeChecker.class);

    private final IStatixProject project;

    public ProjectTypeChecker(IStatixProject project, Spec spec, IDebugContext debug) {
        super(spec, debug);
        this.project = project;
    }

    @Override public IFuture<ProjectResult> run(ITypeCheckerContext<Scope, ITerm, ITerm> context,
            List<Scope> rootScopes) {
        final Scope projectScope = makeSharedScope(context, "s_prj");

        final IFuture<Map<String, IUnitResult<Scope, ITerm, ITerm, GroupResult>>> groupResults =
                runGroups(context, project.groups(), projectScope, projectScope);

        final IFuture<Map<String, IUnitResult<Scope, ITerm, ITerm, UnitResult>>> unitResults =
                runUnits(context, project.units(), projectScope, projectScope);

        runLibraries(context, project.libraries(), projectScope);

        context.closeScope(projectScope);

        final IFuture<SolverResult> result = runSolver(context, project.rule(), Arrays.asList(projectScope));

        return AggregateFuture.apply(groupResults, unitResults, result).thenApply(e -> {
            return ProjectResult.of(project.resource(), e._1(), e._2(), e._3(), null);
        }).whenComplete((r, ex) -> {
            logger.debug("project {}: returned.", context.id());
        });
    }

}