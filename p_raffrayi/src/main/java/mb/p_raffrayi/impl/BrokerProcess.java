package mb.p_raffrayi.impl;

import mb.p_raffrayi.actors.IActor;

class BrokerProcess<S, L, D> implements IProcess<S, L, D> {

    @Override public IDeadlockProtocol<S, L, D> from(IActor<? extends IDeadlockProtocol<S, L, D>> process, IUnitContext<S, L, D> context) {
        return context.deadlock();
    }

    @Override public IDeadlockProtocol<S, L, D> from(Broker<S, L, D, ?> process) {
        return process;
    }

    @Override public String toString() {
        return "system:/";
    }
}