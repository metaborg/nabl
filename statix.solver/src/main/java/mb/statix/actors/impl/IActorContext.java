package mb.statix.actors.impl;

import org.metaborg.util.functions.Function1;

import mb.statix.actors.IActor;
import mb.statix.actors.IActorRef;
import mb.statix.actors.TypeTag;

interface IActorContext {

    <U> IActor<U> add(String id, TypeTag<U> type, Function1<IActor<U>, U> supplier);

    <T> T async(IActorRef<T> receiver);

}