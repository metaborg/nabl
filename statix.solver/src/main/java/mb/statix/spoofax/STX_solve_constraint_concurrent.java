package mb.statix.spoofax;

import static mb.nabl2.terms.matching.TermPattern.P;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import org.metaborg.util.future.IFuture;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;

import com.google.inject.Inject;

import mb.nabl2.terms.ITerm;
import mb.p_raffrayi.IUnitResult;
import mb.p_raffrayi.impl.AInitialState;
import mb.p_raffrayi.impl.Broker;
import mb.statix.concurrent.IStatixProject;
import mb.statix.concurrent.ProjectResult;
import mb.statix.concurrent.ProjectTypeChecker;
import mb.statix.concurrent.StatixDifferOps;
import mb.statix.concurrent.StatixProject;
import mb.statix.concurrent.nameresolution.ScopeImpl;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.spec.Rule;
import mb.statix.spec.Spec;

public class STX_solve_constraint_concurrent extends StatixConstraintPrimitive {

    @Inject public STX_solve_constraint_concurrent() {
        super(STX_solve_constraint_concurrent.class.getSimpleName());
    }

    @Override protected SolverResult solve(Spec spec, IConstraint constraint, IDebugContext debug, IProgress progress,
            ICancel cancel) throws InterruptedException, ExecutionException {
        final IStatixProject project =
                StatixProject.builder().resource("").rule(Rule.of("resolve", Arrays.asList(P.newWld()), constraint)).build();
        final IFuture<IUnitResult<Scope, ITerm, ITerm, ProjectResult>> future = Broker.run("",
                new ProjectTypeChecker(project, spec, debug), new ScopeImpl(), spec.allLabels(),
                AInitialState.added(), new StatixDifferOps(), cancel);
        final IUnitResult<Scope, ITerm, ITerm, ProjectResult> result = future.asJavaCompletion().get();
        final SolverResult resultConfig = result.analysis().solveResult();
        final IState.Immutable state = resultConfig.state().withScopeGraph(result.scopeGraph());
        return resultConfig.withState(state);
    }

}
