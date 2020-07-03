package mb.statix.actors.impl;

import javax.annotation.Nullable;

import mb.statix.actors.IActorRef;

interface IMessage<T> {

    @Nullable IActorRef<?> sender();

    void dispatch(T impl);

}