package mb.p_raffrayi.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.junit.Test;
import org.metaborg.util.Ref;
import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.ICompletable;
import org.metaborg.util.future.ICompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.unit.Unit;

import io.usethesource.capsule.Set;
import mb.p_raffrayi.impl.diff.IDifferContext;
import mb.p_raffrayi.impl.diff.ScopeGraphDiffer;
import mb.p_raffrayi.impl.diff.StaticDifferContext;
import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.diff.Edge;
import mb.scopegraph.oopsla20.diff.ScopeGraphDiff;
import mb.scopegraph.oopsla20.reference.ScopeGraph;

public class ScopeGraphDifferTest extends BaseDifferTest {

    private final String s0o = "s0o";
    private final String s0n = "s0n";

    private final String s1o = "s1o";
    private final String s1n = "s1n";

    private final String s2o = "s2o";
    private final String s2n = "s2n";

    private final String s3o = "s3o";
    private final String s3n = "s3n";

    private final Integer l1 = 1;

    private final Set.Immutable<Integer> edgeLabels = CapsuleUtil.immutableSet(l1);

    @Test public void testAdded() {
        // @formatter:off
        final IScopeGraph.Immutable<String, Integer, List<String>> currentGraph = ScopeGraph.Immutable.<String, Integer, List<String>>of()
            .addEdge(s0n, l1, s1n)
            ;
        // @formatter:on

        final TestDifferContext<String, Integer, List<String>> currentContext =
                new TestDifferContext<>(new StaticDifferContext<>(currentGraph, CapsuleUtil.immutableSet(s0n, s1n),
                        TestDifferDataOps.instance));
        final IDifferContext<String, Integer, List<String>> previousContext = new StaticDifferContext<>(
                ScopeGraph.Immutable.of(), CapsuleUtil.immutableSet(),  TestDifferDataOps.instance);

        final ScopeGraphDiffer<String, Integer, List<String>> differ =
                new ScopeGraphDiffer<>(currentContext, previousContext, TestDifferOps.instance, edgeLabels);

        final Ref<ScopeGraphDiff<String, Integer, List<String>>> result = new Ref<>();
        differ.diff(Arrays.asList(s0n), Arrays.asList(s0o)).thenAccept(result::set);
        differ.typeCheckerFinished();

        while(!currentContext.propagatedAll()) {
            assertNull("Differ completed too early.", result.get());
            currentContext.next();
        }

        assertTrue("Differ didn't complete", result.get() != null);
        assertEquals(1, result.get().added().edges().size());

        assertTrue("First edge not added", result.get().added().edges().contains(new Edge<>(s0n, l1, s1n)));
    }

    @Test public void testTransitiveAdded() {
        // @formatter:off
        final IScopeGraph.Immutable<String, Integer, List<String>> currentGraph = ScopeGraph.Immutable.<String, Integer, List<String>>of()
            .addEdge(s0n, l1, s1n)
            .addEdge(s1n, l1, s2n)
            ;
        // @formatter:on

        final TestDifferContext<String, Integer, List<String>> currentContext =
                new TestDifferContext<>(new StaticDifferContext<>(currentGraph, CapsuleUtil.toSet(s0n, s1n, s2n), TestDifferDataOps.instance));
        final IDifferContext<String, Integer, List<String>> previousContext =
                new StaticDifferContext<>(ScopeGraph.Immutable.of(), CapsuleUtil.immutableSet(),  TestDifferDataOps.instance);

        final ScopeGraphDiffer<String, Integer, List<String>> differ =
                new ScopeGraphDiffer<>(currentContext, previousContext, TestDifferOps.instance, edgeLabels);

        final Ref<ScopeGraphDiff<String, Integer, List<String>>> result = new Ref<>();
        differ.diff(Arrays.asList(s0n), Arrays.asList(s0o)).thenAccept(result::set);
        differ.typeCheckerFinished();

        while(!currentContext.propagatedAll()) {
            assertNull("Differ completed too early.", result.get());
            currentContext.next();
        }

        assertNotNull("Differ didn't complete", result.get());
        assertEquals(2, result.get().added().edges().size());

        assertTrue("First edge not added", result.get().added().edges().contains(new Edge<>(s0n, l1, s1n)));
        assertTrue("Second edge not added", result.get().added().edges().contains(new Edge<>(s1n, l1, s2n)));

    }

    @Test public void testAddedFromData() {
        // @formatter:off
        final IScopeGraph.Immutable<String, Integer, List<String>> currentGraph = ScopeGraph.Immutable.<String, Integer, List<String>>of()
            .addEdge(s0n, l1, s1n)
            .addEdge(s2n, l1, s3n)
            .setDatum(s1n, Arrays.asList(s2n, s3n))
            .setDatum(s2n, Arrays.asList(s2n, s3n))
            ;
        // @formatter:on

        final TestDifferContext<String, Integer, List<String>> currentContext =
                new TestDifferContext<>(new StaticDifferContext<>(currentGraph, CapsuleUtil.toSet(s0n, s1n, s2n, s3n),
                        TestDifferDataOps.instance));
        final IDifferContext<String, Integer, List<String>> previousContext = new StaticDifferContext<>(
                ScopeGraph.Immutable.of(), CapsuleUtil.immutableSet(), TestDifferDataOps.instance);

        final ScopeGraphDiffer<String, Integer, List<String>> differ =
                new ScopeGraphDiffer<>(currentContext, previousContext, TestDifferOps.instance, edgeLabels);

        final Ref<ScopeGraphDiff<String, Integer, List<String>>> result = new Ref<>();
        differ.diff(Arrays.asList(s0n), Arrays.asList(s0o)).thenAccept(result::set);
        differ.typeCheckerFinished();

        while(!currentContext.propagatedAll()) {
            assertNull("Differ completed too early.", result.get());
            currentContext.next();
        }

        assertTrue("Differ didn't complete", result.get() != null);
        assertEquals(2, result.get().added().edges().size());

        assertTrue("First edge not added", result.get().added().edges().contains(new Edge<>(s0n, l1, s1n)));
        assertTrue("Second edge not added", result.get().added().edges().contains(new Edge<>(s2n, l1, s3n)));
    }

    private final class TestDifferContext<S, L, D> implements IDifferContext<S, L, D> {

        private final IDifferContext<S, L, D> inner;
        private final LinkedList<ICompletable<Unit>> queue = new LinkedList<>();
        private final Random rand;

        public TestDifferContext(IDifferContext<S, L, D> inner) {
            this.inner = inner;
            this.rand = new Random();
        }

        @Override public IFuture<Iterable<S>> getEdges(S scope, L label) {
            return signal().thenCompose(__ -> inner.getEdges(scope, label));
        }

        @Override public IFuture<Optional<D>> datum(S scope) {
            return signal().thenCompose(__ -> inner.datum(scope));
        }

        @Override public Optional<D> rawDatum(S scope) {
            return inner.rawDatum(scope);
        }

        @Override public boolean available(S scope) {
            return inner.available(scope);
        }

        private IFuture<Unit> signal() {
            final ICompletableFuture<Unit> signal = new CompletableFuture<>();
            queue.add(rand.nextInt(queue.size() + 1), signal);
            return signal;
        }

        public void next() {
            queue.remove().complete(Unit.unit);
        }

        public boolean propagatedAll() {
            return queue.isEmpty();
        }
    }

}
