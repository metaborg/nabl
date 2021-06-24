package mb.p_raffrayi.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.metaborg.util.functions.Function2;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.tuple.Tuple2;

import io.usethesource.capsule.Set.Immutable;
import mb.p_raffrayi.IUnitResult;
import mb.p_raffrayi.PRaffrayiSettings;
import mb.p_raffrayi.actors.IActor;
import mb.p_raffrayi.actors.IActorRef;
import mb.scopegraph.oopsla20.diff.BiMap;

/**
 * Protocol accepted by the broker, from units
 */
public interface IUnitContext<S, L, D> {

    ICancel cancel();

    PRaffrayiSettings settings();

    S makeScope(String name);

    String scopeId(S scope);

    D substituteScopes(D datum, Map<S, S> substitution);

    Immutable<S> getScopes(D datum);

    D embed(S scope);

    Optional<BiMap.Immutable<S>> matchDatums(D currentDatum, D previousDatum);

    IFuture<IActorRef<? extends IUnit<S, L, D, ?>>> owner(S scope);

    <R> Tuple2<IFuture<IUnitResult<S, L, D, R>>, IActorRef<? extends IUnit<S, L, D, R>>> add(String id,
            Function2<IActor<IUnit<S, L, D, R>>, IUnitContext<S, L, D>, IUnit<S, L, D, R>> unitProvider,
            List<S> rootScopes);

    int parallelism();

    IDeadlockProtocol<S, L, D> deadlock();

}