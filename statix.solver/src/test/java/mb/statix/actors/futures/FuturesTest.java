package mb.statix.actors.futures;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import io.usethesource.capsule.Set;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.Futures;
import mb.statix.concurrent.actors.futures.IFuture;

public class FuturesTest {

    @Test public void testReduceEmpty() {
        final AtomicBoolean marker = new AtomicBoolean(false);
        IFuture<Set.Immutable<Integer>> result = Futures.reduce(Set.Immutable.of(), Arrays.<Integer>asList(),
                (u, t) -> CompletableFuture.completedFuture(u.__insert(t)));
        result.whenComplete((r, ex) -> {
            marker.set(r != null && r.isEmpty());
        });
        assertTrue(marker.get());
    }

    @Test public void testReduceSingle() {
        final AtomicBoolean marker = new AtomicBoolean(false);
        IFuture<Set.Immutable<Integer>> result = Futures.reduce(Set.Immutable.of(), Arrays.asList(1),
                (u, t) -> CompletableFuture.completedFuture(u.__insert(t)));
        result.whenComplete((r, ex) -> {
            marker.set(r != null && r.size() == 1);
        });
        assertTrue(marker.get());
    }

    @Test public void testReduceMultiple() {
        final AtomicBoolean marker = new AtomicBoolean(false);
        IFuture<Set.Immutable<Integer>> result = Futures.reduce(Set.Immutable.of(), Arrays.asList(1, 1, 2, 3),
                (u, t) -> CompletableFuture.completedFuture(u.__insert(t)));
        result.whenComplete((r, ex) -> {
            marker.set(r != null && r.size() == 3);
        });
        assertTrue(marker.get());
    }

    @Test public void testReduceMultiple_Fail() {
        final AtomicBoolean marker = new AtomicBoolean(false);
        IFuture<Set.Immutable<Integer>> result = Futures.reduce(Set.Immutable.of(), Arrays.asList(1, 1, 2, 3),
                (u, t) -> t % 2 == 0 ? CompletableFuture.completedExceptionally(new Exception())
                        : CompletableFuture.completedFuture(u.__insert(t)));
        result.whenComplete((r, ex) -> {
            marker.set(ex != null);
        });
        assertTrue(marker.get());
    }

    // noneMatch

    @Test public void testNoneMatchEmpty() {
        final AtomicBoolean marker = new AtomicBoolean(false);
        IFuture<Boolean> result =
                Futures.noneMatch(Arrays.<Integer>asList(), t -> CompletableFuture.completedFuture(t % 2 == 0));
        result.whenComplete((r, ex) -> {
            marker.set(r != null && r);
        });
        assertTrue(marker.get());
    }

    @Test public void testNoneMatchSingle() {
        final AtomicBoolean marker = new AtomicBoolean(false);
        IFuture<Boolean> result =
                Futures.noneMatch(Arrays.asList(1), t -> CompletableFuture.completedFuture(t % 2 == 0));
        result.whenComplete((r, ex) -> {
            marker.set(r != null && r);
        });
        assertTrue(marker.get());
    }

    @Test public void testNoneMatchMultiple() {
        final AtomicBoolean marker = new AtomicBoolean(false);
        IFuture<Boolean> result =
                Futures.noneMatch(Arrays.asList(1, 1, 2, 3), t -> CompletableFuture.completedFuture(t % 2 == 0));
        result.whenComplete((r, ex) -> {
            marker.set(r != null && !r);
        });
        assertTrue(marker.get());
    }

    @Test public void testNoneMatchMultiple_Fail() {
        final AtomicBoolean marker = new AtomicBoolean(false);
        IFuture<Boolean> result = Futures.noneMatch(Arrays.asList(1, 1, 2, 3), t -> t % 2 == 0
                ? CompletableFuture.completedExceptionally(new Exception()) : CompletableFuture.completedFuture(false));
        result.whenComplete((r, ex) -> {
            marker.set(ex != null);
        });
        assertTrue(marker.get());
    }
}