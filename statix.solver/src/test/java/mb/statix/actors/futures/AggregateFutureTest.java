package mb.statix.actors.futures;

import static mb.statix.concurrent.actors.futures.CompletableFuture.completedExceptionally;
import static mb.statix.concurrent.actors.futures.CompletableFuture.completedFuture;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import mb.statix.concurrent.actors.futures.AggregateFuture;

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

    @Test public void testAggregateOfMultipleCompleted() {
        final AtomicBoolean marker = new AtomicBoolean(false);
        final AggregateFuture<Integer> af = new AggregateFuture<>(completedFuture(1), completedFuture(2));
        af.whenComplete((r, ex) -> {
            marker.set(ex == null && r.size() == 2);
        });
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

    @Test public void testAggregateOfMultipleCompletedAndFailed() {
        final AtomicBoolean marker = new AtomicBoolean(false);
        final AggregateFuture<Integer> af =
                new AggregateFuture<>(completedFuture(42), completedExceptionally(new Exception()));
        af.whenComplete((r, ex) -> {
            marker.set(ex != null);
        });
        assertTrue(marker.get());
    }

}