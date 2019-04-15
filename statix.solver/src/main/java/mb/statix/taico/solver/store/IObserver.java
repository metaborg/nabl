package mb.statix.taico.solver.store;

@FunctionalInterface
public interface IObserver<T> {
    /**
     * Called whenever an update occurs.
     * 
     * @param t
     *      the item that was updated
     */
    public void notify(T t);
}
