package mb.statix.solver.tracer;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITermVar;
import mb.statix.constraints.CConj;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import org.immutables.value.Value;

import java.util.Collection;

@Value.Immutable
public abstract class ACConjStep implements IStep {

    @Value.Parameter @Override public abstract CConj constraint();

    @Value.Parameter @Override public abstract StepResult result();

}
