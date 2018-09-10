package mb.nabl2.util.tuples;

import java.util.Objects;
import java.util.function.Predicate;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IScope;

@SuppressWarnings("unused")
@Value.Immutable
@Serial.Version(value = 42L)
public abstract class ScopeLabelScope<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements HasLabel<L> {

    @Value.Parameter public abstract S sourceScope();

    @Value.Parameter public abstract L label();

    @Value.Parameter public abstract S targetScope();

    public final static <S extends IScope, L extends ILabel, O extends IOccurrence> 
    Predicate<ScopeLabelScope<S, L, O>> sourceScopeEquals(S scope) {
        return tuple -> Objects.equals(tuple.sourceScope(), scope);
    } 
    
    public final static <S extends IScope, L extends ILabel, O extends IOccurrence> 
    Predicate<ScopeLabelScope<S, L, O>> targetScopeEquals(S scope) {
        return tuple -> Objects.equals(tuple.targetScope(), scope);
    }    
    
}