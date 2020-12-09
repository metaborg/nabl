package mb.statix.concurrent.actors.futures;

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

    /////////////////////////////////////////////////////////////////////
    // ICompletable
    /////////////////////////////////////////////////////////////////////

    @Override public void complete(T value, Throwable ex) {
        // ignore
    }

    /////////////////////////////////////////////////////////////////////
    // IFuture
    /////////////////////////////////////////////////////////////////////

    @Override public <U> IFuture<U>
            handle(CheckedFunction2<? super T, Throwable, ? extends U, ? extends Throwable> handler) {
        try {
            return CompletableFuture.completedFuture(handler.apply(null, ex));
        } catch(Throwable ex) {
            return CompletableFuture.completedExceptionally(ex);
        }
    }

    @Override public IFuture<T> whenComplete(CheckedAction2<? super T, Throwable, ? extends Throwable> handler) {
        try {
            handler.apply(null, ex);
            return CompletableFuture.completedExceptionally(ex);
        } catch(Throwable ex) {
            return CompletableFuture.completedExceptionally(ex);
        }
    }

    @Override public <U> IFuture<U> thenApply(CheckedFunction1<? super T, ? extends U, ? extends Throwable> handler) {
        return CompletableFuture.completedExceptionally(ex);
    }

    @Override public IFuture<Void> thenAccept(CheckedAction1<? super T, ? extends Throwable> handler) {
        return CompletableFuture.completedExceptionally(ex);
    }

    @Override public <U> IFuture<U>
            thenCompose(CheckedFunction1<? super T, ? extends IFuture<? extends U>, ? extends Throwable> handler) {
        return CompletableFuture.completedExceptionally(ex);
    }

    @SuppressWarnings("unchecked") @Override public <U> IFuture<U> compose(
            CheckedFunction2<? super T, Throwable, ? extends IFuture<? extends U>, ? extends Throwable> handler) {
        try {
            return (IFuture<U>) handler.apply(null, ex);
        } catch(Throwable inner) {
            return CompletableFuture.completedExceptionally(inner);
        }
    }

    @Override public boolean isDone() {
        return true;
    }

    @Override public java.util.concurrent.CompletableFuture<T> asJavaCompletion() {
        final java.util.concurrent.CompletableFuture<T> future = new java.util.concurrent.CompletableFuture<>();
        future.completeExceptionally(ex);
        return future;
    }

    @Override public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());
    }

}