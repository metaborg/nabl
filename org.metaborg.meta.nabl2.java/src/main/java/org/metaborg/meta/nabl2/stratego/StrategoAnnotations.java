package org.metaborg.meta.nabl2.stratego;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.spoofax.interpreter.terms.IStrategoList;

@Value.Immutable
@Serial.Version(value = 42L)
abstract class StrategoAnnotations {

    @Value.Parameter public abstract IStrategoList getAnnotationList();

}