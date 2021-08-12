package mb.p_raffrayi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.Ignore;
import org.junit.Test;
import org.metaborg.util.future.AggregateFuture;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.tuple.Tuple2;
import org.metaborg.util.unit.Unit;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import io.usethesource.capsule.Set;
import mb.p_raffrayi.IUnitResult.TransitionTrace;
import mb.p_raffrayi.impl.RecordedQuery;
import mb.p_raffrayi.impl.UnitResult;
import mb.p_raffrayi.nameresolution.DataLeq;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelOrder;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.reference.Env;
import mb.scopegraph.oopsla20.reference.ScopeGraph;
import mb.scopegraph.oopsla20.terms.newPath.ResolutionPath;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

public class IncrementalTest extends PRaffrayiTestBase {

    ///////////////////////////////////////////////////////////////////////////
    // Release conditions
    ///////////////////////////////////////////////////////////////////////////

    @Test(timeout = 10000) public void testSimpleRelease() throws InterruptedException, ExecutionException {
        final IUnitResult<Scope, Integer, IDatum, Unit> previousResult = rootResult().build();

        final IFuture<IUnitResult<Scope, Integer, IDatum, Unit>> future =
                this.run(new NoopTypeChecker(".", false), Set.Immutable.of(), previousResult);

        final IUnitResult<Scope, Integer, IDatum, Unit> result = future.asJavaCompletion().get();

        assertEquals(TransitionTrace.RELEASED, result.stateTransitionTrace());
        assertTrue(result.failures().isEmpty());
    }

