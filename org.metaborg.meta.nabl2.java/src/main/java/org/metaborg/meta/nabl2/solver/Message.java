package org.metaborg.meta.nabl2.solver;

import org.immutables.value.Value;
import org.metaborg.meta.nabl2.terms.ITerm;

@Value.Immutable
public abstract class Message {

    @Value.Parameter public abstract ITerm getOrigin();

    @Value.Parameter public abstract String getMessage();

}