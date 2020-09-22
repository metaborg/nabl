package mb.statix.concurrent.actors.futures;

import java.util.concurrent.ExecutorService;

public class AsyncCompletable<T> implements ICompletable<T> {

    private final ExecutorService executor;
    private final ICompletable<T> completable;

    public AsyncCompletable(ExecutorService executor, ICompletable<T> completable) {
        this.executor = executor;
        this.completable = completable;
    }

    @Override public boolean isDone() {
        return completable.isDone();
    }

    @Override public void complete(T value, Throwable ex) {
        executor.submit(() -> {
            completable.complete(value, ex);
        });
    }

}