package mb.statix.taico.solver;

import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.immutables.value.Value;

import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.taico.module.IModule;

@Value.Immutable
public abstract class ACompletenessResult {

    @Value.Parameter public abstract boolean isComplete();

    @Value.Parameter @Nullable public abstract IModule cause();
    
    @Value.Default @Nullable public Delay delay() {
        return null;
    }
    
    @Value.Default @Nullable public List<CriticalEdge> details() {
        return null;
    }
}
