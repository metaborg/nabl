package mb.p_raffrayi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.MultiSet;
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
import mb.p_raffrayi.impl.StateCapture;
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

    private static final Result<Integer, Unit> UNIT_RESULT = Result.of(Unit.unit);

    @Test(timeout = 10000) public void testSimpleRelease() throws InterruptedException, ExecutionException {
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> previousResult = rootResult().build();

        final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI>> future =
                this.run(new NoopTypeChecker(".", false), Set.Immutable.<Integer>of(), previousResult);

        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> result =
                future.asJavaCompletion().get();

        assertEquals(TransitionTrace.RELEASED, result.stateTransitionTrace());
        assertTrue(result.failures().isEmpty());
    }

    @Test(timeout = 10000) public void testReleaseChild_ParentCached() throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> childResult = subResult("/./sub", root)
            .addQueries(recordedQuery(root).build())
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> parentResult = rootResult(root)
            .putSubUnitResults("sub", childResult)
            .build();
        // @formatter:on

        // @formatter:off
        final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI>> future = this.run(
            new ComposedRootTypeChecker<>(false,
                new SingleQueryTypeChecker("sub", false)
            ), Set.Immutable.of(), parentResult);
        // @formatter:on

        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> result =
                future.asJavaCompletion().get();
        assertTrue(result.failures().isEmpty());

        assertEquals(TransitionTrace.RELEASED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.RELEASED, result.subUnitResults().get("sub").stateTransitionTrace());
    }

    @Test(timeout = 10000) public void testReleaseChild_ParentChanged()
            throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> childResult = subResult("/./sub", root)
            .addQueries(recordedQuery(root).build())
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> parentResult = rootResult(root)
            .putSubUnitResults("sub", childResult)
            .build();
        // @formatter:on

        // @formatter:off
        final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI>> future = this.run(
            new ComposedRootTypeChecker<>(true,
                new SingleQueryTypeChecker("sub", false)
            ), Set.Immutable.of(), parentResult);
        // @formatter:on

        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> result =
                future.asJavaCompletion().get();
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
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> childResult = subResult("/./sub", root)
            .addScopes(d)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
            .addQueries(recordedQuery(root, env).build())
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> parentResult = rootResult(root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d))
            .addQueries(recordedQuery(root, env).build())
            .putSubUnitResults("sub", childResult)
            .build();
        // @formatter:on

        // @formatter:off
        final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI>> future = this.run(
            new ComposedRootTypeChecker<>(false,
                new DeclQueryTypeChecker("sub", false, lbl)
            ), Set.Immutable.of(lbl), parentResult);
        // @formatter:on

        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> result =
                future.asJavaCompletion().get();
        assertTrue(result.failures().isEmpty());

        assertEquals(TransitionTrace.RELEASED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.RELEASED, result.subUnitResults().get("sub").stateTransitionTrace());
    }

    @Test(timeout = 10000) public void
            testRelease_MutualDep_ParentChanged() throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final Integer lbl = 1;
        final Scope d = new Scope("/./sub", 1);

        final ResolutionPath<Scope, Integer, IDatum> path =
                new ScopePath<Scope, Integer>(root).step(lbl, d).get().resolve(d);
        final Env<Scope, Integer, IDatum> env = Env.of(path);

        // @formatter:off
        final IScopeGraph.Immutable<Scope, Integer, IDatum> subLocalSG = ScopeGraph.Immutable.<Scope, Integer, IDatum>of()
                .addEdge(root, lbl, d)
                .setDatum(d, d);
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> childResult = subResult("/./sub", root)
            .addScopes(d)
            .scopeGraph(subLocalSG)
            .localScopeGraph(subLocalSG)
            .localState(StateCapture.<Scope, Integer, IDatum, EmptyI>builder()
                .scopes(CapsuleUtil.immutableSet(d))
                .scopeGraph(subLocalSG)
                .scopeNameCounters(MultiSet.Immutable.of("d"))
                .usedStableScopes(CapsuleUtil.immutableSet())
                .typeCheckerState(EmptyI.of())
                .build())
            .addQueries(recordedQuery(root, env).build())
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> parentResult = rootResult(root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d))
            .addQueries(recordedQuery(root, env).build())
            .putSubUnitResults("sub", childResult)
            .build();
        // @formatter:on

        // @formatter:off
        final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI>> future = this.run(
            new ComposedRootTypeChecker<>(true,
                new DeclQueryTypeChecker("sub", false, lbl)
            ), Set.Immutable.of(lbl), parentResult);
        // @formatter:on

        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> result =
                future.asJavaCompletion().get();
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
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> childResult = subResult("/./sub", root)
            .addScopes(d)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
            .addQueries(recordedQuery(root, env).build())
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> parentResult = rootResult(root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d))
            .addQueries(recordedQuery(root, env).build())
            .putSubUnitResults("sub", childResult)
            .build();
        // @formatter:on

        // @formatter:off
        final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI>> future = this.run(
            new ComposedRootTypeChecker<>(false,
                new DeclQueryTypeChecker("sub", true, lbl)
            ), Set.Immutable.of(lbl), parentResult);
        // @formatter:on

        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> result =
                future.asJavaCompletion().get();
        assertTrue(result.failures().isEmpty());

        assertEquals(TransitionTrace.RELEASED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.INITIALLY_STARTED, result.subUnitResults().get("sub").stateTransitionTrace());
    }

    @Test(timeout = 10000) public void testRelease_NewUnitChangesSharedEdge()
            throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final Integer lbl = 1;
        final IDatum datum = new IDatum() {};

        // @formatter:off
        final RecordedQuery<Scope, Integer, IDatum> rq = recordedQuery(root)
            .labelWf(NoAcceptLabelWf.instance)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> child1Result = subResult("/./sub1", root)
            .addQueries(rq)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> parentResult = rootResult(root)
            .putSubUnitResults("sub1", child1Result)
            .build();
        // @formatter:on

        // @formatter:off
        final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI>> future = this.run(
            new ComposedRootTypeChecker<>(false,
                new SingleQueryTypeChecker("sub1", false),
                new DeclTypeChecker("sub2", true, lbl, datum)
            ), Set.Immutable.of(lbl), parentResult);
        // @formatter:on

        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> result =
                future.asJavaCompletion().get();
        assertTrue(result.failures().isEmpty());

        assertEquals(TransitionTrace.RELEASED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.RELEASED, result.subUnitResults().get("sub1").stateTransitionTrace());
        assertEquals(TransitionTrace.INITIALLY_STARTED, result.subUnitResults().get("sub2").stateTransitionTrace());
    }

    @Test(timeout = 10000) public void testRelease_NestedDeclChanged() throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final Integer lbl = 1;
        final Scope si = new Scope("/./sub2", 122);
        final Scope d1 = new Scope("/./sub2", 123);
        final IDatum datum = new IDatum() {};

        final ScopePath<Scope, Integer> scopePath = new ScopePath<Scope, Integer>(root).step(lbl, si).get();
        final ResolutionPath<Scope, Integer, IDatum> path = scopePath.step(lbl, d1).get().resolve(d1);

        // @formatter:off
        final RecordedQuery<Scope, Integer, IDatum> rqTrans = recordedQuery(scopePath, Env.of(path))
            .labelWf(NoAcceptLabelWf.instance)
            .build();
        final RecordedQuery<Scope, Integer, IDatum> rq = recordedQuery(scopePath, Env.of(path))
            .labelWf(NoAcceptLabelWf.instance)
            .addTransitiveQueries(rqTrans)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> child1Result = subResult("/./sub1", root)
            .addQueries(rq)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> child2Result = subResult("/./sub2", root)
            .addScopes(si, d1)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of()
                .addEdge(root, lbl, si)
                .addEdge(si, lbl, d1)
                .setDatum(d1, d1))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of()
                .addEdge(root, lbl, si)
                .addEdge(si, lbl, d1)
                .setDatum(d1, d1))
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> parentResult = rootResult(root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of()
                .addEdge(root, lbl, si)
                .addEdge(si, lbl, d1))
            .putSubUnitResults("sub1", child1Result)
            .putSubUnitResults("sub2", child2Result)
            .build();
        // @formatter:on

        // @formatter:off
        final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI>> future = this.run(
            new ComposedRootTypeChecker<>(false,
                new SingleQueryTypeChecker("sub1", false),
                new NestedDeclTypeChecker("sub2", true, lbl, datum)
            ), Set.Immutable.of(lbl), parentResult);
        // @formatter:on

        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> result =
                future.asJavaCompletion().get();
        assertTrue(result.failures().isEmpty());

        assertEquals(TransitionTrace.RELEASED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.RELEASED, result.subUnitResults().get("sub1").stateTransitionTrace());
        assertEquals(TransitionTrace.INITIALLY_STARTED, result.subUnitResults().get("sub2").stateTransitionTrace());
    }

    @Test(timeout = 10000) public void testRelease_CyclicGraph() throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final Integer lbl = 1;
        final Scope s1 = new Scope("/./sub1", 122);
        final Scope s2 = new Scope("/./sub2", 123);
        final Scope d1 = new Scope("/./sub2", 124);
        final IDatum datum = new IDatum() {};

        final ScopePath<Scope, Integer> path2 = new ScopePath<Scope, Integer>(root).step(lbl, s1).get();
        final ScopePath<Scope, Integer> pathRoot = path2.step(lbl, s2).get();
        final ScopePath<Scope, Integer> path1 = pathRoot.step(lbl, d1).get();
        final ResolutionPath<Scope, Integer, IDatum> path = path2.resolve(datum);

        // @formatter:off
        final RecordedQuery<Scope, Integer, IDatum> q1 = recordedQuery(path1, Env.of(path))
            .labelWf(NoAcceptLabelWf.instance)
            .build();
        final RecordedQuery<Scope, Integer, IDatum> qr = recordedQuery(pathRoot, Env.of(path))
            .labelWf(NoAcceptLabelWf.instance)
            .build();
        final RecordedQuery<Scope, Integer, IDatum> q2 = recordedQuery(path2, Env.of(path))
            .labelWf(NoAcceptLabelWf.instance)
            .addTransitiveQueries(q1, qr)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> child1Result = subResult("/./sub1", root)
            .addScopes(s1)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of()
                .addEdge(root, lbl, s1)
                .addEdge(s1, lbl, s2))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of()
                .addEdge(root, lbl, s1)
                .addEdge(s1, lbl, s2))
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> child2Result = subResult("/./sub2", root)
            .addScopes(d1, s2)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of()
                .addEdge(s2, lbl, root)
                .addEdge(s2, lbl, d1)
                .setDatum(d1, datum))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of()
                .addEdge(s2, lbl, root)
                .addEdge(s2, lbl, d1)
                .setDatum(d1, datum))
            .addQueries(q2)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> parentResult = rootResult(root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of()
                .addEdge(root, lbl, s1))
            .putSubUnitResults("sub1", child1Result)
            .putSubUnitResults("sub2", child2Result)
            .build();
        // @formatter:on

        // @formatter:off
        final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI>> future = this.run(
            new ComposedRootTypeChecker<>(false,
                new NoopTypeChecker("sub1", false),
                new NoopTypeChecker("sub2", false)
            ), Set.Immutable.of(lbl), parentResult);
        // @formatter:on

        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> result =
                future.asJavaCompletion().get();
        assertTrue(result.failures().isEmpty());

        assertEquals(TransitionTrace.RELEASED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.RELEASED, result.subUnitResults().get("sub1").stateTransitionTrace());
        assertEquals(TransitionTrace.RELEASED, result.subUnitResults().get("sub2").stateTransitionTrace());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Restart conditions
    ///////////////////////////////////////////////////////////////////////////

    @Test(timeout = 10000) public void testSimpleRestart() throws InterruptedException, ExecutionException {
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> previousResult = rootResult().build();

        final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI>> future =
                this.run(new NoopTypeChecker(".", true), Set.Immutable.of(), previousResult);

        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> result =
                future.asJavaCompletion().get();

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
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> childResult = subResult("/./sub", root)
            .addScopes(d)
            .addQueries(recordedQuery(root, Env.of(path)).build())
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> parentResult = rootResult(root)
            .putSubUnitResults("sub", childResult)
            .build();
        // @formatter:on

        // @formatter:off
        final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI>> future = this.run(
            new ComposedSelfDeclRootTypeChecker<>(true, lbl,
                new SingleQueryTypeChecker("sub", false)
            ), Set.Immutable.of(lbl), parentResult);
        // @formatter:on

        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> result =
                future.asJavaCompletion().get();
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
        final RecordedQuery<Scope, Integer, IDatum> rqTrans = recordedQuery(scopePath, Env.of(path)).build();
        final RecordedQuery<Scope, Integer, IDatum> rq =
                recordedQuery(scopePath, Env.of(path)).addTransitiveQueries(rqTrans).build();

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> child1Result = subResult("/./sub1", root)
            .addQueries(rq)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> child2Result = subResult("/./sub2", root)
            .addScopes(d1)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> parentResult = rootResult(root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1))
            .putSubUnitResults("sub1", child1Result)
            .putSubUnitResults("sub2", child2Result)
            .build();
        // @formatter:on

        // @formatter:off
        final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI>> future = this.run(
            new ComposedRootTypeChecker<>(false,
                new SingleQueryTypeChecker("sub1", false),
                new DeclTypeChecker("sub2", true, lbl, datum)
            ), Set.Immutable.of(lbl), parentResult);
        // @formatter:on

        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> result =
                future.asJavaCompletion().get();
        assertTrue(result.failures().isEmpty());

        assertEquals(TransitionTrace.RELEASED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.RESTARTED, result.subUnitResults().get("sub1").stateTransitionTrace());
        assertEquals(TransitionTrace.INITIALLY_STARTED, result.subUnitResults().get("sub2").stateTransitionTrace());
    }

    @Test(timeout = 10000) public void testRestart_FailureInInitialState()
            throws InterruptedException, ExecutionException {
        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> previousResult = rootResult()
            .addFailures(new Exception())
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI>> future =
                this.run(new NoopTypeChecker(".", false), Set.Immutable.of(), previousResult);

        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> result =
                future.asJavaCompletion().get();

        assertTrue(result.failures().isEmpty());
        assertEquals(TransitionTrace.INITIALLY_STARTED, result.stateTransitionTrace());
    }

    @Test(timeout = 10000) public void testRestart_NewUnitChangesSharedEdge()
            throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final Integer lbl = 1;
        final IDatum datum = new IDatum() {};

        final RecordedQuery<Scope, Integer, IDatum> rq = recordedQuery(root).build();

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> child1Result = subResult("/./sub1", root)
            .addQueries(rq)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> parentResult = rootResult(root)
            .putSubUnitResults("sub1", child1Result)
            .build();
        // @formatter:on

        // @formatter:off
        final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI>> future = this.run(
            new ComposedRootTypeChecker<>(false,
                new SingleQueryTypeChecker("sub1", false),
                new DeclTypeChecker("sub2", true, lbl, datum)
            ), Set.Immutable.of(lbl), parentResult);
        // @formatter:on

        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> result =
                future.asJavaCompletion().get();
        assertTrue(result.failures().isEmpty());

        assertEquals(TransitionTrace.RELEASED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.RESTARTED, result.subUnitResults().get("sub1").stateTransitionTrace());
        assertEquals(TransitionTrace.INITIALLY_STARTED, result.subUnitResults().get("sub2").stateTransitionTrace());
    }

    @Test(timeout = 10000) public void testRestart_NestedDeclChanged() throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final Integer lbl = 1;
        final Scope si = new Scope("/./sub2", 122);
        final Scope d1 = new Scope("/./sub2", 123);
        final IDatum datum = new IDatum() {};

        final ScopePath<Scope, Integer> scopePath = new ScopePath<Scope, Integer>(root).step(lbl, si).get();
        final ResolutionPath<Scope, Integer, IDatum> path = scopePath.step(lbl, d1).get().resolve(d1);

        final RecordedQuery<Scope, Integer, IDatum> rqTrans = recordedQuery(scopePath, Env.of(path)).build();
        final RecordedQuery<Scope, Integer, IDatum> rq =
                recordedQuery(scopePath, Env.of(path)).addTransitiveQueries(rqTrans).build();

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> child1Result = subResult("/./sub1", root)
            .addQueries(rq)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> child2Result = subResult("/./sub2", root)
            .addScopes(si, d1)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of()
                .addEdge(root, lbl, si)
                .addEdge(si, lbl, d1)
                .setDatum(d1, d1))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of()
                .addEdge(root, lbl, si)
                .addEdge(si, lbl, d1)
                .setDatum(d1, d1))
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> parentResult = rootResult(root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of()
                .addEdge(root, lbl, si)
                .addEdge(si, lbl, d1))
            .putSubUnitResults("sub1", child1Result)
            .putSubUnitResults("sub2", child2Result)
            .build();
        // @formatter:on

        // @formatter:off
        final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI>> future = this.run(
            new ComposedRootTypeChecker<>(false,
                new SingleQueryTypeChecker("sub1", false),
                new NestedDeclTypeChecker("sub2", true, lbl, datum)
            ), Set.Immutable.of(lbl), parentResult);
        // @formatter:on

        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> result =
                future.asJavaCompletion().get();
        assertTrue(result.failures().isEmpty());

        assertEquals(TransitionTrace.RELEASED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.RESTARTED, result.subUnitResults().get("sub1").stateTransitionTrace());
        assertEquals(TransitionTrace.INITIALLY_STARTED, result.subUnitResults().get("sub2").stateTransitionTrace());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Query Recording behavior
    ///////////////////////////////////////////////////////////////////////////

    @Test(timeout = 10000) public void testRecord_SimpleQuery() throws InterruptedException, ExecutionException {
        final Integer lbl = 1;

        // @formatter:off
        final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI>> future = this.run(
            new ComposedSelfDeclRootTypeChecker<>(true, lbl,
                new SingleQueryTypeChecker("sub", true)
            ), Set.Immutable.of(lbl), null);
        // @formatter:on

        IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> result = future.asJavaCompletion().get();

        assertTrue(result.failures().isEmpty());
        assertEquals(1, result.subUnitResults().get("sub").queries().size());
    }

    @Test(timeout = 10000) public void testRecord_SharedDeclQuery() throws InterruptedException, ExecutionException {
        final Integer lbl = 1;
        final IDatum datum = new IDatum() {};

        // @formatter:off
        final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Integer, Integer>, EmptyI>> future = this.run(
            new ComposedRootTypeChecker<Integer>(true,
                new EnvSizeTypeChecker("sub1", true),
                new DeclTypeChecker("sub2", true, lbl, datum)
            ), Set.Immutable.of(lbl), null);
        // @formatter:on

        IUnitResult<Scope, Integer, IDatum, Result<Integer, Integer>, EmptyI> result = future.asJavaCompletion().get();

        assertTrue(result.failures().isEmpty());
        assertEquals(1, (int) result.analysis().value());

        assertEquals(1, result.subUnitResults().get("sub1").queries().size());
        assertEquals(1, result.subUnitResults().get("sub1").queries().iterator().next().transitiveQueries().size());
        assertEquals(0, result.queries().size());

    }

    @Test(timeout = 10000) public void testRecord_SharedScopeQuery() throws InterruptedException, ExecutionException {
        final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, Result<Integer, Unit>>> future =
                this.run(".", new ITypeChecker<Scope, Integer, IDatum, Result<Integer, Unit>, Result<Integer, Unit>>() {
                    @Override public IFuture<Result<Integer, Unit>> run(
                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, Result<Integer, Unit>, Result<Integer, Unit>> unit,
                            List<Scope> rootScopes) {
                        return unit.runIncremental(restarted -> {
                            final Scope s = unit.freshScope("s", Arrays.asList(), false, true);
                            final IFuture<Result<Integer, Unit>> future =
                                    unit.query(s, LabelWf.any(), LabelOrder.none(), DataWf.any(), DataLeq.any())
                                            .thenApply(__ -> UNIT_RESULT);
                            unit.closeScope(s);
                            return future;
                        });
                    }
                }, Arrays.asList());

        IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, Result<Integer, Unit>> result =
                future.asJavaCompletion().get();

        assertEquals(TransitionTrace.INITIALLY_STARTED, result.stateTransitionTrace());
        assertTrue(result.failures().isEmpty());
        assertEquals(1, result.queries().size());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Release behavior
    ///////////////////////////////////////////////////////////////////////////

    @Test(timeout = 10000) public void testQueryInReleasedUnit() throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final Integer lbl = 1;
        final Scope d = new Scope("/.", 1);

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> parentResult = rootResult(root)
            .addScopes(d)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
            .build();
        // @formatter:on

        // @formatter:off
        final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI>> future = this.run(
            new ComposedRootTypeChecker<Unit>(false,
                new DeclQueryTypeChecker("sub", true, lbl)
            ), Set.Immutable.of(lbl), parentResult);
        // @formatter:on

        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> result =
                future.asJavaCompletion().get();
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
        final IRecordedQuery<Scope, Integer, IDatum> query = recordedQuery(root, env).build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> childResult = subResult("/./sub", root)
            .addScopes(d)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
            .addQueries(query)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> parentResult = rootResult(root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d))
            .addQueries(query)
            .putSubUnitResults("sub", childResult)
            .build();
        // @formatter:on

        // @formatter:off
        final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI>> future = this.run(
            new ComposedRootTypeChecker<Unit>(false,
                new DeclQueryTypeChecker("sub", false, lbl)
            ), Set.Immutable.of(lbl), parentResult);
        // @formatter:on

        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> result =
                future.asJavaCompletion().get();
        final IUnitResult<Scope, Integer, IDatum, ?, ?> subResult = result.subUnitResults().get("sub");

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
        final IRecordedQuery<Scope, Integer, IDatum> query = recordedQuery(root, env).build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> childResult = subResult("/./sub", root)
            .addScopes(d)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
            .addQueries(query)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> parentResult = rootResult(root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d))
            .putSubUnitResults("sub", childResult)
            .addQueries(query)
            .build();
        // @formatter:on

        // @formatter:off
        final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI>> future = this.run(
            new ComposedRootTypeChecker<Unit>(false,
                new DeclQueryTypeChecker("sub", false, lbl)
            ), Set.Immutable.of(lbl), parentResult);
        // @formatter:on

        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> result =
                future.asJavaCompletion().get();

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
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> previousResult = rootResult(root)
            .addScopes(d)
            .scopeGraph(sg)
            .localScopeGraph(sg)
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI>> future =
                this.run(new NoopTypeChecker(".", false), Set.Immutable.of(), previousResult);

        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> result =
                future.asJavaCompletion().get();

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
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> childResult = subResult("/./sub", root)
            .addScopes(d1)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> parentResult = rootResult(root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1))
            .putSubUnitResults("sub", childResult)
            .build();
        // @formatter:on

        // @formatter:off
        final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI>> future = this.run(
            new ComposedRootTypeChecker<Unit>(false,
                new SelfDeclTypeChecker("sub", false, lbl)
            ), Set.Immutable.of(lbl), parentResult);
        // @formatter:on

        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> result =
                future.asJavaCompletion().get();
        final IUnitResult<Scope, Integer, IDatum, ?, ?> subResult = result.subUnitResults().get("sub");

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
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Integer>, EmptyI> parentResult = UnitResult.<Scope, Integer, IDatum, Result<Integer, Integer>, EmptyI>builder()
            .id("/.")
            .addScopes(d1)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .analysis(Result.of(-1))
            .build();
        // @formatter:on

        // @formatter:off
        final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Integer, Integer>, EmptyI>> future = this.run(
            new ComposedSelfDeclRootTypeChecker<Integer>(true, lbl,
                new EnvSizeTypeChecker("sub", true)
            ), Set.Immutable.of(lbl), parentResult);
        // @formatter:on

        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Integer>, EmptyI> result =
                future.asJavaCompletion().get();
        assertTrue(result.failures().isEmpty());
        assertEquals(1, (int) result.analysis().value());
    }

    @Test(timeout = 10000) public void testRestartChild_ParentUpdateSG()
            throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final Integer lbl = 1;
        final Scope d1 = new Scope("/./sub", 1);
        final IDatum datum = new IDatum() {};
        final IRecordedQuery<Scope, Integer, IDatum> query = recordedQuery(root).build();

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> childResult = subResult("/./sub", root)
            .addScopes(d1)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .addQueries(query)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> parentResult = rootResult(root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1))
            .addQueries(query)
            .putSubUnitResults("sub", childResult)
            .build();
        // @formatter:on

        // @formatter:off
        final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI>> future = this.run(
            new ComposedRootTypeChecker<Unit>(false,
                new DeclTypeChecker("sub", true, lbl, datum)
            ), Set.Immutable.of(lbl), parentResult);
        // @formatter:on

        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> result =
                future.asJavaCompletion().get();
        final IUnitResult<Scope, Integer, IDatum, ?, ?> subResult = result.subUnitResults().get("sub");

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

    @Test(timeout = 10000) public void testRestartCluster() throws InterruptedException, ExecutionException {
        final Integer lbl = 1;
        final Scope root = new Scope("/.", 0);

        final Scope s1 = new Scope("/./sub1", -1);
        final Scope s2 = new Scope("/./sub2", -2);
        final Scope s3 = new Scope("/./sub3", -3);

        final IRecordedQuery<Scope, Integer, IDatum> q1 = recordedQuery(s1).build();
        final IRecordedQuery<Scope, Integer, IDatum> q2 = recordedQuery(s2).build();
        final IRecordedQuery<Scope, Integer, IDatum> q3 = recordedQuery(s3).build();

        // @formatter:off
        final IScopeGraph.Immutable<Scope, Integer, IDatum> sg1 = ScopeGraph.Immutable.<Scope, Integer, IDatum>of()
            .addEdge(root, lbl, s1)
            .setDatum(s1, s1);
        // @formatter:on

        // @formatter:off
        final IScopeGraph.Immutable<Scope, Integer, IDatum> sg2 = ScopeGraph.Immutable.<Scope, Integer, IDatum>of()
            .addEdge(root, lbl, s2)
            .setDatum(s2, s2);
        // @formatter:on

        // @formatter:off
        final IScopeGraph.Immutable<Scope, Integer, IDatum> sg3 = ScopeGraph.Immutable.<Scope, Integer, IDatum>of()
            .addEdge(root, lbl, s3)
            .setDatum(s3, s3);
        // @formatter:on

        // @formatter:off
        final IScopeGraph.Immutable<Scope, Integer, IDatum> sgRoot = ScopeGraph.Immutable.<Scope, Integer, IDatum>of()
            .addEdge(root, lbl, s1)
            .addEdge(root, lbl, s2)
            .addEdge(root, lbl, s3);
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> sub1Result = subResult("/./sub1", root)
            .localScopeGraph(sg1)
            .scopeGraph(sg1)
            .addScopes(root, s1)
            .addQueries(q2, q3)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> sub2Result = subResult("/./sub2", root)
            .localScopeGraph(sg2)
            .scopeGraph(sg2)
            .addScopes(root, s2)
            .addQueries(q1, q3)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> sub3Result = subResult("/./sub3", root)
            .localScopeGraph(sg3)
            .scopeGraph(sg3)
            .addScopes(root, s3)
            .addQueries(q1, q2)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> rootResult = rootResult(root)
            .localScopeGraph(ScopeGraph.Immutable.of())
            .scopeGraph(sgRoot)
            .addScopes(root)
            .putSubUnitResults("sub1", sub1Result)
            .putSubUnitResults("sub2", sub2Result)
            .putSubUnitResults("sub3", sub3Result)
            .build();
        // @formatter:on

        // @formatter:off
        final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI>> future = this.run(
            new ComposedRootTypeChecker<>(false,
                new TestTypeChecker<Unit>("sub1", true) {

                    @Override public IFuture<Result<Integer, Unit>> run(
                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> unit,
                            List<Scope> rootScopes) {
                        unit.initScope(rootScopes.get(0), Arrays.asList(), false);
                        return unit.runIncremental(p -> CompletableFuture.completedFuture(Result.of(Unit.unit)));
                    }

                },
                new NoopTypeChecker("sub2", false),
                new NoopTypeChecker("sub3", false)
            ),
            Arrays.asList(lbl), rootResult);
        // @formatter:on

        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> result =
                future.asJavaCompletion().get();

        assertTrue(result.allFailures().isEmpty());

        assertEquals(TransitionTrace.RELEASED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.INITIALLY_STARTED, result.subUnitResults().get("sub1").stateTransitionTrace());
        assertEquals(TransitionTrace.RELEASED, result.subUnitResults().get("sub2").stateTransitionTrace());
        assertEquals(TransitionTrace.RELEASED, result.subUnitResults().get("sub3").stateTransitionTrace());
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
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> sub1Result = subResult("/./sub1", root)
            .addQueries(recordedQuery(d1).build())
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> sub2Result = subResult("/./sub2", root)
            .addScopes(d1)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> parentResult = rootResult(root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1))
            .putSubUnitResults("sub1", sub1Result)
            .putSubUnitResults("sub2", sub2Result)
            .analysis(UNIT_RESULT)
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI>> future =
                this.run(".", new ITypeChecker<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI>() {

                    @Override public IFuture<Result<Integer, Unit>> run(
                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> unit,
                            List<Scope> roots) {
                        final Scope s = unit.stableFreshScope("s", Arrays.asList(lbl), false);
                        final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI>> sub1Future =
                                unit.add("sub1", new NoopTypeChecker("sub1", false), Arrays.asList(s), false);

                        // Do some 'heavy work', ensure request for unit 2 ref from unit 1 is executed by now.
                        try {
                            Thread.sleep(100);
                        } catch(InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI>> sub2Future =
                                unit.add("sub2", new NoopTypeChecker("sub2", false), Arrays.asList(s), false);

                        unit.closeScope(s);

                        return unit.runIncremental(restarted -> CompletableFuture.completedFuture(UNIT_RESULT))
                                .thenCompose(res -> sub2Future.thenCompose(s2res -> sub1Future))
                                .thenApply(IUnitResult::analysis);
                    }

                }, Set.Immutable.of(lbl), false, parentResult);

        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> result =
                future.asJavaCompletion().get();

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
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> sub1Result = subResult("/./sub1", root)
            .addQueries(recordedQuery(d1).build())
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> sub2subResult = subResult("/./sub2/sub", root)
            .addScopes(d1)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> sub2Result = subResult("/./sub2", root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .putSubUnitResults("sub", sub2subResult)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> parentResult = rootResult(root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, Integer, IDatum>of().addEdge(root, lbl, d1))
            .putSubUnitResults("sub1", sub1Result)
            .putSubUnitResults("sub2", sub2Result)
            .build();
        // @formatter:on

        // @formatter:off
        final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI>> future = this.run(
            new ComposedRootTypeChecker<Unit>(false,
                new NoopTypeChecker("sub1", false)
            ), Set.Immutable.of(lbl), parentResult);
        // @formatter:on

        final IUnitResult<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> result =
                future.asJavaCompletion().get();
        assertTrue(result.allFailures().isEmpty());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Type Checkers
    ///////////////////////////////////////////////////////////////////////////

    private final class NoopTypeChecker extends TestTypeChecker<Unit> {
        protected NoopTypeChecker(String id, boolean changed) {
            super(id, changed);
        }

        @Override public IFuture<Result<Integer, Unit>> run(
                IIncrementalTypeCheckerContext<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> unit,
                List<Scope> roots) {
            return unit.runIncremental(restarted -> CompletableFuture.completedFuture(UNIT_RESULT));
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

        @Override public IFuture<Result<Integer, Unit>> run(
                IIncrementalTypeCheckerContext<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> unit,
                List<Scope> rootScopes) {
            final Scope s1 = rootScopes.get(0);
            unit.initScope(s1, Arrays.asList(lbl), false);
            return unit.runIncremental(restarted -> {
                final Scope d = unit.freshScope("d", Arrays.asList(), true, false);
                unit.addEdge(s1, lbl, d);
                unit.setDatum(d, datum);
                unit.closeEdge(s1, lbl);
                return CompletableFuture.completedFuture(UNIT_RESULT);
            });
        }
    }

    private final class NestedDeclTypeChecker extends TestTypeChecker<Unit> {

        private final Integer lbl;
        private final IDatum datum;

        private NestedDeclTypeChecker(String id, boolean changed, Integer lbl, IDatum datum) {
            super(id, changed);
            this.lbl = lbl;
            this.datum = datum;
        }

        @Override public IFuture<Result<Integer, Unit>> run(
                IIncrementalTypeCheckerContext<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> unit,
                List<Scope> rootScopes) {
            final Scope s1 = rootScopes.get(0);
            unit.initScope(s1, Arrays.asList(lbl), false);
            return unit.runIncremental(restarted -> {
                final Scope si = unit.freshScope("s", Arrays.asList(lbl), false, false);
                unit.addEdge(s1, lbl, si);
                unit.closeEdge(s1, lbl);

                final Scope d = unit.freshScope("s", Arrays.asList(), true, false);
                unit.addEdge(si, lbl, d);
                unit.setDatum(d, datum);
                unit.closeEdge(si, lbl);

                return CompletableFuture.completedFuture(UNIT_RESULT);
            });
        }
    }

    private final class SelfDeclTypeChecker extends TestTypeChecker<Unit> {
        private final Integer lbl;

        private SelfDeclTypeChecker(String id, boolean changed, Integer lbl) {
            super(id, changed);
            this.lbl = lbl;
        }

        @Override public IFuture<Result<Integer, Unit>> run(
                IIncrementalTypeCheckerContext<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> unit,
                List<Scope> rootScopes) {
            final Scope s1 = rootScopes.get(0);
            unit.initScope(s1, Arrays.asList(lbl), false);
            return unit.runIncremental(restarted -> {
                final Scope d = unit.freshScope("d", Arrays.asList(), true, false);
                unit.addEdge(s1, lbl, d);
                unit.setDatum(d, d);
                unit.closeEdge(s1, lbl);
                return CompletableFuture.completedFuture(UNIT_RESULT);
            });
        }
    }

    private final class SingleQueryTypeChecker extends TestTypeChecker<Unit> {

        public SingleQueryTypeChecker(String id, boolean changed) {
            super(id, changed);
        }

        @Override public IFuture<Result<Integer, Unit>> run(
                IIncrementalTypeCheckerContext<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> unit,
                List<Scope> rootScopes) {
            final Scope s1 = rootScopes.get(0);
            unit.initScope(s1, Arrays.asList(), false);
            return unit.runIncremental(restarted -> {
                return unit.query(s1, LabelWf.any(), LabelOrder.none(), DataWf.any(), DataLeq.any())
                        .thenApply(__ -> Unit.unit).thenApply(Result::of);
            });
        }
    }

    private final class DeclQueryTypeChecker extends TestTypeChecker<Unit> {

        private final Integer lbl;

        private DeclQueryTypeChecker(String id, boolean changed, Integer lbl) {
            super(id, changed);
            this.lbl = lbl;
        }

        @Override public IFuture<Result<Integer, Unit>> run(
                IIncrementalTypeCheckerContext<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> unit,
                List<Scope> rootScopes) {
            final Scope s1 = rootScopes.get(0);
            unit.initScope(s1, Arrays.asList(lbl), false);
            return unit.runIncremental(restarted -> {
                final Scope d = unit.freshScope("d", Arrays.asList(), true, false);
                unit.setDatum(d, d);
                unit.addEdge(s1, lbl, d);
                unit.closeEdge(s1, lbl);

                return unit.query(s1, LabelWf.any(), LabelOrder.none(), DataWf.any(), DataLeq.any())
                        .thenApply(__ -> Unit.unit).thenApply(Result::of);
            });
        }
    }

    private final class EnvSizeTypeChecker extends TestTypeChecker<Integer> {

        protected EnvSizeTypeChecker(String id, boolean changed) {
            super(id, changed);
        }

        @Override public IFuture<Result<Integer, Integer>> run(
                IIncrementalTypeCheckerContext<Scope, Integer, IDatum, Result<Integer, Integer>, EmptyI> unit,
                List<Scope> rootScopes) {
            final Scope s1 = rootScopes.get(0);
            unit.initScope(s1, Arrays.asList(), false);
            return unit.runIncremental(restarted -> {
                return unit.query(s1, LabelWf.any(), LabelOrder.none(), DataWf.any(), DataLeq.any())
                        .thenApply(env -> env.size()).thenApply(Result::of);
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

        @Override public IFuture<Result<Integer, R>> run(
                IIncrementalTypeCheckerContext<Scope, Integer, IDatum, Result<Integer, R>, EmptyI> unit,
                List<Scope> rootScopes) {
            final Scope root = unit.stableFreshScope("s", rootLabels(), false);

            // Start subunits
            final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Integer, R>, EmptyI>> result =
                    unit.add(typeChecker.getId(), typeChecker, Arrays.asList(root), typeChecker.isChanged());
            final IFuture<?> otherResults = AggregateFuture.forAll(typeCheckers,
                    tc -> unit.add(tc.getId(), tc, Arrays.asList(root), tc.isChanged()));

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

        protected void callback(IIncrementalTypeCheckerContext<Scope, Integer, IDatum, Result<Integer, R>, EmptyI> unit,
                Scope root) {
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

        @Override protected void callback(
                IIncrementalTypeCheckerContext<Scope, Integer, IDatum, Result<Integer, R>, EmptyI> unit, Scope root) {
            final Scope d = unit.freshScope("d", Arrays.asList(), true, false);
            unit.addEdge(root, lbl, d);
            unit.closeEdge(root, lbl);
            unit.setDatum(d, d);
        }

    }

    ///////////////////////////////////////////////////////////////////////////
    // Initial state builder utils
    ///////////////////////////////////////////////////////////////////////////

    private UnitResult.Builder<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> rootResult() {
        // @formatter:off
        return UnitResult.<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI>builder()
                .id("/.")
                .scopeGraph(ScopeGraph.Immutable.of())
                .localScopeGraph(ScopeGraph.Immutable.of())
                .analysis(UNIT_RESULT);
        // @formatter:on
    }

    private UnitResult.Builder<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> rootResult(Scope root) {
        return rootResult().addScopes(root);
    }

    private UnitResult.Builder<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI> subResult(String id, Scope root) {
        // @formatter:off
        return UnitResult.<Scope, Integer, IDatum, Result<Integer, Unit>, EmptyI>builder()
                .id(id)
                .addRootScopes(root)
                .addScopes(root)
                .scopeGraph(ScopeGraph.Immutable.of())
                .localScopeGraph(ScopeGraph.Immutable.of())
                .analysis(UNIT_RESULT);
        // @formatter:on
    }

    private RecordedQuery.Builder<Scope, Integer, IDatum> recordedQuery() {
        // @formatter:off
        return RecordedQuery.<Scope, Integer, IDatum>builder()
            .labelWf(LabelWf.any())
            .dataWf(DataWf.any())
            .empty(true);
        // @formatter:on
    }

    private RecordedQuery.Builder<Scope, Integer, IDatum> recordedQuery(ScopePath<Scope, Integer> path) {
        return recordedQuery().scopePath(path);
    }

    private RecordedQuery.Builder<Scope, Integer, IDatum> recordedQuery(Scope root) {
        return recordedQuery(new ScopePath<PRaffrayiTestBase.Scope, Integer>(root));
    }

    private RecordedQuery.Builder<Scope, Integer, IDatum> recordedQuery(ScopePath<Scope, Integer> path,
            Env<Scope, Integer, IDatum> env) {
        return recordedQuery(path).empty(env.isEmpty());
    }

    private RecordedQuery.Builder<Scope, Integer, IDatum> recordedQuery(Scope root, Env<Scope, Integer, IDatum> env) {
        return recordedQuery(root).empty(env.isEmpty());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Custom Query Arguments
    ///////////////////////////////////////////////////////////////////////////

    private static class NoAcceptLabelWf implements LabelWf<Integer> {

        public static final NoAcceptLabelWf instance = new NoAcceptLabelWf();

        private NoAcceptLabelWf() {
        }

        @Override public Optional<LabelWf<Integer>> step(Integer l) {
            return Optional.of(this);
        }

        @Override public boolean accepting() {
            return false;
        }

    }
}
