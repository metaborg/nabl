package mb.p_raffrayi.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;
import org.metaborg.util.Ref;
import org.metaborg.util.collection.CapsuleUtil;

import com.google.common.collect.ImmutableList;

import io.usethesource.capsule.Set;
import mb.p_raffrayi.impl.diff.IScopeGraphDiffer;
import mb.p_raffrayi.impl.diff.ScopeGraphDiffer;
import mb.p_raffrayi.impl.diff.StaticDifferContext;
import mb.p_raffrayi.impl.envdiff.AddedEdge;
import mb.p_raffrayi.impl.envdiff.DifferBasedContext;
import mb.p_raffrayi.impl.envdiff.EnvDiffer;
import mb.p_raffrayi.impl.envdiff.IEnvChange;
import mb.p_raffrayi.impl.envdiff.IEnvDiff;
import mb.p_raffrayi.impl.envdiff.IEnvDiffer;
import mb.p_raffrayi.impl.envdiff.RemovedEdge;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.diff.BiMap;
import mb.scopegraph.oopsla20.reference.ScopeGraph;

public class EnvDiffTest extends BaseDifferTest {

    private static final String s1o = "s1o";
    private static final String s2o = "s2o";
    private static final String s3o = "s3o";

    private static final String s1n = "s1n";
    private static final String s2n = "s2n";
    private static final String s3n = "s3n";

    private static final Integer l1 = 1;

    private static final Set.Immutable<Integer> edgeLabels = CapsuleUtil.immutableSet(l1);

    @Test public void testAddedEdge() {
        // @formatter:off
        final IScopeGraph.Immutable<String, Integer, List<String>> sc1 =
            ScopeGraph.Immutable.<String, Integer, List<String>>of();
        final IScopeGraph.Immutable<String, Integer, List<String>> sc2 =
            ScopeGraph.Immutable.<String, Integer, List<String>>of()
                .addEdge(s1n, l1, s2n);
        // @formatter:on

        final IScopeGraphDiffer<String, Integer, List<String>> differ = new ScopeGraphDiffer<>(
                new StaticDifferContext<>(sc2, CapsuleUtil.toSet(s1n, s2n), TestDifferDataOps.instance),
                new StaticDifferContext<>(sc1, CapsuleUtil.immutableSet(s1o), TestDifferDataOps.instance),
                TestDifferOps.instance, edgeLabels);
        differ.diff(ImmutableList.of(s1n), ImmutableList.of(s1o));

        final IEnvDiffer<String, Integer, List<String>> envDiffer =
                new EnvDiffer<>(new DifferBasedContext<>(differ, edgeLabels), TestDifferOps.instance);

        final Ref<IEnvDiff<String, Integer, List<String>>> diffResult = new Ref<>();
        envDiffer.diff(s1o, LabelWf.any()).thenAccept(diffResult::set);

        assertNotNull(diffResult.get());

        Set.Immutable<IEnvChange<String, Integer, List<String>>> changes = diffResult.get().changes();

        assertEquals(1, changes.size());
        IEnvChange<String, Integer, List<String>> change = changes.iterator().next();

        assertEquals(AddedEdge.of(s2n, LabelWf.any()), change);

        assertEquals(BiMap.Immutable.of(s1n, s1o), diffResult.get().patches());
    }

    @Test public void testRemovedEdge() {
        // @formatter:off
        final IScopeGraph.Immutable<String, Integer, List<String>> sc1 =
            ScopeGraph.Immutable.<String, Integer, List<String>>of()
                .addEdge(s1o, l1, s2o)
                .addEdge(s2o, l1, s3o);
        final IScopeGraph.Immutable<String, Integer, List<String>> sc2 =
            ScopeGraph.Immutable.<String, Integer, List<String>>of()
                .addEdge(s1n, l1, s2n);
        // @formatter:on

        final IScopeGraphDiffer<String, Integer, List<String>> differ = new ScopeGraphDiffer<>(
                new StaticDifferContext<>(sc2, CapsuleUtil.toSet(s1n, s2n), TestDifferDataOps.instance),
                new StaticDifferContext<>(sc1, CapsuleUtil.toSet(s1o, s2o, s3o), TestDifferDataOps.instance),
                TestDifferOps.instance, edgeLabels);
        differ.diff(ImmutableList.of(s1n), ImmutableList.of(s1o));

        final IEnvDiffer<String, Integer, List<String>> envDiffer =
                new EnvDiffer<>(new DifferBasedContext<>(differ, edgeLabels), TestDifferOps.instance);

        final Ref<IEnvDiff<String, Integer, List<String>>> diffResult = new Ref<>();
        envDiffer.diff(s1o, LabelWf.any()).thenAccept(diffResult::set);

        assertNotNull(diffResult.get());

        Set.Immutable<IEnvChange<String, Integer, List<String>>> changes = diffResult.get().changes();

        assertEquals(1, changes.size());
        IEnvChange<String, Integer, List<String>> change = changes.iterator().next();

        assertEquals(RemovedEdge.of(s3o, LabelWf.any()), change);

        assertEquals(BiMap.Immutable.of(s1n, s1o).put(s2n, s2o), diffResult.get().patches());
    }


