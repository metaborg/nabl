package mb.statix.concurrent.actors.futures;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.metaborg.util.functions.CheckedAction1;
import org.metaborg.util.functions.CheckedAction2;
import org.metaborg.util.functions.CheckedFunction1;
import org.metaborg.util.functions.CheckedFunction2;

import com.google.common.collect.Lists;

public class AggregateFuture<T> implements IFuture<List<T>> {

    private final CompletableFuture<List<T>> result;
    private final List<T> results;
    private final AtomicInteger count;

    public AggregateFuture(Iterable<IFuture<T>> futures) {
        final List<IFuture<T>> futureList = Lists.newArrayList(futures);
        final int count = futureList.size();
        this.result = new CompletableFuture<>();
        this.results = Lists.newArrayListWithExpectedSize(count);
        // set the count before calling whenComplete, as whenComplete is triggered
        // immediately for futures that already completed
        this.count = new AtomicInteger(count);
        for(int i = 0; i < count; i++) {
            final int j = i;
            futureList.get(i).whenComplete((r, ex) -> {
                whenComplete(j, r, ex);
            });
        }
        fireIfComplete();
    }

    private synchronized void whenComplete(int i, T r, Throwable ex) {
        // INVARIANT count > 0
        if(ex != null) {
            count.set(-1); // count will never be 0 and trigger completion
            result.completeExceptionally(ex);
        } else {
            count.decrementAndGet();
            results.set(i, r);
        }
        fireIfComplete();
    }

    private synchronized void fireIfComplete() {
        if(count.get() == 0) {
            result.completeValue(results);
        }
    }

    @Override public <U> IFuture<U> handle(CheckedFunction2<? super List<T>, Throwable, ? extends U, ?> handler) {
        return result.handle(handler);
    }

    @Override public IFuture<List<T>> whenComplete(CheckedAction2<? super List<T>, Throwable, ?> handler) {
        return result.whenComplete(handler);
    }

    @Override public List<T> get() throws ExecutionException, InterruptedException {
        return result.get();
    }

    @Override public <U> IFuture<U> thenApply(CheckedFunction1<? super List<T>, ? extends U, ?> handler) {
        return result.thenApply(handler);
    }

    @Override public IFuture<Void> thenAccept(CheckedAction1<? super List<T>, ?> handler) {
        return result.thenAccept(handler);
    }

    @Override public <U> IFuture<U> thenCompose(CheckedFunction1<? super List<T>, ? extends IFuture<U>, ?> handler) {
        return result.thenCompose(handler);
    }

    @Override public boolean isDone() {
        return result.isDone();
    }

}