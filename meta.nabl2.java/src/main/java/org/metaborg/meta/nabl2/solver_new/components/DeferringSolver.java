package org.metaborg.meta.nabl2.solver_new.components;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.solver_new.ASolver;
import org.metaborg.meta.nabl2.solver_new.SolverCore;

import com.google.common.collect.Sets;

public class DeferringSolver<C extends IConstraint> extends ASolver<C, DeferringSolver.DeferringResult<C>> {

    private final java.util.Set<C> constraints;

    public DeferringSolver(SolverCore core) {
        super(core);
        this.constraints = Sets.newHashSet();
    }

    // ------------------------------------------------------------------------------------------------------//

    @Override public boolean add(C constraint) {
        return constraints.add(constraint);
    }

    public DeferringResult<C> finish() {
        return ImmutableDeferringResult.of(constraints);
    }

    // ------------------------------------------------------------------------------------------------------//

    @Value.Immutable
    @Serial.Version(42L)
    public static abstract class DeferringResult<C extends IConstraint> {

        @Value.Parameter public abstract java.util.Set<C> residualConstraints();

    }

}