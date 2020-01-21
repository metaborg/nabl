package mb.nabl2.sets;

import org.metaborg.util.functions.CheckedFunction0;

import mb.nabl2.scopegraph.esop.CriticalEdgeException;

@FunctionalInterface
public interface ISetProducer<T> extends CheckedFunction0<java.util.Set<IElement<T>>, CriticalEdgeException> {

}