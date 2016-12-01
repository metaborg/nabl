package org.metaborg.meta.nabl2.solver;

import java.io.Serializable;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.common.collect.Multimap;

@Value.Immutable
@Serial.Version(value = 1L)
public interface Solution extends Serializable {

    @Value.Parameter Multimap<IStrategoTerm,String> getErrors();

    @Value.Parameter Multimap<IStrategoTerm,String> getWarnings();

    @Value.Parameter Multimap<IStrategoTerm,String> getNotes();

}