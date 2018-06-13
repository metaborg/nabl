package mb.nabl2.spoofax.analysis;

import java.io.Serializable;
import java.util.Optional;
import java.util.Set;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.solver.Fresh;
import mb.nabl2.solver.ISolution;

public interface IScopeGraphUnit extends Serializable {

    String resource();

    Set<IConstraint> constraints();

    Optional<ISolution> solution();

    Optional<CustomSolution> customSolution();

    Fresh fresh();

    /**
     * Check if the solution returned is for this resource exactly,
     * or if this unit is just a part of it.
     */
    boolean isPrimary();

}