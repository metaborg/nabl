package org.metaborg.meta.nabl2.spoofax;

import java.util.Optional;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.spoofax.interpreter.terms.IStrategoTerm;

@Value.Immutable
@Serial.Structural
public interface InitialResult {

    @Value.Parameter Iterable<IConstraint> getConstraints();

    @Value.Parameter Iterable<IStrategoTerm> getParams();

    @Value.Parameter Optional<IStrategoTerm> getType();

}