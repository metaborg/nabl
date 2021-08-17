package mb.p_raffrayi.impl;

import mb.p_raffrayi.actors.IActor;
import mb.p_raffrayi.actors.IActorRef;

class UnitProcess<S, L, D> implements IProcess<S, L, D> {

    private final IActorRef<? extends IDeadlockProtocol<S, L, D>> unit;

    public UnitProcess(IActorRef<? extends IDeadlockProtocol<S, L, D>> unit) {
        this.unit = unit;
    }

    @Override public IDeadlockProtocol<S, L, D> from(IActor<? extends IDeadlockProtocol<S, L, D>> origin, IUnitContext<S, L, D> context) {
        return origin.async(unit);
    }

    @Override public IDeadlockProtocol<S, L, D> from(Broker<S, L, D, ?, ?> origin) {
        return origin.deadlock(unit);
    }

    @Override public boolean equals(Object obj) {
        if(obj == this)
            return true;
        if(obj == null)
            return false;
        if(this.getClass() != obj.getClass())
            return false;
        @SuppressWarnings("unchecked") UnitProcess<S, L, D> other = (UnitProcess<S, L, D>) obj;
        return this.unit == other.unit;
    }

    @Override public int hashCode() {
        return unit.hashCode();
    }

    @Override public String toString() {
        return unit.toString();
    }
}