package mb.nabl2.sets;

import io.usethesource.capsule.Set;
import mb.nabl2.scopegraph.CriticalEdgeException;
import mb.nabl2.scopegraph.StuckException;

@FunctionalInterface
public interface ISetProducer<T> {

    Set.Immutable<IElement<T>> apply() throws CriticalEdgeException, StuckException, InterruptedException;

}