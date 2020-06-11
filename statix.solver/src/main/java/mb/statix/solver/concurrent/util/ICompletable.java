package mb.statix.solver.concurrent.util;

public interface ICompletable<T> {

    boolean isDone();

    void complete(T value);

    void completeExceptionally(Throwable ex);

}
