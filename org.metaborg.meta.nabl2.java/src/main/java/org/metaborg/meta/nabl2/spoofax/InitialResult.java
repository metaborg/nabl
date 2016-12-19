package org.metaborg.meta.nabl2.spoofax;

import java.util.Optional;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.solver.SolverConfig;
import org.spoofax.interpreter.terms.IStrategoTerm;

@Value.Immutable
@Serial.Version(value = 42L)
public interface InitialResult {

    @Value.Parameter Iterable<IConstraint> getConstraints();

    @Value.Parameter Args getArgs();

    @Value.Parameter SolverConfig getConfig();

    Optional<IStrategoTerm> getCustomResult();

}