package org.metaborg.meta.nabl2.stratego;

import java.util.List;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.spoofax.interpreter.terms.IStrategoTerm;

@Value.Immutable
@Serial.Version(value = 42L)
abstract class StrategoAnnotations {

    @Value.Parameter public abstract List<IStrategoTerm> getAnnotationList();

}