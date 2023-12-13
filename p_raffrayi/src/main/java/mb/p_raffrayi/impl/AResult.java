package mb.p_raffrayi.impl;

import java.util.Set;

import jakarta.annotation.Nullable;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.p_raffrayi.ITypeChecker.IOutput;
import mb.p_raffrayi.ITypeChecker.IState;
import mb.scopegraph.oopsla20.IScopeGraph;

/**
 * Result emitted by a {@link TypeCheckerUnit}.
 */
@Value.Immutable
@Serial.Version(42)
public abstract class AResult<S, L, D, A extends IOutput<S, L, D>, T extends IState<S, L, D>> {

    /**
     * @return Analysis result.
     */
    @Value.Parameter @Nullable public abstract A analysis();

    /**
     * @return Capture of context-independent state.
     */
    @Value.Parameter @Nullable public abstract StateCapture<S, L, D, T> localState();

    /**
     * @return Local scope graph created by this type-checker.
     */
    @Value.Parameter @Nullable public abstract IScopeGraph.Immutable<S, L, D> scopeGraph();

    /**
     * @return Scopes shared with subunits.
     */
    @Value.Parameter @Nullable public abstract Set<S> sharedScopes();

}
