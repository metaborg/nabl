package mb.nabl2.sets;

import mb.nabl2.scopegraph.CriticalEdgeException;
import mb.nabl2.scopegraph.StuckException;

@FunctionalInterface
public interface ISetProducer<T> {

    java.util.Set<IElement<T>> apply() throws CriticalEdgeException, StuckException, InterruptedException;

}