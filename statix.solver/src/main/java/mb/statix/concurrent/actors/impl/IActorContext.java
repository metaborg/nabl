package mb.statix.concurrent.actors.impl;

import java.util.concurrent.ExecutorService;

import org.metaborg.util.functions.Function1;

import mb.statix.concurrent.actors.IActor;
import mb.statix.concurrent.actors.IActorRef;
import mb.statix.concurrent.actors.TypeTag;

interface IActorContext {

    <U> IActor<U> add(String id, TypeTag<U> type, Function1<IActor<U>, U> supplier);

    <T> T async(IActorRef<T> receiver);

    ExecutorService executor();

}