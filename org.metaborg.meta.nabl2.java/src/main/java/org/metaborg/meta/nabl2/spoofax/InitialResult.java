package org.metaborg.meta.nabl2.spoofax;

import java.util.Optional;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.spoofax.interpreter.terms.IStrategoTerm;

@Value.Immutable
@Serial.Structural
public interface InitialResult {

    Iterable<IConstraint> getConstraints();

    Iterable<IStrategoTerm> getParams();

    Optional<IStrategoTerm> getType();

}