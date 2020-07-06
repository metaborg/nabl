package mb.statix.p_raffrayi.impl;

import mb.statix.actors.futures.ICompletable;
import mb.statix.p_raffrayi.ITypeCheckerContext;
import mb.statix.scopegraph.reference.Access;

public interface IUnit<S, L, D, R>
        extends IUnit2UnitProtocol<S, L, D>, IBroker2UnitProtocol<S, L, D, R>, ITypeCheckerContext<S, L, D, R> {

    void _setDatum(S scope, D datum, Access access);

    void _complete(ICompletable<Void> future);

}