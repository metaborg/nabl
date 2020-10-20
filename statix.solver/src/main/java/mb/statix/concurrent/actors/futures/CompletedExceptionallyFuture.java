package mb.statix.concurrent.actors.futures;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import org.metaborg.util.functions.CheckedAction1;
import org.metaborg.util.functions.CheckedAction2;
import org.metaborg.util.functions.CheckedFunction1;
import org.metaborg.util.functions.CheckedFunction2;

class CompletedExceptionallyFuture<T> implements ICompletableFuture<T> {

    private final Throwable ex;

    public CompletedExceptionallyFuture(Throwable ex) {
        if(ex == null) {
            throw new IllegalArgumentException("null value is not supported.");
        }
        this.ex = ex;
    }

    @Override public <U> IFuture<U> handle(CheckedFunction2<? super T, Throwable, ? extends U, ?> handler) {
        try {
            return CompletableFuture.completedFuture(handler.apply(null, ex));
        } catch(Throwable ex) {
            return CompletableFuture.completedExceptionally(ex);
        }
    }

    @Override public IFuture<T> whenComplete(CheckedAction2<? super T, Throwable, ?> handler) {
        try {
            handler.apply(null, ex);
            return CompletableFuture.completedExceptionally(ex);
        } catch(Throwable ex) {
            return CompletableFuture.completedExceptionally(ex);
        }
    }

    @Override public T get() throws ExecutionException {
        throw new ExecutionException(ex);
    }

    @Override public T getNow() throws CompletionException, InterruptedException {
        throw new CompletionException(ex);
    }

    @Override public void complete(T value, Throwable ex) {
        // ignore
    }

    @Override public <U> IFuture<U> thenApply(CheckedFunction1<? super T, ? extends U, ?> handler) {
        return CompletableFuture.completedExceptionally(ex);
    }

    @Override public IFuture<Void> thenAccept(CheckedAction1<? super T, ?> handler) {
        return CompletableFuture.completedExceptionally(ex);
    }

    @Override public <U> IFuture<U> thenCompose(CheckedFunction1<? super T, ? extends IFuture<U>, ?> handler) {
        return CompletableFuture.completedExceptionally(ex);
    }

    @Override public boolean isDone() {
        return true;
    }

}