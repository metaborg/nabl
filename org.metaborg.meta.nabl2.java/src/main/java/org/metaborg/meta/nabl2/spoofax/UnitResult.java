package org.metaborg.meta.nabl2.spoofax;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.spoofax.interpreter.terms.IStrategoTerm;

@Value.Immutable
@Serial.Structural
public interface UnitResult {

    IStrategoTerm getAST();

    Iterable<IConstraint> getConstraints();

}