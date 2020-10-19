package mb.statix.concurrent.actors.futures;

public interface ICompletable<T> {

    boolean isDone();

    void complete(T value, Throwable ex);

    default void complete(T value) {
        complete(value, null);
    }

    default void completeExceptionally(Throwable ex) {
        complete(null, ex);
    }

}