    @Test(timeout = 10000) public void testReleaseChild_ParentCached() throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Unit> childResult = subResult("/./sub", root)
            .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), Env.empty()))
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Unit> parentResult = rootResult()
            .putSubUnitResults("sub", childResult)
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, Integer, IDatum, Unit>> future =
                this.run(new ComposedRootTypeChecker<>(false, new SingleQueryTypeChecker("sub", false)), Set.Immutable.of(), parentResult);

        final IUnitResult<Scope, Integer, IDatum, Unit> result = future.asJavaCompletion().get();
        assertTrue(result.failures().isEmpty());

        assertEquals(TransitionTrace.RELEASED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.RELEASED, result.subUnitResults().get("sub").stateTransitionTrace());
    }

    @Test(timeout = 10000) public void testReleaseChild_ParentChanged() throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Unit> childResult = subResult("/./sub", root)
            .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), Env.empty()))
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Unit> parentResult = rootResult()
            .putSubUnitResults("sub", childResult)
            .build();
        // @formatter:on


        final IFuture<IUnitResult<Scope, Integer, IDatum, Unit>> future =
                this.run(new ComposedRootTypeChecker<>(true, new SingleQueryTypeChecker("sub", false)), Set.Immutable.of(), parentResult);

        final IUnitResult<Scope, Integer, IDatum, Unit> result = future.asJavaCompletion().get();
        assertTrue(result.failures().isEmpty());

        assertEquals(TransitionTrace.INITIALLY_STARTED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.RELEASED, result.subUnitResults().get("sub").stateTransitionTrace());
    }

    @Test(timeout = 10000) public void testRelease_MutualDep_Cached() throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final Integer lbl = 1;
        final Scope d = new Scope("/./sub", -1);

        final ResolutionPath<Scope, Integer, IDatum> path =
                new ScopePath<Scope, Integer>(root).step(lbl, d).get().resolve(d);
        final Env<Scope, Integer, IDatum> env = Env.of(path);

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Unit> childResult = subResult("/./sub", root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
            .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), env))
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Unit> parentResult = rootResult()
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d))
            .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), env))
            .putSubUnitResults("sub", childResult)
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, Integer, IDatum, Unit>> future =
                this.run(new ComposedRootTypeChecker<>(false, new DeclQueryTypeChecker("sub", false, lbl)), Set.Immutable.of(lbl), parentResult);

        final IUnitResult<Scope, Integer, IDatum, Unit> result = future.asJavaCompletion().get();
        assertTrue(result.failures().isEmpty());

        assertEquals(TransitionTrace.RELEASED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.RELEASED, result.subUnitResults().get("sub").stateTransitionTrace());
    }

    @Ignore("Requires early validation of shared edges") @Test(timeout = 10000) public void
            testRelease_MutualDep_ParentChanged() throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final Integer lbl = 1;
        final Scope d = new Scope("/./sub", 1);

        final ResolutionPath<Scope, Integer, IDatum> path =
                new ScopePath<Scope, Integer>(root).step(lbl, d).get().resolve(d);
        final Env<Scope, Integer, IDatum> env = Env.of(path);

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Unit> childResult = subResult("/./sub", root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
            .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), env))
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Unit> parentResult = rootResult()
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d))
            .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), env))
            .putSubUnitResults("sub", childResult)
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, Integer, IDatum, Unit>> future =
                this.run(new ComposedRootTypeChecker<>(true, new DeclQueryTypeChecker("sub", false, lbl)), Set.Immutable.of(lbl), parentResult);

        final IUnitResult<Scope, Integer, IDatum, Unit> result = future.asJavaCompletion().get();
        assertTrue(result.failures().isEmpty());

        assertEquals(TransitionTrace.INITIALLY_STARTED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.RELEASED, result.subUnitResults().get("sub").stateTransitionTrace());
    }

    @Test(timeout = 10000) public void testRelease_MutualDep_ChildChanged()
            throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final Integer lbl = 1;
        final Scope d = new Scope("/./sub", -1);

        final ResolutionPath<Scope, Integer, IDatum> path =
                new ScopePath<Scope, Integer>(root).step(lbl, d).get().resolve(d);
        final Env<Scope, Integer, IDatum> env = Env.of(path);

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Unit> childResult = subResult("/./sub", root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
            .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), env))
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Unit> parentResult = rootResult()
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d))
            .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), env))
            .putSubUnitResults("sub", childResult)
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, Integer, IDatum, Unit>> future =
                this.run(new ComposedRootTypeChecker<>(false, new DeclQueryTypeChecker("sub", true, lbl)), Set.Immutable.of(lbl), parentResult);

        final IUnitResult<Scope, Integer, IDatum, Unit> result = future.asJavaCompletion().get();
        assertTrue(result.failures().isEmpty());

        assertEquals(TransitionTrace.RELEASED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.INITIALLY_STARTED, result.subUnitResults().get("sub").stateTransitionTrace());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Restart conditions
    ///////////////////////////////////////////////////////////////////////////

    @Test(timeout = 10000) public void testSimpleRestart() throws InterruptedException, ExecutionException {
        final IUnitResult<Scope, Integer, IDatum, Unit> previousResult = rootResult().build();

        final IFuture<IUnitResult<Scope, Integer, IDatum, Unit>> future =
                this.run(new NoopTypeChecker(".", true), Set.Immutable.of(), previousResult);

        final IUnitResult<Scope, Integer, IDatum, Unit> result = future.asJavaCompletion().get();

        assertTrue(result.failures().isEmpty());
        assertEquals(TransitionTrace.INITIALLY_STARTED, result.stateTransitionTrace());
    }

    @Test(timeout = 10000) public void testRestartChild_ParentChanged()
            throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final Integer lbl = 1;
        final Scope d = new Scope("/./sub", 1);
        final ResolutionPath<Scope, Integer, IDatum> path =
                new ScopePath<Scope, Integer>(root).step(lbl, d).get().resolve(d);

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Unit> childResult = subResult("/./sub", root)
            .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), Env.of(path)))
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Unit> parentResult = rootResult()
            .putSubUnitResults("sub", childResult)
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, Integer, IDatum, Unit>> future =
                this.run(new ComposedSelfDeclRootTypeChecker<>(true, lbl,
                        new SingleQueryTypeChecker("sub", false)), Set.Immutable.of(lbl), parentResult);

        final IUnitResult<Scope, Integer, IDatum, Unit> result = future.asJavaCompletion().get();
        assertTrue(result.failures().isEmpty());

        assertEquals(TransitionTrace.INITIALLY_STARTED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.RESTARTED, result.subUnitResults().get("sub").stateTransitionTrace());
    }

    @Test(timeout = 10000) public void testRestart_SharedEdgeChanged() throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final Integer lbl = 1;
        final Scope d1 = new Scope("/./sub2", 123);
        final IDatum datum = new IDatum() {};

        final ScopePath<Scope, Integer> scopePath = new ScopePath<Scope, Integer>(root).step(lbl, d1).get();
        final ResolutionPath<Scope, Integer, IDatum> path = scopePath.resolve(d1);
        final RecordedQuery<Scope, Integer, IDatum> rqTrans = RecordedQuery.of(scopePath, LabelWf.any(), DataWf.any(),
                LabelOrder.none(), DataLeq.any(), Env.of(path));
        final RecordedQuery<Scope, Integer, IDatum> rq =
                RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), Env.of(path))
                        .withTransitiveQueries(rqTrans);

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Unit> child1Result = subResult("/./sub1", root)
            .addQueries(rq)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Unit> child2Result = subResult("/./sub2", root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Unit> parentResult = rootResult()
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1))
            .putSubUnitResults("sub1", child1Result)
            .putSubUnitResults("sub2", child2Result)
            .build();
        // @formatter:on


        final IFuture<IUnitResult<Scope, Integer, IDatum, Unit>> future =
                this.run(new ComposedRootTypeChecker<>(false, new SingleQueryTypeChecker("sub1", false),
                        new DeclTypeChecker("sub2", true, lbl, datum)), Set.Immutable.of(lbl), parentResult);

        final IUnitResult<Scope, Integer, IDatum, Unit> result = future.asJavaCompletion().get();
        assertTrue(result.failures().isEmpty());

        assertEquals(TransitionTrace.RELEASED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.RESTARTED, result.subUnitResults().get("sub1").stateTransitionTrace());
        assertEquals(TransitionTrace.INITIALLY_STARTED, result.subUnitResults().get("sub2").stateTransitionTrace());
    }

    @Test(timeout = 10000) public void testRestart_FailureInInitialState()
            throws InterruptedException, ExecutionException {
        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Unit> previousResult = rootResult()
            .addFailures(new Exception())
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, Integer, IDatum, Unit>> future =
                this.run(new NoopTypeChecker(".", false), Set.Immutable.of(), previousResult);

        final IUnitResult<Scope, Integer, IDatum, Unit> result = future.asJavaCompletion().get();

        assertTrue(result.failures().isEmpty());
        assertEquals(TransitionTrace.INITIALLY_STARTED, result.stateTransitionTrace());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Query Recording behavior
    ///////////////////////////////////////////////////////////////////////////

    @Test(timeout = 10000) public void testRecord_SimpleQuery() throws InterruptedException, ExecutionException {
        final Integer lbl = 1;

        final IFuture<IUnitResult<Scope, Integer, IDatum, Unit>> future =
                this.run(new ComposedSelfDeclRootTypeChecker<>(true, lbl, new SingleQueryTypeChecker("sub", true)), Set.Immutable.of(lbl), null);

        IUnitResult<Scope, Integer, IDatum, Unit> result = future.asJavaCompletion().get();

        assertTrue(result.failures().isEmpty());
        assertEquals(1, result.subUnitResults().get("sub").queries().size());
    }

    @Test(timeout = 10000) public void testRecord_SharedDeclQuery() throws InterruptedException, ExecutionException {
        final Integer lbl = 1;
        final IDatum datum = new IDatum() {};

        final IFuture<IUnitResult<Scope, Integer, IDatum, Integer>> future =
                this.run(new ComposedRootTypeChecker<Integer>(true, new EnvSizeTypeChecker("sub1", true),
                        new DeclTypeChecker("sub2", true, lbl, datum)), Set.Immutable.of(lbl), null);

        IUnitResult<Scope, Integer, IDatum, Integer> result = future.asJavaCompletion().get();

        assertTrue(result.failures().isEmpty());
        assertEquals(1, (int) result.analysis());

        assertEquals(1, result.subUnitResults().get("sub1").queries().size());
        assertEquals(1, result.subUnitResults().get("sub1").queries().iterator().next().transitiveQueries().size());
        assertEquals(0, result.queries().size());

    }

    ///////////////////////////////////////////////////////////////////////////
    // Release behavior
    ///////////////////////////////////////////////////////////////////////////

    @Test(timeout = 10000) public void testQueryInReleasedUnit() throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final Integer lbl = 1;
        final Scope d = new Scope("/.", 1);

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Unit> parentResult = rootResult()
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, Integer, IDatum, Unit>> future =
                this.run(new ComposedRootTypeChecker<Unit>(false, new DeclQueryTypeChecker("sub", true, lbl)),
                        Set.Immutable.of(lbl), parentResult);

        final IUnitResult<Scope, Integer, IDatum, Unit> result = future.asJavaCompletion().get();
        assertTrue(result.failures().isEmpty());

        assertEquals(TransitionTrace.RELEASED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.INITIALLY_STARTED, result.subUnitResults().get("sub").stateTransitionTrace());
    }

    @Test(timeout = 10000) public void testReleasedUnit_ContainsRootScopes()
            throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 123);
        final Integer lbl = 1;
        final Scope d = new Scope("/./sub", 1);

        // @formatter:off
        final ResolutionPath<Scope, Integer, IDatum> path = new ScopePath<Scope, Integer>(root).step(lbl, d).get().resolve(d);
        final Env<Scope, Integer, IDatum> env = Env.of(path);
        final IRecordedQuery<Scope, Integer, IDatum> query = RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), env);
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Unit> childResult = subResult("/./sub", root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
            .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), env))
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Unit> parentResult = rootResult()
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d))
            .addQueries(query)
            .putSubUnitResults("sub", childResult)
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, Integer, IDatum, Unit>> future =
                this.run(new ComposedRootTypeChecker<Unit>(false, new DeclQueryTypeChecker("sub", false, lbl)),
                        Set.Immutable.of(lbl), parentResult);

        final IUnitResult<Scope, Integer, IDatum, Unit> result = future.asJavaCompletion().get();
        final IUnitResult<Scope, Integer, IDatum, ?> subResult = result.subUnitResults().get("sub");

        assertEquals(TransitionTrace.RELEASED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.RELEASED, subResult.stateTransitionTrace());

        assertEquals(1, subResult.rootScopes().size());
        // Root scopes does not necessarily include `root`, but rather its match.
    }

    @Test(timeout = 10000) public void testReleasedUnit_ContainsQueries()
            throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final Integer lbl = 1;
        final Scope d = new Scope("/./sub", 1);

        // @formatter:off
        final ResolutionPath<Scope, Integer, IDatum> path = new ScopePath<Scope, Integer>(root).step(lbl, d).get().resolve(d);
        final Env<Scope, Integer, IDatum> env = Env.of(path);
        final IRecordedQuery<Scope, Integer, IDatum> query = RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), env);
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Unit> childResult = subResult("/./sub", root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
            .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), env))
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Unit> parentResult = rootResult()
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d))
            .putSubUnitResults("sub", childResult)
            .addQueries(query)
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, Integer, IDatum, Unit>> future =
                this.run(new ComposedRootTypeChecker<Unit>(false, new DeclQueryTypeChecker("sub", false, lbl)),
                        Set.Immutable.of(lbl), parentResult);

        final IUnitResult<Scope, Integer, IDatum, Unit> result = future.asJavaCompletion().get();

        assertEquals(TransitionTrace.RELEASED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.RELEASED, result.subUnitResults().get("sub").stateTransitionTrace());

        assertEquals("Invalid query count.", 1, result.queries().size());
        assertTrue("Query not recorded", result.queries().contains(query));
    }

    @Test(timeout = 10000) public void testReleasedUnit_ContainsLocalSG()
            throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final Integer lbl = 1;
        final Scope d = new Scope("/.", 1);

        // @formatter:off
        final IScopeGraph.Immutable<Scope, Integer, IDatum> sg = ScopeGraph.Immutable.<Scope, Integer, IDatum>of()
            .addEdge(root, lbl, d)
            .setDatum(d, d);
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Unit> previousResult = rootResult()
            .scopeGraph(sg)
            .localScopeGraph(sg)
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, Integer, IDatum, Unit>> future =
                this.run(new NoopTypeChecker(".", false), Set.Immutable.of(), previousResult);

        final IUnitResult<Scope, Integer, IDatum, Unit> result = future.asJavaCompletion().get();

        assertTrue(result.failures().isEmpty());
        assertEquals(TransitionTrace.RELEASED, result.stateTransitionTrace());

        assertEquals(1, result.scopeGraph().getEdges().size());
        assertTrue(Iterables.elementsEqual(Arrays.asList(d), result.scopeGraph().getEdges(root, lbl)));
        assertEquals(sg.getData(), result.scopeGraph().getData());

        assertEquals(1, result.localScopeGraph().getEdges().size());
        assertTrue(Iterables.elementsEqual(Arrays.asList(d), result.localScopeGraph().getEdges(root, lbl)));
        assertEquals(sg.getData(), result.localScopeGraph().getData());
    }

    @Test(timeout = 10000) public void testReleaseChild_ParentUpdateSG()
            throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final Integer lbl = 1;
        final Scope d1 = new Scope("/./sub", 1);

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Unit> childResult = subResult("/./sub", root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Unit> parentResult = rootResult()
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1))
            .putSubUnitResults("sub", childResult)
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, Integer, IDatum, Unit>> future =
                this.run(new ComposedRootTypeChecker<Unit>(false, new SelfDeclTypeChecker("sub", false, lbl)),
                        Set.Immutable.of(lbl), parentResult);

        final IUnitResult<Scope, Integer, IDatum, Unit> result = future.asJavaCompletion().get();
        final IUnitResult<Scope, Integer, IDatum, ?> subResult = result.subUnitResults().get("sub");

        assertTrue(result.failures().isEmpty());
        assertEquals(TransitionTrace.RELEASED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.RELEASED, subResult.stateTransitionTrace());

        final Scope newRoot = subResult.rootScopes().get(0);

        // Verify scope graph is correct
        final List<Scope> allTargets = Lists.newArrayList(result.scopeGraph().getEdges(newRoot, lbl));

        assertEquals(1, allTargets.size());
        final Scope tgt = allTargets.get(0);

        assertEquals(d1, subResult.scopeGraph().getData(tgt).get());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Restart behavior
    ///////////////////////////////////////////////////////////////////////////

    @Test(timeout = 10000) public void testQueryInRestartedUnit() throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final Integer lbl = 1;
        final Scope d1 = new Scope("/.", 1);

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Integer> parentResult = UnitResult.<Scope, Integer, IDatum, Integer>builder()
            .id("/.")
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .analysis(-1)
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, Integer, IDatum, Integer>> future =
                this.run(new ComposedSelfDeclRootTypeChecker<Integer>(true, lbl, new EnvSizeTypeChecker("sub", true)),
                        Set.Immutable.of(lbl), parentResult);

        final IUnitResult<Scope, Integer, IDatum, Integer> result = future.asJavaCompletion().get();
        assertTrue(result.failures().isEmpty());
        assertEquals(1, (int) result.analysis());
    }

    @Test(timeout = 10000) public void testRestartChild_ParentUpdateSG()
            throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final Integer lbl = 1;
        final Scope d1 = new Scope("/./sub", 1);
        final IDatum datum = new IDatum() {};

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Unit> childResult = subResult("/./sub", root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), Env.empty()))
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Unit> parentResult = rootResult()
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1))
            .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.none(), LabelOrder.none(), DataLeq.any(), Env.empty()))
            .putSubUnitResults("sub", childResult)
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, Integer, IDatum, Unit>> future =
                this.run(new ComposedRootTypeChecker<Unit>(false, new DeclTypeChecker("sub", true, lbl, datum)),
                        Set.Immutable.of(lbl), parentResult);

        final IUnitResult<Scope, Integer, IDatum, Unit> result = future.asJavaCompletion().get();
        final IUnitResult<Scope, Integer, IDatum, ?> subResult = result.subUnitResults().get("sub");

        assertTrue(result.failures().isEmpty());
        assertEquals(TransitionTrace.RELEASED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.INITIALLY_STARTED, subResult.stateTransitionTrace());

        final Scope newRoot = subResult.rootScopes().get(0);

        // Verify scope graph is correct
        final List<Scope> allTargets = Lists.newArrayList(result.scopeGraph().getEdges(newRoot, lbl));

        assertEquals(1, allTargets.size());
        final Scope tgt = allTargets.get(0);

        assertEquals(datum, subResult.scopeGraph().getData(tgt).get());

        // Verify local part of scope graph is correct
        final List<Scope> parentTargets = Lists.newArrayList(result.localScopeGraph().getEdges(newRoot, lbl));
        assertEquals(0, parentTargets.size());

        final List<Scope> childTargets = Lists.newArrayList(subResult.localScopeGraph().getEdges(newRoot, lbl));
        assertEquals(Arrays.asList(tgt), childTargets);

        assertEquals(datum, subResult.localScopeGraph().getData(tgt).get());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Other
    ///////////////////////////////////////////////////////////////////////////

    @Test(timeout = 10000) public void testActivateOnDelayedUnitAddition()
            throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final Integer lbl = 1;
        final Scope d1 = new Scope("/./sub2", 1);

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Unit> sub1Result = subResult("/./sub1", root)
            .addQueries(RecordedQuery.of(d1, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.none(), Env.empty()))
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Unit> sub2Result = subResult("/./sub2", root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Unit> parentResult = rootResult()
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1))
            .putSubUnitResults("sub1", sub1Result)
            .putSubUnitResults("sub2", sub2Result)
            .analysis(Unit.unit)
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, Integer, IDatum, Unit>> future =
                this.run(".", new ITypeChecker<Scope, Integer, IDatum, Unit>() {

                    @Override public IFuture<Unit> run(
                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, Unit> unit, List<Scope> roots) {
                        final Scope s = unit.freshScope("s", Arrays.asList(lbl), false, true);
                        final IFuture<IUnitResult<Scope, Integer, IDatum, Unit>> sub1Future =
                                unit.add("sub1", new NoopTypeChecker("sub1", false), Arrays.asList(s), false);

                        // Do some 'heavy work', ensure request for unit 2 ref from unit 1 is executed by now.
                        try {
                            Thread.sleep(100);
                        } catch(InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        final IFuture<IUnitResult<Scope, Integer, IDatum, Unit>> sub2Future =
                                unit.add("sub2", new NoopTypeChecker("sub2", false), Arrays.asList(s), false);

                        unit.closeScope(s);

                        return unit.runIncremental(restarted -> CompletableFuture.completedFuture(Unit.unit))
                                .thenCompose(res -> sub2Future.thenCompose(s2res -> sub1Future)).thenApply(IUnitResult::analysis);
                    }

                }, Set.Immutable.of(lbl), false, parentResult);

        final IUnitResult<Scope, Integer, IDatum, Unit> result = future.asJavaCompletion().get();

        assertEquals(TransitionTrace.RELEASED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.RELEASED, result.subUnitResults().get("sub1").stateTransitionTrace());
        assertEquals(TransitionTrace.RELEASED, result.subUnitResults().get("sub2").stateTransitionTrace());

        assertTrue(result.failures().isEmpty());
    }

    @Test(timeout = 10000) public void testChildRemoved() throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final Integer lbl = 1;
        final Scope d1 = new Scope("/./sub2/sub", 1);

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Unit> sub1Result = subResult("/./sub1", root)
            .addQueries(RecordedQuery.of(d1, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.none(), Env.empty()))
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Unit> sub2subResult = subResult("/./sub2/sub", root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Unit> sub2Result = subResult("/./sub2", root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .putSubUnitResults("sub", sub2subResult)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Unit> parentResult = rootResult()
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1))
            .putSubUnitResults("sub1", sub1Result)
            .putSubUnitResults("sub2", sub2Result)
            .build();
        // @formatter:on


        final IFuture<IUnitResult<Scope, Integer, IDatum, Unit>> future =
                this.run(new ComposedRootTypeChecker<Unit>(false, new NoopTypeChecker("sub1", false)),
                        Set.Immutable.of(lbl), parentResult);

        final IUnitResult<Scope, Integer, IDatum, Unit> result = future.asJavaCompletion().get();
        assertTrue(result.allFailures().isEmpty());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Type Checkers
    ///////////////////////////////////////////////////////////////////////////

    private final class NoopTypeChecker extends TestTypeChecker<Unit> {
        protected NoopTypeChecker(String id, boolean changed) {
            super(id, changed);
        }

        @Override public IFuture<Unit> run(
                IIncrementalTypeCheckerContext<Scope, Integer, IDatum, Unit> unit, List<Scope> roots) {
            return unit.runIncremental(restarted -> CompletableFuture.completedFuture(Unit.unit));
        }
    }

    private final class DeclTypeChecker extends TestTypeChecker<Unit> {

        private final Integer lbl;
        private final IDatum datum;

        private DeclTypeChecker(String id, boolean changed, Integer lbl, IDatum datum) {
            super(id, changed);
            this.lbl = lbl;
            this.datum = datum;
        }

        @Override public IFuture<Unit> run(
                IIncrementalTypeCheckerContext<Scope, Integer, IDatum, Unit> unit,
                List<Scope> rootScopes) {
            final Scope s1 = rootScopes.get(0);
            unit.initScope(s1, Arrays.asList(lbl), false);
            return unit.runIncremental(restarted -> {
                final Scope d = unit.freshScope("d", Arrays.asList(), true, false);
                unit.addEdge(s1, lbl, d);
                unit.setDatum(d, datum);
                unit.closeEdge(s1, lbl);
                return CompletableFuture.completedFuture(Unit.unit);
            });
        }
    }

    private final class SelfDeclTypeChecker extends TestTypeChecker<Unit> {
        private final Integer lbl;

        private SelfDeclTypeChecker(String id, boolean changed, Integer lbl) {
            super(id, changed);
            this.lbl = lbl;
        }

        @Override public IFuture<Unit> run(
                IIncrementalTypeCheckerContext<Scope, Integer, IDatum, Unit> unit,
                List<Scope> rootScopes) {
            final Scope s1 = rootScopes.get(0);
            unit.initScope(s1, Arrays.asList(lbl), false);
            return unit.runIncremental(restarted -> {
                final Scope d = unit.freshScope("d", Arrays.asList(), true, false);
                unit.addEdge(s1, lbl, d);
                unit.setDatum(d, d);
                unit.closeEdge(s1, lbl);
                return CompletableFuture.completedFuture(Unit.unit);
            });
        }
    }

    private final class SingleQueryTypeChecker extends TestTypeChecker<Unit> {

        public SingleQueryTypeChecker(String id, boolean changed) {
            super(id, changed);
        }

        @Override public IFuture<Unit> run(
                IIncrementalTypeCheckerContext<Scope, Integer, IDatum, Unit> unit,
                List<Scope> rootScopes) {
            final Scope s1 = rootScopes.get(0);
            unit.initScope(s1, Arrays.asList(), false);
            return unit.runIncremental(restarted -> {
                return unit.query(s1, LabelWf.any(), LabelOrder.none(), DataWf.any(),
                        DataLeq.any()).thenApply(__ -> Unit.unit);
            });
        }
    }

    private final class DeclQueryTypeChecker extends TestTypeChecker<Unit> {

        private final Integer lbl;

        private DeclQueryTypeChecker(String id, boolean changed, Integer lbl) {
            super(id, changed);
            this.lbl = lbl;
        }

        @Override public IFuture<Unit> run(
                IIncrementalTypeCheckerContext<Scope, Integer, IDatum, Unit> unit,
                List<Scope> rootScopes) {
            final Scope s1 = rootScopes.get(0);
            unit.initScope(s1, Arrays.asList(lbl), false);
            return unit.runIncremental(restarted -> {
                final Scope d = unit.freshScope("d", Arrays.asList(), true, false);
                unit.setDatum(d, d);
                unit.addEdge(s1, lbl, d);
                unit.closeEdge(s1, lbl);

                return unit.query(s1, LabelWf.any(), LabelOrder.none(), DataWf.any(),
                        DataLeq.any()).thenApply(__ -> Unit.unit);
            });
        }
    }

    private final class EnvSizeTypeChecker extends TestTypeChecker<Integer> {

        protected EnvSizeTypeChecker(String id, boolean changed) {
            super(id, changed);
        }

        @Override public IFuture<Integer> run(
                IIncrementalTypeCheckerContext<Scope, Integer, IDatum, Integer> unit,
                List<Scope> rootScopes) {
            final Scope s1 = rootScopes.get(0);
            unit.initScope(s1, Arrays.asList(), false);
            return unit.runIncremental(restarted -> {
                return unit.query(s1, LabelWf.any(), LabelOrder.none(), DataWf.any(),
                        DataLeq.any()).thenApply(env -> env.size());
            });
        }
    }

    private class ComposedRootTypeChecker<R> extends TestTypeChecker<R> {

        private final TestTypeChecker<R> typeChecker;

        private final List<TestTypeChecker<?>> typeCheckers;

        public ComposedRootTypeChecker(boolean changed, TestTypeChecker<R> typeChecker,
                TestTypeChecker<?>... typeCheckers) {
            super(".", changed);
            this.typeChecker = typeChecker;
            this.typeCheckers = Arrays.asList(typeCheckers);
        }

        @Override public IFuture<R> run(IIncrementalTypeCheckerContext<Scope, Integer, IDatum, R> unit,
                List<Scope> rootScopes) {
            final Scope root = unit.freshScope("s", rootLabels(), false, true);

            // Start subunits
            final IFuture<IUnitResult<Scope, Integer, IDatum, R>> result =
                    unit.add(typeChecker.getId(), typeChecker, Arrays.asList(root), typeChecker.isChanged());
            final IFuture<?> otherResults = AggregateFuture.forAll(typeCheckers, tc -> unit.add(tc.getId(), tc, Arrays.asList(root), tc.isChanged()));

            unit.closeScope(root);

            // @formatter:off
            return unit.<Unit>runIncremental(
                restarted -> {
                    // Perform other actions
                    callback(unit, root);
                    return CompletableFuture.completedFuture(Unit.unit);
                },
                __ -> Unit.unit,
                (__, ex) -> {
                    if(ex != null) {
                        return CompletableFuture.completedExceptionally(ex);
                    }
                    return AggregateFuture.apply(result, otherResults).thenApply(Tuple2::_1).thenApply(IUnitResult::analysis);
                }
            );
            // @formatter:on
        }

        protected Iterable<Integer> rootLabels() {
            return Arrays.asList();
        }

        protected void callback(IIncrementalTypeCheckerContext<Scope, Integer, IDatum, R> unit, Scope root) {
            // No-op by default
        }

    }

    private class ComposedSelfDeclRootTypeChecker<R> extends ComposedRootTypeChecker<R> {

        private final Integer lbl;

        public ComposedSelfDeclRootTypeChecker(boolean changed, Integer lbl, TestTypeChecker<R> typeChecker,
                TestTypeChecker<?>... typeCheckers) {
            super(changed, typeChecker, typeCheckers);
            this.lbl = lbl;
        }

        @Override protected Iterable<Integer> rootLabels() {
            return Arrays.asList(lbl);
        }

        @Override protected void callback(IIncrementalTypeCheckerContext<Scope, Integer, IDatum, R> unit, Scope root) {
            final Scope d = unit.freshScope("d", Arrays.asList(), true, false);
            unit.addEdge(root, lbl, d);
            unit.closeEdge(root, lbl);
            unit.setDatum(d, d);
        }

    }

    ///////////////////////////////////////////////////////////////////////////
    // Initial state builder utils
    ///////////////////////////////////////////////////////////////////////////

    private UnitResult.Builder<Scope, Integer, IDatum, Unit> rootResult() {
        return UnitResult.<Scope, Integer, IDatum, Unit>builder()
                .id("/.")
                .scopeGraph(ScopeGraph.Immutable.of())
                .localScopeGraph(ScopeGraph.Immutable.of())
                .analysis(Unit.unit);
    }

    private UnitResult.Builder<Scope, Integer, IDatum, Unit> subResult(String id, Scope root) {
        return UnitResult.<Scope, Integer, IDatum, Unit>builder()
                .id(id)
                .addRootScopes(root)
                .scopeGraph(ScopeGraph.Immutable.of())
                .localScopeGraph(ScopeGraph.Immutable.of())
                .analysis(Unit.unit);
    }

}
