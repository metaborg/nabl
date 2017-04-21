package org.metaborg.meta.nabl2.spoofax.analysis;

import java.io.Serializable;
import java.util.Optional;
import java.util.Set;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.solver.Fresh;
import org.metaborg.meta.nabl2.solver.Solution;

public interface IScopeGraphUnit extends Serializable {

    String resource();

    Set<IConstraint> constraints();

    Optional<Solution> solution();

    Optional<CustomSolution> customSolution();

    Fresh fresh();

    /**
     * Check if the solution returned is for this resource exactly,
     * or if this unit is just a part of it.
     */
    boolean isPrimary();

}
