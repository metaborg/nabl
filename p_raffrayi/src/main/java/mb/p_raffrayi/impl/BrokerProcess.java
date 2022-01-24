package mb.p_raffrayi.impl;

import mb.p_raffrayi.actors.IActor;

class BrokerProcess<S, L, D> implements IProcess<S, L, D> {

    private static final BrokerProcess<?, ?, ?> INSTANCE = new BrokerProcess<>();

    private BrokerProcess() {}

    @Override public IDeadlockProtocol<S, L, D> from(IActor<? extends IDeadlockProtocol<S, L, D>> origin, IUnitContext<S, L, D> context) {
        return context.deadlock();
    }

    @Override public IDeadlockProtocol<S, L, D> from(Broker<S, L, D, ?, ?> origin) {
        return origin;
    }

    @Override public String toString() {
        return "system:/";
    }

    @SuppressWarnings("unchecked")
    public static <S, L, D> BrokerProcess<S, L, D> of() {
        return (BrokerProcess<S, L, D>) INSTANCE;
    }
}