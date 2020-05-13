package mb.nabl2.sets;

import mb.nabl2.scopegraph.esop.CriticalEdgeException;

@FunctionalInterface
public interface ISetProducer<T> {

    java.util.Set<IElement<T>> apply() throws CriticalEdgeException, InterruptedException;

}