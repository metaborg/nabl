package mb.statix.solver.concurrent;

import mb.statix.actors.IFuture;

public interface ITypeChecker<S, L, D, R> {

    IFuture<R> run(S root) throws InterruptedException;

}