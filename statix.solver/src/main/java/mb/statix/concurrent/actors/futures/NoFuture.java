package mb.statix.concurrent.actors.futures;

import org.metaborg.util.functions.CheckedAction1;
import org.metaborg.util.functions.CheckedAction2;
import org.metaborg.util.functions.CheckedFunction1;
import org.metaborg.util.functions.CheckedFunction2;

class NoFuture<T> implements ICompletableFuture<T> {

    public NoFuture() {
    }

    /////////////////////////////////////////////////////////////////////
    // ICompletable
    /////////////////////////////////////////////////////////////////////

    @SuppressWarnings("unused") @Override public void complete(T value, Throwable ex) {
        // ignore
    }


    /////////////////////////////////////////////////////////////////////
    // IFuture
    /////////////////////////////////////////////////////////////////////

    @SuppressWarnings("unused") @Override public <U> IFuture<U>
            handle(CheckedFunction2<? super T, Throwable, ? extends U, ? extends Throwable> handler) {
        return CompletableFuture.noFuture();
    }

    @SuppressWarnings("unused") @Override public IFuture<T>
            whenComplete(CheckedAction2<? super T, Throwable, ? extends Throwable> handler) {
        return CompletableFuture.noFuture();
    }

    @SuppressWarnings("unused") @Override public <U> IFuture<U>
            thenApply(CheckedFunction1<? super T, ? extends U, ? extends Throwable> handler) {
        return CompletableFuture.noFuture();
    }

    @SuppressWarnings("unused") @Override public IFuture<Void>
            thenAccept(CheckedAction1<? super T, ? extends Throwable> handler) {
        return CompletableFuture.noFuture();
    }

    @SuppressWarnings("unused") @Override public <U> IFuture<U>
            thenCompose(CheckedFunction1<? super T, ? extends IFuture<? extends U>, ? extends Throwable> handler) {
        return CompletableFuture.noFuture();
    }

    @SuppressWarnings("unused") @Override public <U> IFuture<U> compose(
            CheckedFunction2<? super T, Throwable, ? extends IFuture<? extends U>, ? extends Throwable> handler) {
        return CompletableFuture.noFuture();
    }

    @Override public boolean isDone() {
        return false;
    }

    @Override public java.util.concurrent.CompletableFuture<T> asJavaCompletion() {
        final java.util.concurrent.CompletableFuture<T> future = new java.util.concurrent.CompletableFuture<>();
        return future;
    }

    @Override public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());
    }

}