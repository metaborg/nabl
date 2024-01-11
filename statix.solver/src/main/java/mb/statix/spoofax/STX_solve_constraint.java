package mb.statix.spoofax;

import java.util.concurrent.ExecutionException;

import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;


import mb.statix.solver.IConstraint;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.solver.persistent.State;
import mb.statix.solver.tracer.EmptyTracer.Empty;
import mb.statix.spec.Spec;

public class STX_solve_constraint extends StatixConstraintPrimitive {

    @jakarta.inject.Inject @javax.inject.Inject public STX_solve_constraint() {
        super(STX_solve_constraint.class.getSimpleName());
    }

    @Override protected SolverResult<Empty> solve(Spec spec, IConstraint constraint, IDebugContext debug,
            IProgress progress, ICancel cancel) throws InterruptedException, ExecutionException {
        return Solver.solve(spec, State.of(), constraint, debug, cancel, progress, 0);
    }

}
