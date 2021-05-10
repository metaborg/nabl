package mb.p_raffrayi.actors.impl;

import mb.p_raffrayi.actors.IActorRef;
import mb.p_raffrayi.actors.TypeTag;

interface IActorContext {

    <U> IActorImpl<U> add(IActorInternal<?> self, String id, TypeTag<U> type);

    <U> U async(IActorRef<U> receiver);

    IActorScheduler scheduler();

}