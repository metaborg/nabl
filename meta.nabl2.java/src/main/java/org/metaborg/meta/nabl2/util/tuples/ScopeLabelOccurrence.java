package org.metaborg.meta.nabl2.util.tuples;

import java.util.Objects;
import java.util.function.Predicate;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class ScopeLabelOccurrence<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements HasLabel<L> {

    @Value.Parameter public abstract S scope();

    @Value.Parameter public abstract L label();

    @Value.Parameter public abstract O occurrence();

    public final static <S extends IScope, L extends ILabel, O extends IOccurrence> 
    Predicate<ScopeLabelOccurrence<S, L, O>> occurrenceEquals(O occurrence) {
        return tuple -> Objects.equals(tuple.occurrence(), occurrence);
    }
    
    public final static <S extends IScope, L extends ILabel, O extends IOccurrence> 
    Predicate<ScopeLabelOccurrence<S, L, O>> scopeEquals(S scope) {
        return tuple -> Objects.equals(tuple.scope(), scope);
    }    
    
}