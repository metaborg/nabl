package mb.statix.concurrent.actors.impl;

import mb.statix.concurrent.actors.IActorRef;
import mb.statix.concurrent.actors.TypeTag;

interface IActorContext {

    <U> IActorImpl<U> add(IActorInternal<?> self, String id, TypeTag<U> type);

    <U> U async(IActorRef<U> receiver);

    IActorScheduler scheduler();

}