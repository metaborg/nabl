package mb.statix.concurrent.p_raffrayi.impl;

import java.util.List;

import javax.annotation.Nullable;

import org.metaborg.util.task.ICancel;

import mb.nabl2.util.Tuple2;
import mb.statix.concurrent.actors.IActorRef;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.ITypeChecker;
import mb.statix.concurrent.p_raffrayi.IUnitResult;

/**
 * Protocol accepted by the broker, from units
 */
public interface IUnitContext<S, L, D> {

    ICancel cancel();

    S makeScope(String name);

    IActorRef<? extends IUnit<S, L, D, ?>> owner(S scope);

    <R> Tuple2<IFuture<IUnitResult<S, L, D, R>>, IActorRef<? extends IUnit<S, L, D, R>>> add(String id,
            ITypeChecker<S, L, D, R> unitChecker, List<S> rootScopes,
            @Nullable IUnitResult<S, L, D, R> previousResult);

}