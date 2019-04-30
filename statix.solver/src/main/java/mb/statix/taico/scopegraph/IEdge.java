package mb.statix.taico.scopegraph;

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
public interface IEdge<S, L, T> {
    S getSource();
    L getLabel();
    T getTarget();
}
