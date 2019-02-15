package mb.statix.taico.scopegraph;

import mb.statix.taico.util.IOwnable;

/**
 * Interface to represent an edge with a certain label.
 *
 * @param <S>
 *      the type of source
 * @param <L>
 *      the type of labels
 * @param <T>
 *      the type of targets
 */
public interface IEdge<S extends IOwnable<S, ?, ?>, L, T> extends IOwnable {
    S getSource();
    L getLabel();
    T getTarget();
}
