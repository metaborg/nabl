package mb.statix.actors.impl;

interface IMessage<T> {

    void dispatch(T impl) throws ActorException;

}