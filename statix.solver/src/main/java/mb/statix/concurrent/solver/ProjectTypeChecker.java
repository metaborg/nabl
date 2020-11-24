package mb.statix.concurrent.solver;

import java.util.Map;

import javax.annotation.Nullable;

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

    private final IStatixProject project;

    public ProjectTypeChecker(IStatixProject project, Spec spec, IDebugContext debug) {
        super(spec, debug);
        this.project = project;
    }

    @Override public IFuture<ProjectResult> run(ITypeCheckerContext<Scope, ITerm, ITerm> context,
            @Nullable Scope root) {
        final Scope projectScope = makeSharedScope(context, "s_prj");
        final IFuture<Map<String, IUnitResult<Scope, ITerm, ITerm, GroupResult>>> groupResults =
                runGroups(context, project.groups(), projectScope);
        final IFuture<Map<String, IUnitResult<Scope, ITerm, ITerm, UnitResult>>> unitResults =
                runUnits(context, project.units(), projectScope);
        context.closeScope(projectScope);
        final IFuture<SolverResult> result = runSolver(context, project.rule(), projectScope);
        return AggregateFuture.apply(groupResults, unitResults, result).thenApply(e -> {
            return ProjectResult.of(project.resource(), e._1(), e._2(), e._3(), null);
        });
    }

}