    @Test public void testFilterEdge() {
        // @formatter:off
        final IScopeGraph.Immutable<String, Integer, List<String>> sc1 =
            ScopeGraph.Immutable.<String, Integer, List<String>>of();
        final IScopeGraph.Immutable<String, Integer, List<String>> sc2 =
            ScopeGraph.Immutable.<String, Integer, List<String>>of()
                .addEdge(s1n, l1, s2n);
        // @formatter:on

        final IScopeGraphDiffer<String, Integer, List<String>> differ = new ScopeGraphDiffer<>(
                new StaticDifferContext<>(sc2, CapsuleUtil.toSet(s1n, s2n), TestDifferDataOps.instance),
                new StaticDifferContext<>(sc1, CapsuleUtil.immutableSet(s1o), TestDifferDataOps.instance),
                TestDifferOps.instance, edgeLabels);
        differ.diff(ImmutableList.of(s1n), ImmutableList.of(s1o));

        final IEnvDiffer<String, Integer, List<String>> envDiffer =
                new EnvDiffer<>(new DifferBasedContext<>(differ, edgeLabels), TestDifferOps.instance);

        final Ref<IEnvDiff<String, Integer, List<String>>> diffResult = new Ref<>();
        envDiffer.diff(s1o, LabelWf.none()).thenAccept(diffResult::set);

        assertNotNull(diffResult.get());

        Set.Immutable<IEnvChange<String, Integer, List<String>>> changes = diffResult.get().changes();

        assertEquals(0, changes.size());

        assertEquals(BiMap.Immutable.of(s1n, s1o), diffResult.get().patches());
    }


    @Test public void testCycle() {
        // @formatter:off
        final IScopeGraph.Immutable<String, Integer, List<String>> sc1 =
            ScopeGraph.Immutable.<String, Integer, List<String>>of()
                .addEdge(s1o, l1, s2o)
                .addEdge(s2o, l1, s1o);
        final IScopeGraph.Immutable<String, Integer, List<String>> sc2 =
            ScopeGraph.Immutable.<String, Integer, List<String>>of()
                .addEdge(s1n, l1, s2n)
                .addEdge(s2n, l1, s1n)
                .addEdge(s2n, l1, s3n);
        // @formatter:on

        final IScopeGraphDiffer<String, Integer, List<String>> differ = new ScopeGraphDiffer<>(
                new StaticDifferContext<>(sc2, CapsuleUtil.toSet(s1n, s2n, s3n), TestDifferDataOps.instance),
                new StaticDifferContext<>(sc1, CapsuleUtil.toSet(s1o, s2o), TestDifferDataOps.instance),
                TestDifferOps.instance, edgeLabels);
        differ.diff(ImmutableList.of(s1n), ImmutableList.of(s1o));

        final IEnvDiffer<String, Integer, List<String>> envDiffer =
                new EnvDiffer<>(new DifferBasedContext<>(differ, edgeLabels), TestDifferOps.instance);

        final Ref<IEnvDiff<String, Integer, List<String>>> diffResult = new Ref<>();
        envDiffer.diff(s1o, LabelWf.any()).thenAccept(diffResult::set);

        assertNotNull(diffResult.get());

        Set.Immutable<IEnvChange<String, Integer, List<String>>> changes =  diffResult.get().changes();

        assertEquals(1, changes.size());
        IEnvChange<String, Integer, List<String>> change = changes.iterator().next();

        assertEquals(AddedEdge.of(s3n, LabelWf.any()), change);

        assertEquals(BiMap.Immutable.of(s1n, s1o).put(s2n, s2o), diffResult.get().patches());
    }

}
