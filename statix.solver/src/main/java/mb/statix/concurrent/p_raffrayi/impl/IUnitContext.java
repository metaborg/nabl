package mb.statix.concurrent.p_raffrayi.impl;

import java.util.List;
import java.util.Map;

import org.metaborg.util.functions.Function2;
import org.metaborg.util.task.ICancel;

import mb.nabl2.util.Tuple2;
import mb.statix.concurrent.actors.IActor;
import mb.statix.concurrent.actors.IActorRef;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.IUnitResult;

/**
 * Protocol accepted by the broker, from units
 */
public interface IUnitContext<S, L, D> {

    ICancel cancel();

    S makeScope(String name);

    D substituteScopes(D datum, Map<S, S> substitution);

    IActorRef<? extends IUnit<S, L, D, ?>> owner(S scope);

    <R> Tuple2<IFuture<IUnitResult<S, L, D, R>>, IActorRef<? extends IUnit<S, L, D, R>>> add(String id,
            Function2<IActor<IUnit<S, L, D, R>>, IUnitContext<S, L, D>, IUnit<S, L, D, R>> unitProvider,
            List<S> rootScopes);

}