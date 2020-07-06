package mb.statix.concurrent.actors.futures;

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

    @Override public <U> IFuture<U> handle(CheckedFunction2<? super T, Throwable, ? extends U, ?> handler) {
        final CompletableFuture<U> result = new CompletableFuture<>();
        future.whenComplete((r, ex) -> {
            if(ex != null) {
                result.completeExceptionally(ex);
            } else {
                try {
                    result.completeValue(handler.apply(r, ex));
                } catch(Throwable inner) {
                    result.completeExceptionally(inner);
                }
            }
        });
        return result;
    }

    @Override public IFuture<T> whenComplete(CheckedAction2<? super T, Throwable, ?> handler) {
        final CompletableFuture<T> result = new CompletableFuture<>();
        future.whenComplete((r, ex) -> {
            if(ex != null) {
                result.completeExceptionally(ex);
            } else {
                try {
                    handler.apply(r, ex);
                    result.completeValue(r);
                } catch(Throwable inner) {
                    result.completeExceptionally(inner);
                }
            }
        });
        return result;
    }

    @Override public T get() throws ExecutionException, InterruptedException {
        return future.get();
    }

    @Override public void complete(T value, Throwable ex) {
        if(ex != null) {
            future.completeExceptionally(ex);
        } else {
            future.complete(value);
        }
    }

    @Override public <U> IFuture<U> thenApply(CheckedFunction1<? super T, ? extends U, ?> handler) {
        final CompletableFuture<U> result = new CompletableFuture<>();
        future.whenComplete((r, ex) -> {
            if(ex != null) {
                result.completeExceptionally(ex);
            } else {
                try {
                    result.completeValue(handler.apply(r));
                } catch(Throwable inner) {
                    result.completeExceptionally(inner);
                }
            }
        });
        return result;
    }

    @Override public IFuture<Void> thenAccept(CheckedAction1<? super T, ?> handler) {
        final CompletableFuture<Void> result = new CompletableFuture<>();
        future.whenComplete((r, ex) -> {
            if(ex != null) {
                result.completeExceptionally(ex);
            } else {
                try {
                    handler.apply(r);
                    result.completeValue(null);
                } catch(Throwable inner) {
                    result.completeExceptionally(inner);
                }
            }
        });
        return result;
    }

    @Override public <U> IFuture<U> thenCompose(CheckedFunction1<? super T, ? extends IFuture<U>, ?> handler) {
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

    @Override public boolean isDone() {
        return future.isDone();
    }

    public static <T> IFuture<T> of(T value) {
        final java.util.concurrent.CompletableFuture<T> future = new java.util.concurrent.CompletableFuture<>();
        future.complete(value);
        return new CompletableFuture<>(future);
    }

    public static <T> IFuture<T> completedExceptionally(Throwable ex) {
        final java.util.concurrent.CompletableFuture<T> future = new java.util.concurrent.CompletableFuture<>();
        future.completeExceptionally(ex);
        return new CompletableFuture<>(future);
    }

}