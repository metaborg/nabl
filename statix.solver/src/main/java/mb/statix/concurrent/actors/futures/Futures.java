package mb.statix.concurrent.actors.futures;

import java.util.Iterator;

import org.metaborg.util.functions.CheckedFunction1;
import org.metaborg.util.functions.CheckedFunction2;

public final class Futures {

    private Futures() {
    }

    public static <T, U> IFuture<U> reduce(U initial, Iterable<T> items, CheckedFunction2<U, T, IFuture<U>, ? extends Throwable> f) {
        return reduce(initial, items.iterator(), f);
    }

    private static <T, U> IFuture<U> reduce(U initial, Iterator<T> items, CheckedFunction2<U, T, IFuture<U>, ? extends Throwable> f) {
        if(items.hasNext()) {
            try {
                return f.apply(initial, items.next()).thenCompose(next -> reduce(next, items, f));
            } catch(Throwable ex) {
                return CompletableFuture.completedExceptionally(ex);
            }
        } else {
            return CompletableFuture.completedFuture(initial);
        }
    }


    public static <T> IFuture<Boolean> noneMatch(Iterable<T> items,
            CheckedFunction1<T, IFuture<Boolean>, InterruptedException> p) {
        return noneMatch(items.iterator(), p);
    }

    private static <T> IFuture<Boolean> noneMatch(Iterator<T> items, CheckedFunction1<T, IFuture<Boolean>, ? extends Throwable> p) {
        if(items.hasNext()) {
            try {
                return p.apply(items.next()).thenCompose(match -> {
                    return match ? CompletableFuture.completedFuture(false) : noneMatch(items, p);
                });
            } catch(Throwable ex) {
                return CompletableFuture.completedExceptionally(ex);
            }
        } else {
            return CompletableFuture.completedFuture(true);
        }
    }

}