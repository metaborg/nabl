package mb.nabl2.util.tuples;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IScope;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class ScopeLabelOccurrence<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements HasScope<S>, HasLabel<L>, HasOccurrence<O> {

    @Override @Value.Parameter public abstract S scope();

    @Override @Value.Parameter public abstract L label();

    @Override @Value.Parameter public abstract O occurrence();

}