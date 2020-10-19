package mb.statix.concurrent.actors.futures;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import org.metaborg.util.functions.CheckedAction1;
import org.metaborg.util.functions.CheckedAction2;
import org.metaborg.util.functions.CheckedFunction1;
import org.metaborg.util.functions.CheckedFunction2;

public interface IFuture<T> {

    <U> IFuture<U> thenApply(CheckedFunction1<? super T, ? extends U, ?> handler);

    IFuture<Void> thenAccept(CheckedAction1<? super T, ?> handler);

    <U> IFuture<U> thenCompose(CheckedFunction1<? super T, ? extends IFuture<U>, ?> handler);

    <U> IFuture<U> handle(CheckedFunction2<? super T, Throwable, ? extends U, ?> handler);

    IFuture<T> whenComplete(CheckedAction2<? super T, Throwable, ?> handler);

    T get() throws ExecutionException, InterruptedException;

    T getNow() throws CompletionException, InterruptedException;

    boolean isDone();

}