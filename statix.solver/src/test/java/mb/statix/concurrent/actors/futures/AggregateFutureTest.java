package mb.statix.concurrent.actors.futures;

import static mb.statix.concurrent.actors.futures.CompletableFuture.completedExceptionally;
import static mb.statix.concurrent.actors.futures.CompletableFuture.completedFuture;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

public class AggregateFutureTest {

    @Test public void testAggregateOfNone() {
        final AtomicBoolean marker = new AtomicBoolean(false);
        final AggregateFuture<Integer> af = new AggregateFuture<>();
        af.whenComplete((r, ex) -> {
            marker.set(ex == null && r.isEmpty());
        });
        assertTrue(marker.get());
    }

    @Test public void testAggregateOfOneCompleted() {
        final AtomicBoolean marker = new AtomicBoolean(false);
        final AggregateFuture<Integer> af = new AggregateFuture<>(completedFuture(1));
        af.whenComplete((r, ex) -> {
            marker.set(ex == null && r.size() == 1);
        });
        assertTrue(marker.get());
    }

    @Test public void testAggregateOfOneFailed() {
        final AtomicBoolean marker = new AtomicBoolean(false);
        final AggregateFuture<Integer> af = new AggregateFuture<>(completedExceptionally(new Exception()));
        af.whenComplete((r, ex) -> {
            marker.set(ex != null);
        });
        assertTrue(marker.get());
    }

    @Test public void testAggregateOfOneFailed_Delayed() {
        final AtomicBoolean marker = new AtomicBoolean(false);
        final CompletableFuture<Integer> f = new CompletableFuture<>();
        final AggregateFuture<Integer> af = new AggregateFuture<>(f);
        af.whenComplete((r, ex) -> {
            marker.set(ex != null);
        });
        f.completeExceptionally(new Exception());
        assertTrue(marker.get());
    }

    @Test public void testAggregateOfMultipleCompleted() {
        final AtomicBoolean marker = new AtomicBoolean(false);
        final AggregateFuture<Integer> af = new AggregateFuture<>(completedFuture(1), completedFuture(2));
        af.whenComplete((r, ex) -> {
            marker.set(ex == null && r.size() == 2);
        });
        assertTrue(marker.get());
    }

    @Test public void testAggregateOfMultipleCompleted_Delayed() {
        final AtomicBoolean marker = new AtomicBoolean(false);
        final CompletableFuture<Integer> f1 = new CompletableFuture<>();
        final CompletableFuture<Integer> f2 = new CompletableFuture<>();
        final AggregateFuture<Integer> af = new AggregateFuture<>(f1, f2);
        af.whenComplete((r, ex) -> {
            marker.set(ex == null && r.size() == 2);
        });
        f1.complete(1);
        assertFalse(marker.get());
        f2.complete(2);
        assertTrue(marker.get());
    }

    @Test public void testAggregateOfMultipleFailed() {
        final AtomicBoolean marker = new AtomicBoolean(false);
        final AggregateFuture<Integer> af =
                new AggregateFuture<>(completedExceptionally(new Exception()), completedExceptionally(new Exception()));
        af.whenComplete((r, ex) -> {
            marker.set(ex != null);
        });
        assertTrue(marker.get());
    }

    @Test public void testAggregateOfMultipleFailed_Delayed() {
        final AtomicBoolean marker = new AtomicBoolean(false);
        final CompletableFuture<Integer> f1 = new CompletableFuture<>();
        final CompletableFuture<Integer> f2 = new CompletableFuture<>();
        final AggregateFuture<Integer> af = new AggregateFuture<>(f1, f2);
        af.whenComplete((r, ex) -> {
            marker.set(ex != null);
        });
        assertFalse(marker.get());
        f2.completeExceptionally(new Exception());
        assertTrue(marker.get());
        f1.completeExceptionally(new Exception());
        assertTrue(marker.get());
    }

    @Test public void testAggregateOfMultipleCompletedAndFailed() {
        final AtomicBoolean marker = new AtomicBoolean(false);
        final AggregateFuture<Integer> af =
                new AggregateFuture<>(completedFuture(42), completedExceptionally(new Exception()));
        af.whenComplete((r, ex) -> {
            marker.set(ex != null);
        });
        assertTrue(marker.get());
    }

    @Test public void testAggregateOfMultipleCompletedAndFailed_Delayed() {
        final AtomicBoolean valMarker = new AtomicBoolean(false);
        final AtomicBoolean marker = new AtomicBoolean(false);
        final CompletableFuture<Integer> f1 = new CompletableFuture<>();
        final CompletableFuture<Integer> f2 = new CompletableFuture<>();
        final AggregateFuture<Integer> af = new AggregateFuture<>(f1, f2);
        af.whenComplete((r, ex) -> {
            valMarker.set(r != null);
            marker.set(ex != null);
        });
        f2.complete(42);
        assertFalse(valMarker.get());
        assertFalse(marker.get());
        f1.completeExceptionally(new Exception());
        assertFalse(valMarker.get());
        assertTrue(marker.get());
    }

}