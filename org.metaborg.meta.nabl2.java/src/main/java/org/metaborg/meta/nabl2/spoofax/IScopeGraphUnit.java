package org.metaborg.meta.nabl2.spoofax;

import java.io.Serializable;
import java.util.Optional;

import org.metaborg.meta.nabl2.solver.Solution;

public interface IScopeGraphUnit extends Serializable {

    String resource();

    Optional<Solution> solution();

    Optional<CustomSolution> customSolution();

}