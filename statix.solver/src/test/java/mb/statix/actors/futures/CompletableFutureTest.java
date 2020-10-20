package mb.statix.actors.futures;

import static org.junit.Assert.assertFalse;
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

    // complete + thenCompose

    @Test public void testComplete_ComposeComplete() {
        final AtomicBoolean marker = new AtomicBoolean(false);
        final CompletableFuture<Integer> cf = new CompletableFuture<>();
        cf.complete(42);
        cf.thenCompose(r -> {
            return CompletableFuture.completedFuture(r);
        }).whenComplete((r, ex) -> {
            marker.set(r != null);
        });
        assertTrue(marker.get());
    }

    @Test public void testComposeComplete_Complete() {
        final AtomicBoolean marker = new AtomicBoolean(false);
        final CompletableFuture<Integer> cf = new CompletableFuture<>();
        cf.thenCompose(r -> {
            return CompletableFuture.completedFuture(r);
        }).whenComplete((r, ex) -> {
            marker.set(r != null);
        });
        assertFalse(marker.get());
        cf.complete(42);
        assertTrue(marker.get());
    }

    @Test public void testComplete_ComposeFail() {
        final AtomicBoolean marker = new AtomicBoolean(false);
        final CompletableFuture<Integer> cf = new CompletableFuture<>();
        cf.complete(42);
        cf.thenCompose(r -> {
            return CompletableFuture.completedExceptionally(new Exception());
        }).whenComplete((r, ex) -> {
            marker.set(ex != null);
        });
        assertTrue(marker.get());
    }

    @Test public void testComposeFail_Complete() {
        final AtomicBoolean marker = new AtomicBoolean(false);
        final CompletableFuture<Integer> cf = new CompletableFuture<>();
        cf.thenCompose(r -> {
            return CompletableFuture.completedExceptionally(new Exception());
        }).whenComplete((r, ex) -> {
            marker.set(ex != null);
        });
        assertFalse(marker.get());
        cf.complete(42);
        assertTrue(marker.get());
    }

    @Test public void testFail_ComposeComplete() {
        final AtomicBoolean marker = new AtomicBoolean(false);
        final CompletableFuture<Integer> cf = new CompletableFuture<>();
        cf.completeExceptionally(new Exception());
        cf.thenCompose(r -> {
            return CompletableFuture.completedFuture(r);
        }).whenComplete((r, ex) -> {
            marker.set(ex != null);
        });
        assertTrue(marker.get());
    }

    @Test public void testComposeComplete_Fail() {
        final AtomicBoolean marker = new AtomicBoolean(false);
        final CompletableFuture<Integer> cf = new CompletableFuture<>();
        cf.thenCompose(r -> {
            return CompletableFuture.completedFuture(r);
        }).whenComplete((r, ex) -> {
            marker.set(ex != null);
        });
        assertFalse(marker.get());
        cf.completeExceptionally(new Exception());
        assertTrue(marker.get());
    }

    // complete + thenApply

    @Test public void testThenApply_Complete() {
        final AtomicBoolean marker1 = new AtomicBoolean(false);
        final AtomicBoolean marker2 = new AtomicBoolean(false);
        final CompletableFuture<Integer> cf = new CompletableFuture<>();
        cf.thenApply(r -> {
            marker1.set(true);
            return r;
        }).whenComplete((r, ex) -> {
            marker2.set(r != null);
        });
        cf.complete(42);
        assertTrue(marker1.get());
        assertTrue(marker2.get());
    }

    @Test public void testThenApply_Fail() {
        final AtomicBoolean marker1 = new AtomicBoolean(false);
        final AtomicBoolean marker2 = new AtomicBoolean(false);
        final CompletableFuture<Integer> cf = new CompletableFuture<>();
        cf.thenApply(r -> {
            marker1.set(true);
            return r;
        }).whenComplete((r, ex) -> {
            marker2.set(ex != null);
        });
        cf.completeExceptionally(new Exception());
        assertFalse(marker1.get());
        assertTrue(marker2.get());
    }

    @Test public void testComplete_ThenApply() {
        final AtomicBoolean marker1 = new AtomicBoolean(false);
        final AtomicBoolean marker2 = new AtomicBoolean(false);
        final CompletableFuture<Integer> cf = new CompletableFuture<>();
        cf.complete(42);
        cf.thenApply(r -> {
            marker1.set(true);
            return r;
        }).whenComplete((r, ex) -> {
            marker2.set(r != null);
        });
        assertTrue(marker1.get());
        assertTrue(marker2.get());
    }

    @Test public void testFail_ThenApply() {
        final AtomicBoolean marker1 = new AtomicBoolean(false);
        final AtomicBoolean marker2 = new AtomicBoolean(false);
        final CompletableFuture<Integer> cf = new CompletableFuture<>();
        cf.completeExceptionally(new Exception());
        cf.thenApply(r -> {
            marker1.set(true);
            return r;
        }).whenComplete((r, ex) -> {
            marker2.set(ex != null);
        });
        assertFalse(marker1.get());
        assertTrue(marker2.get());
    }

}