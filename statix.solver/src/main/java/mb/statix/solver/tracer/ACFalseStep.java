package mb.statix.solver.tracer;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITermVar;
import mb.statix.constraints.CFalse;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import org.immutables.value.Value;

import java.util.Collection;

@Value.Immutable
public abstract class ACFalseStep implements IStep {

    @Value.Parameter @Override public abstract CFalse constraint();

    @Value.Parameter @Override public abstract StepResult result();

}
