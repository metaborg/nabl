package mb.statix.solver.concurrent2.impl;

import mb.statix.actors.IFuture;
import mb.statix.scopegraph.reference.Access;

/**
 * Protocol accepted by clients, from other clients
 */
public interface IUnitProtocol<S, L, D> {

    void start(S root);

    IFuture<String> query(S scope, String text, Access access);

}