package mb.statix.concurrent.actors.futures;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import org.metaborg.util.functions.CheckedAction1;
import org.metaborg.util.functions.CheckedAction2;
import org.metaborg.util.functions.CheckedFunction1;
import org.metaborg.util.functions.CheckedFunction2;

public class CompletableFuture<T> implements ICompletableFuture<T> {

    private final java.util.concurrent.CompletableFuture<T> future;

    public CompletableFuture() {
        this(new java.util.concurrent.CompletableFuture<>());
    }

    public CompletableFuture(java.util.concurrent.CompletableFuture<T> future) {
        this.future = future;
    }

    @Override public <U> IFuture<U> handle(CheckedFunction2<? super T, Throwable, ? extends U, ? extends Throwable> handler) {
        final CompletableFuture<U> result = new CompletableFuture<>();
        future.whenComplete((r, ex) -> {
            try {
                result.complete(handler.apply(r, ex));
            } catch(Throwable inner) {
                result.completeExceptionally(inner);
            }
        });
        return result;
    }

    @Override public IFuture<T> whenComplete(CheckedAction2<? super T, Throwable, ? extends Throwable> handler) {
        final CompletableFuture<T> result = new CompletableFuture<>();
        future.whenComplete((r, ex) -> {
            try {
                handler.apply(r, ex);
                result.complete(r, ex);
            } catch(Throwable inner) {
                result.completeExceptionally(inner);
            }
        });
        return result;
    }

    /**
     * Get the result of this future. Wait if the result if not yet available.
     */
    @Override public T get() throws ExecutionException, InterruptedException {
        return future.get();
    }

    /**
     * Get the result of this future, or null if it has no result yet.
     */
    @Override public T getNow() throws CompletionException, InterruptedException {
        return future.getNow(null);
    }

    @Override public void complete(T value, Throwable ex) {
        if(ex != null) {
            future.completeExceptionally(ex);
        } else {
            if(value == null) {
                throw new IllegalArgumentException("null values are not supported");
            }
            future.complete(value);
        }
    }

    @Override public <U> IFuture<U> thenApply(CheckedFunction1<? super T, ? extends U, ? extends Throwable> handler) {
        final CompletableFuture<U> result = new CompletableFuture<>();
        future.whenComplete((r, ex) -> {
            if(ex != null) {
                result.completeExceptionally(ex);
            } else {
                try {
                    result.complete(handler.apply(r));
                } catch(Throwable inner) {
                    result.completeExceptionally(inner);
                }
            }
        });
        return result;
    }

    @Override public IFuture<Void> thenAccept(CheckedAction1<? super T, ? extends Throwable> handler) {
        final CompletableFuture<Void> result = new CompletableFuture<>();
        future.whenComplete((r, ex) -> {
            if(ex != null) {
                result.completeExceptionally(ex);
            } else {
                try {
                    handler.apply(r);
                    result.complete(null);
                } catch(Throwable inner) {
                    result.completeExceptionally(inner);
                }
            }
        });
        return result;
    }

    @Override public <U> IFuture<U> thenCompose(CheckedFunction1<? super T, ? extends IFuture<U>, ? extends Throwable> handler) {
        final CompletableFuture<U> result = new CompletableFuture<>();
        future.whenComplete((r, ex) -> {
            if(ex != null) {
                result.completeExceptionally(ex);
            } else {
                try {
                    handler.apply(r).whenComplete(result::complete);
                } catch(Throwable inner) {
                    result.completeExceptionally(inner);
                }
            }
        });
        return result;
    }

    @Override public <U> IFuture<U>
            compose(CheckedFunction2<? super T, Throwable, ? extends IFuture<? extends U>, ?> handler) {
        final CompletableFuture<U> result = new CompletableFuture<>();
        future.whenComplete((r, ex) -> {
            try {
                handler.apply(r, ex).whenComplete(result::complete);
            } catch(Throwable inner) {
                result.completeExceptionally(inner);
            }
        });
        return result;
    }

    @Override public boolean isDone() {
        return future.isDone();
    }

    public static <T> IFuture<T> completedFuture(T value) {
        return new CompletedFuture<>(value);
    }

    public static <T> IFuture<T> completedExceptionally(Throwable ex) {
        return new CompletedExceptionallyFuture<>(ex);
    }

    @Override public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());
    }

}
