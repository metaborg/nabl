package mb.p_raffrayi.impl;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.p_raffrayi.IResult;
import mb.p_raffrayi.ITypeCheckerState;
import mb.scopegraph.oopsla20.IScopeGraph;

@Value.Immutable
@Serial.Version(42)
public abstract class ATypeCheckerResult<S, L, D, A extends IResult<S, L, D>, T extends ITypeCheckerState<S, L, D>> {

    /**
     * @return Analysis result.
     */
    @Value.Parameter public abstract A analysis();

    /**
     * @return Capture of context-independent state.
     */
    @Value.Parameter public abstract StateCapture<S, L, D, T> localState();

    /**
     * @return Local scope graph created by this type-checker.
     */
    @Value.Parameter public abstract IScopeGraph.Immutable<S, L, D> scopeGraph();

}
