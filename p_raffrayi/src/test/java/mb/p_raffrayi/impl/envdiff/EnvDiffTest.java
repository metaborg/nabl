package mb.p_raffrayi.impl.envdiff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.List;

import org.junit.Test;
import org.metaborg.util.Ref;
import org.metaborg.util.future.IFuture;

import com.google.common.collect.ImmutableList;

import io.usethesource.capsule.Set;
import mb.p_raffrayi.impl.diff.IDifferOps;
import mb.p_raffrayi.impl.diff.IScopeGraphDiffer;
import mb.p_raffrayi.impl.diff.ScopeGraphDiffer;
import mb.p_raffrayi.impl.diff.StaticDifferContext;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.diff.BiMap;
import mb.scopegraph.oopsla20.reference.ScopeGraph;
import mb.scopegraph.oopsla20.terms.newPath.ResolutionPath;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

public class EnvDiffTest {

    private static final String s1o = "s1o";
    private static final String s2o = "s2o";
    private static final String s3o = "s3o";

    private static final String s1n = "s1n";
    private static final String s2n = "s2n";
    private static final String s3n = "s3n";

    private static final Integer l1 = 1;

    @Test public void testAddedEdge() {
        // @formatter:off
        final IScopeGraph.Immutable<String, Integer, List<String>> sc1 =
            ScopeGraph.Immutable.<String, Integer, List<String>>of();
        final IScopeGraph.Immutable<String, Integer, List<String>> sc2 =
            ScopeGraph.Immutable.<String, Integer, List<String>>of()
                .addEdge(s1n, l1, s2n);
        // @formatter:on

        final IScopeGraphDiffer<String, Integer, List<String>> differ = new ScopeGraphDiffer<>(
                new StaticDifferContext<>(sc2), new StaticDifferContext<>(sc1), TestDifferOps.instance);
        differ.diff(ImmutableList.of(s1n), ImmutableList.of(s1o));

        final IEnvDiffer<String, Integer, List<String>> envDiffer = new EnvDiffer<>(differ, TestDifferOps.instance);

        final Ref<IEnvDiff<String, Integer, List<String>>> diffResult = new Ref<>();
        envDiffer.diff(s1o, LabelWf.any(), DataWf.any()).thenAccept(diffResult::set);

        assertNotNull(diffResult.get());

        Set.Immutable<ResolutionPath<String, Integer, IEnvDiff<String, Integer, List<String>>>> paths =
                diffResult.get().diffPaths();

        assertEquals(1, paths.size());
        ResolutionPath<String, Integer, IEnvDiff<String, Integer, List<String>>> path = paths.iterator().next();

        assertEquals(new ScopePath<>(s1o).step(l1, s2n).get(), path.getPath());
        assertEquals(AddedEdge.of(s2n, /* CapsuleUtil.immutableSet(s1o, s2n), */ LabelWf.any(), DataWf.any()), path.getDatum());

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
                new StaticDifferContext<>(sc2), new StaticDifferContext<>(sc1), TestDifferOps.instance);
        differ.diff(ImmutableList.of(s1n), ImmutableList.of(s1o));

        final IEnvDiffer<String, Integer, List<String>> envDiffer = new EnvDiffer<>(differ, TestDifferOps.instance);

        final Ref<IEnvDiff<String, Integer, List<String>>> diffResult = new Ref<>();
        envDiffer.diff(s1o, LabelWf.any(), DataWf.any()).thenAccept(diffResult::set);

        assertNotNull(diffResult.get());

        Set.Immutable<ResolutionPath<String, Integer, IEnvDiff<String, Integer, List<String>>>> paths =
                diffResult.get().diffPaths();

        assertEquals(1, paths.size());
        ResolutionPath<String, Integer, IEnvDiff<String, Integer, List<String>>> path = paths.iterator().next();

        assertEquals(new ScopePath<>(s1o).step(l1, s2o).get().step(l1, s3o).get(), path.getPath());
        assertEquals(RemovedEdge.of(s3o, LabelWf.any(), DataWf.any()), path.getDatum());

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
                new StaticDifferContext<>(sc2), new StaticDifferContext<>(sc1), TestDifferOps.instance);
        differ.diff(ImmutableList.of(s1n), ImmutableList.of(s1o));

        final IEnvDiffer<String, Integer, List<String>> envDiffer = new EnvDiffer<>(differ, TestDifferOps.instance);

        final Ref<IEnvDiff<String, Integer, List<String>>> diffResult = new Ref<>();
        envDiffer.diff(s1o, LabelWf.none(), DataWf.any()).thenAccept(diffResult::set);

        assertNotNull(diffResult.get());

        Set.Immutable<ResolutionPath<String, Integer, IEnvDiff<String, Integer, List<String>>>> paths =
                diffResult.get().diffPaths();

        assertEquals(0, paths.size());

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
                new StaticDifferContext<>(sc2), new StaticDifferContext<>(sc1), TestDifferOps.instance);
        differ.diff(ImmutableList.of(s1n), ImmutableList.of(s1o));

        final IEnvDiffer<String, Integer, List<String>> envDiffer = new EnvDiffer<>(differ, TestDifferOps.instance);

        final Ref<IEnvDiff<String, Integer, List<String>>> diffResult = new Ref<>();
        envDiffer.diff(s1o, LabelWf.any(), DataWf.any()).thenAccept(diffResult::set);

        assertNotNull(diffResult.get());

        Set.Immutable<ResolutionPath<String, Integer, IEnvDiff<String, Integer, List<String>>>> paths =
                diffResult.get().diffPaths();

        assertEquals(1, paths.size());
        ResolutionPath<String, Integer, IEnvDiff<String, Integer, List<String>>> path = paths.iterator().next();

        assertEquals(new ScopePath<>(s1o).step(l1, s2o).get().step(l1, s3n).get(), path.getPath());
        assertEquals(AddedEdge.of(s3n, /* CapsuleUtil.toSet(s1o, s2o, s3n), */LabelWf.any(), DataWf.any()), path.getDatum());

        assertEquals(BiMap.Immutable.of(s1n, s1o).put(s2n, s2o), diffResult.get().patches());
    }


    private static class TestDifferOps implements IDifferOps<String, Integer, List<String>> {

        public static final TestDifferOps instance = new TestDifferOps();

        private TestDifferOps() {
        }

        @Override public boolean isMatchAllowed(String currentScope, String previousScope) {
            return true;
        }

        @Override public Optional<BiMap.Immutable<String>> matchDatums(List<String> currentDatum,
                List<String> previousDatum) {
            if(currentDatum.size() == previousDatum.size()) {
                final BiMap.Transient<String> matches = BiMap.Transient.of();
                final Iterator<String> pIterator = previousDatum.iterator();
                for(String scope : currentDatum) {
                    matches.put(scope, pIterator.next());
                }
                return Optional.of(matches.freeze());
            }
            return Optional.empty();
        }

        @Override public Collection<String> getScopes(List<String> d) {
            return d;
        }

        @Override public List<String> embed(String scope) {
            return ImmutableList.of(scope);
        }

        @Override public boolean ownScope(String scope) {
            return true;
        }

        @Override public boolean ownOrSharedScope(String currentScope) {
            return true;
        }

        @Override public IFuture<Optional<String>> externalMatch(String previousScope) {
            throw new UnsupportedOperationException();
        }

    }

}
