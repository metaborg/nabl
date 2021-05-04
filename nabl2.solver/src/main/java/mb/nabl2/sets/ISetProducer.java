package mb.nabl2.sets;

import io.usethesource.capsule.Set;
import mb.scopegraph.pepm16.CriticalEdgeException;
import mb.scopegraph.pepm16.StuckException;

@FunctionalInterface
public interface ISetProducer<T> {

    Set.Immutable<IElement<T>> apply() throws CriticalEdgeException, StuckException, InterruptedException;

}