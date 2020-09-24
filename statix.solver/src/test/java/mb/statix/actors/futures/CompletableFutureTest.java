package mb.statix.actors.futures;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import mb.statix.concurrent.actors.futures.CompletableFuture;

public class CompletableFutureTest {

    // complete + whenComplete

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

    // complete + handle

    @Test public void testHandle_Complete() {
        final AtomicBoolean marker = new AtomicBoolean(false);
        final CompletableFuture<Integer> cf = new CompletableFuture<>();
        cf.handle((r, ex) -> {
            marker.set(true);
            return null;
        });
        cf.complete(42);
        assertTrue(marker.get());
    }

    @Test public void testHandle_Fail() {
        final AtomicBoolean marker = new AtomicBoolean(false);
        final CompletableFuture<Integer> cf = new CompletableFuture<>();
        cf.handle((r, ex) -> {
            marker.set(true);
            return null;
        });
        cf.completeExceptionally(new Exception());
        assertTrue(marker.get());
    }

    @Test public void testComplete_Handle() {
        final AtomicBoolean marker = new AtomicBoolean(false);
        final CompletableFuture<Integer> cf = new CompletableFuture<>();
        cf.complete(42);
        cf.handle((r, ex) -> {
            marker.set(true);
            return null;
        });
        assertTrue(marker.get());
    }

    @Test public void testFail_Handle() {
        final AtomicBoolean marker = new AtomicBoolean(false);
        final CompletableFuture<Integer> cf = new CompletableFuture<>();
        cf.completeExceptionally(new Exception());
        cf.handle((r, ex) -> {
            marker.set(true);
            return null;
        });
        assertTrue(marker.get());
    }

}