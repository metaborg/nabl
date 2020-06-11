package mb.statix.solver.concurrent.util;

import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

public class CompletableFuture<T> implements ICompletable<T>, IFuture<T> {

    private static final ILogger logger = LoggerUtils.logger(CompletableFuture.class);

    private final java.util.concurrent.CompletableFuture<T> future;

    public CompletableFuture() {
        this(new java.util.concurrent.CompletableFuture<>());
    }

    private CompletableFuture(java.util.concurrent.CompletableFuture<T> future) {
        this.future = future;
    }

    @Override public <U> IFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> handler) {
        return new CompletableFuture<>(future.handle((t, ex) -> {
            return handler.apply(t, ex);
        }));
    }

    @Override public IFuture<T> whenComplete(BiConsumer<? super T, Throwable> handler) {
        return new CompletableFuture<>(future.whenComplete((t, ex) -> {
            handler.accept(t, ex);
        }));
    }

    @Override public T get() throws ExecutionException, InterruptedException {
        return future.get();
    }

    @Override public void complete(T value) {
        if(!future.complete(value)) {
            logger.error("FIXME: future completion failed.");
        }
    }

    @Override public void completeExceptionally(Throwable ex) {
        if(!future.completeExceptionally(ex)) {
            logger.error("FIXME: future completion failed.");
        }
    }

    @Override public boolean isDone() {
        return future.isDone();
    }

}