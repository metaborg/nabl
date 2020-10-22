package mb.statix.concurrent.actors.futures;

import java.util.concurrent.CompletionException;

import org.metaborg.util.functions.CheckedAction1;
import org.metaborg.util.functions.CheckedAction2;
import org.metaborg.util.functions.CheckedFunction1;
import org.metaborg.util.functions.CheckedFunction2;

class CompletedFuture<T> implements ICompletableFuture<T> {

    private final T result;

    public CompletedFuture(T result) {
        if(result == null) {
            throw new IllegalArgumentException("null value is not supported.");
        }
        this.result = result;
    }

    @Override public <U> IFuture<U> handle(CheckedFunction2<? super T, Throwable, ? extends U, ?> handler) {
        try {
            return CompletableFuture.completedFuture(handler.apply(result, null));
        } catch(Throwable ex) {
            return CompletableFuture.completedExceptionally(ex);
        }
    }

    @Override public IFuture<T> whenComplete(CheckedAction2<? super T, Throwable, ?> handler) {
        try {
            handler.apply(result, null);
            return CompletableFuture.completedFuture(result);
        } catch(Throwable ex) {
            return CompletableFuture.completedExceptionally(ex);
        }
    }

    @Override public T get() {
        return result;
    }

    @Override public T getNow() throws CompletionException, InterruptedException {
        return result;
    }

    @Override public void complete(T value, Throwable ex) {
        // ignore
    }

    @Override public <U> IFuture<U> thenApply(CheckedFunction1<? super T, ? extends U, ?> handler) {
        try {
            return CompletableFuture.completedFuture(handler.apply(result));
        } catch(Throwable ex) {
            return CompletableFuture.completedExceptionally(ex);
        }
    }

    @Override public IFuture<Void> thenAccept(CheckedAction1<? super T, ?> handler) {
        try {
            handler.apply(result);
            return CompletableFuture.completedFuture(null);
        } catch(Throwable ex) {
            return CompletableFuture.completedExceptionally(ex);
        }
    }

    @Override public <U> IFuture<U> thenCompose(CheckedFunction1<? super T, ? extends IFuture<U>, ?> handler) {
        try {
            return handler.apply(result);
        } catch(Throwable ex) {
            return CompletableFuture.completedExceptionally(ex);
        }
    }

    @Override public boolean isDone() {
        return true;
    }

    @Override public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());
    }

}