package mb.statix.concurrent.actors.futures;

import java.util.Arrays;
import java.util.List;

import org.metaborg.util.functions.CheckedAction1;
import org.metaborg.util.functions.CheckedAction2;
import org.metaborg.util.functions.CheckedFunction1;
import org.metaborg.util.functions.CheckedFunction2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.tuple.Tuple2;
import org.metaborg.util.tuple.Tuple3;
import org.metaborg.util.tuple.Tuple4;

import com.google.common.collect.Lists;

public class AggregateFuture<T> implements IFuture<List<T>> {

    private static final ILogger logger = LoggerUtils.logger(AggregateFuture.class);

    private Object lock = new Object();
    private volatile int remaining;
    private final T[] results;
    private final CompletableFuture<List<T>> result;

    @SafeVarargs public AggregateFuture(IFuture<? extends T>... futures) {
        this(Arrays.asList(futures));
    }

    @SuppressWarnings("unchecked") public AggregateFuture(Iterable<? extends IFuture<? extends T>> futures) {
        final List<? extends IFuture<? extends T>> futureList = Lists.newArrayList(futures);
        final int count = futureList.size();
        this.results = (T[]) new Object[count];
        this.result = new CompletableFuture<>();
        logger.trace("{} initialized with {}: {}", this, count, futureList);

        this.remaining = count;
        for(int i = 0; i < count; i++) {
            final int j = i;
            futureList.get(i).whenComplete((r, ex) -> {
                whenComplete(j, r, ex);
            });
        }
        fireIfComplete();
    }

    private void whenComplete(int i, T r, Throwable ex) {
        // INVARIANT count > 0
        synchronized(lock) {
            if(remaining > 0) {
                if(ex != null) {
                    logger.trace("{} completed {}: exception", this, i);
                    remaining = -1; // count will never be 0 and trigger completion
                } else if(remaining > 0) {
                    logger.trace("{} completed {}: value", this, i);
                    remaining -= 1;
                    results[i] = r;
                }
            }
        }
        if(ex != null) {
            result.completeExceptionally(ex);
        } else {
            fireIfComplete();
        }
    }

    private void fireIfComplete() {
        final List<T> results;
        synchronized(lock) {
            if(remaining == 0) {
                logger.trace("{} done: completed all", this);
                results = Arrays.asList(this.results);
            } else {
                logger.trace("{} open: {} remaining", this, remaining);
                results = null;
            }
        }
        if(results != null) {
            result.complete(results);
        }
    }

    @Override public <U> IFuture<U>
            handle(CheckedFunction2<? super List<T>, Throwable, ? extends U, ? extends Throwable> handler) {
        return result.handle(handler);
    }

    @Override public IFuture<List<T>>
            whenComplete(CheckedAction2<? super List<T>, Throwable, ? extends Throwable> handler) {
        return result.whenComplete(handler);
    }

    @Override public <U> IFuture<U>
            thenApply(CheckedFunction1<? super List<T>, ? extends U, ? extends Throwable> handler) {
        return result.thenApply(handler);
    }

    @Override public IFuture<Void> thenAccept(CheckedAction1<? super List<T>, ? extends Throwable> handler) {
        return result.thenAccept(handler);
    }

    @Override public <U> IFuture<U> thenCompose(
            CheckedFunction1<? super List<T>, ? extends IFuture<? extends U>, ? extends Throwable> handler) {
        return result.thenCompose(handler);
    }

    @Override public <U> IFuture<U> compose(
            CheckedFunction2<? super List<T>, Throwable, ? extends IFuture<? extends U>, ? extends Throwable> handler) {
        return result.compose(handler);
    }

    @Override public boolean isDone() {
        return result.isDone();
    }

    @Override public java.util.concurrent.CompletableFuture<List<T>> asJavaCompletion() {
        return result.asJavaCompletion();
    }

    @SuppressWarnings("unchecked") public static <T1, T2> IFuture<Tuple2<T1, T2>> apply(IFuture<T1> f1,
            IFuture<T2> f2) {
        return new AggregateFuture<>(f1, f2).thenApply(rs -> Tuple2.of((T1) rs.get(0), (T2) rs.get(1)));
    }

    @SuppressWarnings("unchecked") public static <T1, T2, T3> IFuture<Tuple3<T1, T2, T3>> apply(IFuture<T1> f1,
            IFuture<T2> f2, IFuture<T3> f3) {
        return new AggregateFuture<>(f1, f2, f3)
                .thenApply(rs -> Tuple3.of((T1) rs.get(0), (T2) rs.get(1), (T3) rs.get(2)));
    }

    @SuppressWarnings("unchecked") public static <T1, T2, T3, T4> IFuture<Tuple4<T1, T2, T3, T4>> apply(IFuture<T1> f1,
            IFuture<T2> f2, IFuture<T3> f3, IFuture<T4> f4) {
        return new AggregateFuture<>(f1, f2, f3, f4)
                .thenApply(rs -> Tuple4.of((T1) rs.get(0), (T2) rs.get(1), (T3) rs.get(2), (T4) rs.get(3)));
    }

}