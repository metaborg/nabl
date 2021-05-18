package mb.p_raffrayi.impl;

import mb.p_raffrayi.actors.IActor;

/**
 * Models an external view on a process, used for deadlock detection. A process can either be a unit or the broker.
 */
public interface IProcess<S, L, D> {

    /**
     * Provides the actor {@code origin} with an interface to send messages to this process.
     */
    IDeadlockProtocol<S, L, D> from(IActor<? extends IDeadlockProtocol<S, L, D>> origin, IUnitContext<S, L, D> context);

    /**
     * Provides the broker {@code origin} with an interface to send messages to this process.
     */
    IDeadlockProtocol<S, L, D> from(Broker<S, L, D, ?> origin);

}
