package mb.p_raffrayi.impl;

import mb.p_raffrayi.actors.IActor;

public interface IProcess<S, L, D> {

    IDeadlockProtocol<S, L, D> from(IActor<? extends IDeadlockProtocol<S, L, D>> process, IUnitContext<S, L, D> context);

    IDeadlockProtocol<S, L, D> from(Broker<S, L, D, ?> process);

}
