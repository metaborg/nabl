package mb.statix.concurrent.actors.futures;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

public class JavaCompletableFutureTest {

    @Test public void testWhenComplete_Complete() {
        final AtomicBoolean marker = new AtomicBoolean(false);
        final CompletableFuture<Integer> cf = new CompletableFuture<>();
        cf.whenComplete((r, ex) -> {
            marker.set(true);
        });
        cf.complete(42);
        assertTrue(marker.get());
    }

    @Test public void testWhenComplete_Fail() {
        final AtomicBoolean marker = new AtomicBoolean(false);
        final CompletableFuture<Integer> cf = new CompletableFuture<>();
        cf.whenComplete((r, ex) -> {
            marker.set(true);
        });
        cf.completeExceptionally(new Exception());
        assertTrue(marker.get());
    }

    @Test public void testComplete_WhenComplete() {
        final AtomicBoolean marker = new AtomicBoolean(false);
        final CompletableFuture<Integer> cf = new CompletableFuture<>();
        cf.complete(42);
        cf.whenComplete((r, ex) -> {
            marker.set(true);
        });
        assertTrue(marker.get());
    }

    @Test public void testFail_WhenComplete() {
        final AtomicBoolean marker = new AtomicBoolean(false);
        final CompletableFuture<Integer> cf = new CompletableFuture<>();
        cf.completeExceptionally(new Exception());
        cf.whenComplete((r, ex) -> {
            marker.set(true);
        });
        assertTrue(marker.get());
    }

}