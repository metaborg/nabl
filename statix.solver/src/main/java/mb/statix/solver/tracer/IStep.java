package mb.statix.solver.tracer;

import mb.statix.solver.IConstraint;

public interface IStep {

    IConstraint constraint();

    StepResult result();

}
