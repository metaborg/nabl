package mb.p_raffrayi.actors.impl;

interface IMessage<T> {

    void dispatch(T impl) throws ActorException;

    void fail(ActorStoppedException ex);

}