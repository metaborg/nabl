package mb.p_raffrayi.impl;

import java.util.Set;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.p_raffrayi.IRecordedQuery;
import mb.scopegraph.oopsla20.reference.Env;

@Value.Immutable
@Serial.Version(42L)
public abstract class AQueryAnswer<S, L, D> implements IQueryAnswer<S, L, D> {

    @Override @Value.Parameter public abstract Env<S, L, D> env();

    @Override @Value.Parameter public abstract Set<IRecordedQuery<S, L, D>> transitiveQueries();

    @Override @Value.Parameter public abstract Set<IRecordedQuery<S, L, D>> predicateQueries();

}
