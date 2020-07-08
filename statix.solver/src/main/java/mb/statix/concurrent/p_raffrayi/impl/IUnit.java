package mb.statix.concurrent.p_raffrayi.impl;

import mb.statix.concurrent.actors.futures.ICompletable;
import mb.statix.concurrent.p_raffrayi.ITypeCheckerContext;

public interface IUnit<S, L, D, R>
        extends IUnit2UnitProtocol<S, L, D>, IBroker2UnitProtocol<S, L, D, R>, ITypeCheckerContext<S, L, D, R> {

    void _complete(ICompletable<Void> future);

}