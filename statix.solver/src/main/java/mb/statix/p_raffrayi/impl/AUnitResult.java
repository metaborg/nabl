package mb.statix.p_raffrayi.impl;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.statix.p_raffrayi.IUnitResult;
import mb.statix.scopegraph.IScopeGraph;

@Value.Immutable
@Serial.Version(42L)
abstract class AUnitResult<S, L, D, R> implements IUnitResult<S, L, D, R> {

    @Value.Parameter @Override public abstract R analysis();

    @Value.Parameter @Override public abstract IScopeGraph.Immutable<S, L, D> scopeGraph();

}
