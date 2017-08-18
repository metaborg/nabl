package meta.flowspec.java.solver;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

import meta.flowspec.java.lattice.CompleteLattice;

@Immutable
public abstract class Metadata {
    enum Direction {
        Forward,
        Backward,
        FlowInsensitive
    }

    @Parameter public abstract Direction dir();
    @Parameter public abstract CompleteLattice<Object> lattice();
    @Parameter public abstract Type type();
}
