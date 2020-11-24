package mb.statix.concurrent.solver;

import javax.annotation.Nullable;

import mb.statix.solver.persistent.SolverResult;


public interface IStatixResult {

    @Nullable SolverResult solveResult();

    @Nullable Throwable exception();

}