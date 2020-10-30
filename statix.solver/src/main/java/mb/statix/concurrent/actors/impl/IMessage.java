package mb.statix.concurrent.actors.impl;

interface IMessage<T> {

    void dispatch(T impl) throws ActorException;

    void fail(ActorStoppedException ex);

}