package mb.p_raffrayi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.Ignore;
import org.junit.Test;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;
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
        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> previousResult = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/.")
            .scopeGraph(ScopeGraph.Immutable.of())
            .localScopeGraph(ScopeGraph.Immutable.of())
            .analysis(Unit.unit)
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> future =
                this.run(".", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                    @Override public IFuture<Unit> run(
                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit, List<Scope> roots) {
                        return unit.runIncremental(restarted -> CompletableFuture.completedFuture(Unit.unit));
                    }

                }, Set.Immutable.of(), false, previousResult);

        final IUnitResult<Scope, IDatum, IDatum, Unit> result = future.asJavaCompletion().get();

        assertEquals(TransitionTrace.RELEASED, result.stateTransitionTrace());
        assertTrue(result.failures().isEmpty());
    }

    @Test(timeout = 10000) public void testReleaseChild_ParentCached() throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);

        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> childResult = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/./sub")
            .addRootScopes(root)
            .scopeGraph(ScopeGraph.Immutable.of())
            .localScopeGraph(ScopeGraph.Immutable.of())
            .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), Env.empty()))
            .analysis(Unit.unit)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> parentResult = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/.")
            .scopeGraph(ScopeGraph.Immutable.of())
            .localScopeGraph(ScopeGraph.Immutable.of())
            .analysis(Unit.unit)
            .putSubUnitResults("sub", childResult)
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> future =
                this.run(".", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                    @Override public IFuture<Unit> run(
                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit, List<Scope> roots) {
                        final Scope s = unit.freshScope("s", Arrays.asList(), false, true);
                        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> subResult =
                                unit.add("sub", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                                    @Override public IFuture<Unit> run(
                                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit,
                                            List<Scope> rootScopes) {
                                        final Scope s1 = rootScopes.get(0);
                                        unit.initScope(s1, Arrays.asList(), false);
                                        return unit.runIncremental(restarted -> {
                                            return unit.query(s1, LabelWf.any(), LabelOrder.none(), DataWf.any(),
                                                    DataLeq.any()).thenApply(__ -> Unit.unit);
                                        });
                                    }
                                }, Arrays.asList(s), false);

                        unit.closeScope(s);

                        return unit.runIncremental(restarted -> CompletableFuture.completedFuture(Unit.unit))
                                .thenCompose(__ -> subResult.thenApply(IUnitResult::analysis));
                    }

                }, Set.Immutable.of(), false, parentResult);

        final IUnitResult<Scope, IDatum, IDatum, Unit> result = future.asJavaCompletion().get();
        assertTrue(result.failures().isEmpty());

        assertEquals(TransitionTrace.RELEASED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.RELEASED, result.subUnitResults().get("sub").stateTransitionTrace());
    }

    @Test(timeout = 10000) public void testReleaseChild_ParentChanged() throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);

        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> childResult = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/./sub")
            .addRootScopes(root)
            .scopeGraph(ScopeGraph.Immutable.of())
            .localScopeGraph(ScopeGraph.Immutable.of())
            .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), Env.empty()))
            .analysis(Unit.unit)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> parentResult = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/.")
            .scopeGraph(ScopeGraph.Immutable.of())
            .localScopeGraph(ScopeGraph.Immutable.of())
            .putSubUnitResults("sub", childResult)
            .analysis(Unit.unit)
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> future =
                this.run(".", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                    @Override public IFuture<Unit> run(
                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit, List<Scope> roots) {
                        final Scope s = unit.freshScope("s", Arrays.asList(), false, true);

                // @formatter:off
                        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> subResult = unit.add("sub", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                            @Override public IFuture<Unit> run(
                                    IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit, List<Scope> rootScopes) {
                                final Scope s1 = rootScopes.get(0);
                                unit.initScope(s1, Arrays.asList(), false);
                                return unit.runIncremental(restarted -> {
                                    return unit.query(s1, LabelWf.any(), LabelOrder.none(), DataWf.any(), DataLeq.any()).thenApply(__ -> Unit.unit);
                                });
                            }
                        }, Arrays.asList(s), false);
                        // @formatter:on

                        unit.closeScope(s);

                        return unit.runIncremental(restarted -> CompletableFuture.completedFuture(Unit.unit))
                                .thenCompose(__ -> subResult.thenApply(IUnitResult::analysis));
                    }

                }, Set.Immutable.of(), true, parentResult);

        final IUnitResult<Scope, IDatum, IDatum, Unit> result = future.asJavaCompletion().get();
        assertTrue(result.failures().isEmpty());

        assertEquals(TransitionTrace.INITIALLY_STARTED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.RELEASED, result.subUnitResults().get("sub").stateTransitionTrace());
    }

    @Test(timeout = 10000) public void testRelease_MutualDep_Cached() throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final IDatum lbl = new IDatum() {};
        final Scope d = new Scope("/./sub", -1);

        final ResolutionPath<Scope, IDatum, IDatum> path =
                new ScopePath<Scope, IDatum>(root).step(lbl, d).get().resolve(d);
        final Env<Scope, IDatum, IDatum> env = Env.of(path);

        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> childResult = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/./sub")
            .addRootScopes(root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
            .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), env))
            .analysis(Unit.unit)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> parentResult = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/.")
            .scopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d))
            .localScopeGraph(ScopeGraph.Immutable.of())
            .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), env))
            .putSubUnitResults("sub", childResult)
            .analysis(Unit.unit)
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> future =
                this.run(".", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                    @Override public IFuture<Unit> run(
                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit, List<Scope> roots) {
                        final Scope s = unit.freshScope("s", Arrays.asList(), false, true);

                // @formatter:off
                        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> subResult = unit.add("sub", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                            @Override public IFuture<Unit> run(
                                    IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit, List<Scope> rootScopes) {
                                final Scope s1 = rootScopes.get(0);
                                unit.initScope(s1, Arrays.asList(lbl), false);
                                return unit.runIncremental(restarted -> {
                                    final Scope d = unit.freshScope("d", Arrays.asList(), true, false);
                                    unit.setDatum(d, d);
                                    unit.addEdge(s1, lbl, d);
                                    unit.closeEdge(s1, lbl);

                                    return unit.query(s1, LabelWf.any(), LabelOrder.none(), DataWf.any(), DataLeq.any()).thenApply(__ -> Unit.unit);
                                });
                            }
                        }, Arrays.asList(s), false);
                        // @formatter:on

                        unit.closeScope(s);

                        return unit.runIncremental(restarted -> CompletableFuture.completedFuture(Unit.unit))
                                .thenCompose(res -> subResult.thenApply(IUnitResult::analysis));
                    }

                }, Set.Immutable.of(lbl), false, parentResult);

        final IUnitResult<Scope, IDatum, IDatum, Unit> result = future.asJavaCompletion().get();
        assertTrue(result.failures().isEmpty());

        assertEquals(TransitionTrace.RELEASED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.RELEASED, result.subUnitResults().get("sub").stateTransitionTrace());
    }

    @Ignore("Requires early validation of shared edges") @Test(timeout = 10000) public void
            testRelease_MutualDep_ParentChanged() throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final IDatum lbl = new IDatum() {};
        final Scope d = new Scope("/./sub", 1);

        final ResolutionPath<Scope, IDatum, IDatum> path =
                new ScopePath<Scope, IDatum>(root).step(lbl, d).get().resolve(d);
        final Env<Scope, IDatum, IDatum> env = Env.of(path);

        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> childResult = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/./sub")
            .addRootScopes(root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
            .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), env))
            .analysis(Unit.unit)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> parentResult = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/.")
            .scopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d))
            .localScopeGraph(ScopeGraph.Immutable.of())
            .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), env))
            .putSubUnitResults("sub", childResult)
            .analysis(Unit.unit)
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> future =
                this.run(".", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                    @Override public IFuture<Unit> run(
                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit, List<Scope> roots) {
                        final Scope s = unit.freshScope("s", Arrays.asList(), false, true);
                        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> subResult =
                                unit.add("sub", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                                    @Override public IFuture<Unit> run(
                                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit,
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
                                }, Arrays.asList(s), false);

                        unit.closeScope(s);

                        return unit.runIncremental(restarted -> CompletableFuture.completedFuture(Unit.unit))
                                .thenCompose(res -> subResult.thenApply(IUnitResult::analysis));
                    }

                }, Set.Immutable.of(lbl), true, parentResult);

        final IUnitResult<Scope, IDatum, IDatum, Unit> result = future.asJavaCompletion().get();
        assertTrue(result.failures().isEmpty());

        assertEquals(TransitionTrace.INITIALLY_STARTED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.RELEASED, result.subUnitResults().get("sub").stateTransitionTrace());
    }

    @Test(timeout = 10000) public void testRelease_MutualDep_ChildChanged()
            throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final IDatum lbl = new IDatum() {};
        final Scope d = new Scope("/./sub", -1);

        final ResolutionPath<Scope, IDatum, IDatum> path =
                new ScopePath<Scope, IDatum>(root).step(lbl, d).get().resolve(d);
        final Env<Scope, IDatum, IDatum> env = Env.of(path);

        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> childResult = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/./sub")
            .addRootScopes(root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
            .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), env))
            .analysis(Unit.unit)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> parentResult = UnitResult .<Scope, IDatum, IDatum, Unit>builder()
            .id("/.")
            .scopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d))
            .localScopeGraph(ScopeGraph.Immutable.of())
            .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), env))
            .putSubUnitResults("sub", childResult)
            .analysis(Unit.unit)
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> future =
                this.run(".", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                    @Override public IFuture<Unit> run(
                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit, List<Scope> roots) {
                        final Scope s = unit.freshScope("s", Arrays.asList(), false, true);
                        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> subResult =
                                unit.add("sub", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                                    @Override public IFuture<Unit> run(
                                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit,
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
                                }, Arrays.asList(s), true);

                        unit.closeScope(s);

                        return unit.runIncremental(restarted -> CompletableFuture.completedFuture(Unit.unit))
                                .thenCompose(res -> subResult.thenApply(IUnitResult::analysis));
                    }

                }, Set.Immutable.of(lbl), false, parentResult);

        final IUnitResult<Scope, IDatum, IDatum, Unit> result = future.asJavaCompletion().get();
        assertTrue(result.failures().isEmpty());

        assertEquals(TransitionTrace.RELEASED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.INITIALLY_STARTED, result.subUnitResults().get("sub").stateTransitionTrace());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Restart conditions
    ///////////////////////////////////////////////////////////////////////////

    @Test(timeout = 10000) public void testSimpleRestart() throws InterruptedException, ExecutionException {
        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> previousResult = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/.")
            .scopeGraph(ScopeGraph.Immutable.of())
            .localScopeGraph(ScopeGraph.Immutable.of())
            .analysis(Unit.unit)
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> future =
                this.run(".", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                    @Override public IFuture<Unit> run(
                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit, List<Scope> roots) {
                        return unit.runIncremental(restarted -> CompletableFuture.completedFuture(Unit.unit));
                    }

                }, Set.Immutable.of(), true, previousResult);

        final IUnitResult<Scope, IDatum, IDatum, Unit> result = future.asJavaCompletion().get();

        assertTrue(result.failures().isEmpty());
        assertEquals(TransitionTrace.INITIALLY_STARTED, result.stateTransitionTrace());
    }

    @Test(timeout = 10000) public void testRestartChild_ParentChanged()
            throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final IDatum lbl = new IDatum() {};
        final Scope d = new Scope("/./sub", 1);
        final ResolutionPath<Scope, IDatum, IDatum> path =
                new ScopePath<Scope, IDatum>(root).step(lbl, d).get().resolve(d);

        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> childResult = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/./sub").addRootScopes(root)
            .scopeGraph(ScopeGraph.Immutable.of())
            .localScopeGraph(ScopeGraph.Immutable.of())
            .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), Env.of(path)))
            .analysis(Unit.unit)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> parentResult = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/.")
            .scopeGraph(ScopeGraph.Immutable.of())
            .localScopeGraph(ScopeGraph.Immutable.of())
            .putSubUnitResults("sub", childResult)
            .analysis(Unit.unit)
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> future =
                this.run(".", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                    @Override public IFuture<Unit> run(
                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit, List<Scope> roots) {
                        final Scope s = unit.freshScope("s", Arrays.asList(lbl), false, true);
                        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> subResult =
                                unit.add("sub", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                                    @Override public IFuture<Unit> run(
                                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit,
                                            List<Scope> rootScopes) {
                                        final Scope s1 = rootScopes.get(0);
                                        unit.initScope(s1, Arrays.asList(), false);
                                        return unit.runIncremental(restarted -> {
                                            return unit.query(s1, LabelWf.any(), LabelOrder.none(), DataWf.any(),
                                                    DataLeq.any()).thenApply(__ -> Unit.unit);
                                        });
                                    }
                                }, Arrays.asList(s), false);

                        unit.closeScope(s);

                        return unit.runIncremental(restarted -> {
                            final Scope d = unit.freshScope("d", Arrays.asList(), true, false);
                            unit.addEdge(s, lbl, d);
                            unit.closeEdge(s, lbl);
                            unit.setDatum(d, d);

                            return CompletableFuture.completedFuture(Unit.unit);
                        }).thenCompose(res -> subResult.thenApply(IUnitResult::analysis));
                    }

                }, Set.Immutable.of(lbl), true, parentResult);

        final IUnitResult<Scope, IDatum, IDatum, Unit> result = future.asJavaCompletion().get();
        assertTrue(result.failures().isEmpty());

        assertEquals(TransitionTrace.INITIALLY_STARTED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.RESTARTED, result.subUnitResults().get("sub").stateTransitionTrace());
    }

    @Test(timeout = 10000) public void testRestart_SharedEdgeChanged() throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final IDatum lbl = new IDatum() {};
        final Scope d1 = new Scope("/./sub2", 123);
        final ScopePath<Scope, IDatum> scopePath = new ScopePath<Scope, IDatum>(root).step(lbl, d1).get();
        final ResolutionPath<Scope, IDatum, IDatum> path = scopePath.resolve(d1);
        final RecordedQuery<Scope, IDatum, IDatum> rqTrans = RecordedQuery.of(scopePath, LabelWf.any(), DataWf.any(),
                LabelOrder.none(), DataLeq.any(), Env.of(path));
        final RecordedQuery<Scope, IDatum, IDatum> rq =
                RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), Env.of(path))
                        .withTransitiveQueries(rqTrans);

        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> child1Result = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/./sub1")
            .addRootScopes(root)
            .scopeGraph(ScopeGraph.Immutable.of())
            .localScopeGraph(ScopeGraph.Immutable.of())
            .addQueries(rq)
            .analysis(Unit.unit)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> child2Result = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/./sub2")
            .addRootScopes(root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .analysis(Unit.unit)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> parentResult = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/.")
            .scopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d1))
            .localScopeGraph(ScopeGraph.Immutable.of())
            .putSubUnitResults("sub1", child1Result)
            .putSubUnitResults("sub2", child2Result)
            .analysis(Unit.unit)
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> future =
                this.run(".", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                    @Override public IFuture<Unit> run(
                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit, List<Scope> roots) {
                        final Scope s = unit.freshScope("s", Arrays.asList(), false, true);

                        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> sub1Result =
                                unit.add("sub1", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                                    @Override public IFuture<Unit> run(
                                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit,
                                            List<Scope> rootScopes) {
                                        final Scope s1 = rootScopes.get(0);
                                        unit.initScope(s1, Arrays.asList(), false);
                                        return unit.runIncremental(restarted -> {
                                            return unit.query(s1, LabelWf.any(), LabelOrder.none(), DataWf.any(),
                                                    DataLeq.any()).thenApply(__ -> Unit.unit);
                                        });
                                    }
                                }, Arrays.asList(s), false);

                        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> sub2Result =
                                unit.add("sub2", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                                    @Override public IFuture<Unit> run(
                                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit,
                                            List<Scope> rootScopes) {
                                        final Scope s1 = rootScopes.get(0);
                                        unit.initScope(s1, Arrays.asList(lbl), false);
                                        return unit.runIncremental(restarted -> {
                                            final Scope d = unit.freshScope("d", Arrays.asList(), true, false);
                                            unit.addEdge(s1, lbl, d);
                                            unit.setDatum(d, s1);
                                            unit.closeEdge(s1, lbl);
                                            return CompletableFuture.completedFuture(Unit.unit);
                                        });
                                    }
                                }, Arrays.asList(s), true);

                        unit.closeScope(s);

                        return unit.runIncremental(restarted -> CompletableFuture.completedFuture(Unit.unit))
                                .thenCompose(res -> sub2Result.thenCompose(sRes2 -> sub1Result))
                                    .thenApply(IUnitResult::analysis);
                    }

                }, Set.Immutable.of(lbl), false, parentResult);

        final IUnitResult<Scope, IDatum, IDatum, Unit> result = future.asJavaCompletion().get();
        assertTrue(result.failures().isEmpty());

        assertEquals(TransitionTrace.RELEASED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.RESTARTED, result.subUnitResults().get("sub1").stateTransitionTrace());
        assertEquals(TransitionTrace.INITIALLY_STARTED, result.subUnitResults().get("sub2").stateTransitionTrace());
    }

    @Test(timeout = 10000) public void testRestart_FailureInInitialState()
            throws InterruptedException, ExecutionException {
        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> previousResult = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/.")
            .scopeGraph(ScopeGraph.Immutable.of())
            .localScopeGraph(ScopeGraph.Immutable.of())
            .analysis(Unit.unit)
            .addFailures(new Exception())
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> future =
                this.run(".", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                    @Override public IFuture<Unit> run(
                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit, List<Scope> roots) {
                        return unit.runIncremental(restarted -> CompletableFuture.completedFuture(Unit.unit));
                    }

                }, Set.Immutable.of(), false, previousResult);

        final IUnitResult<Scope, IDatum, IDatum, Unit> result = future.asJavaCompletion().get();

        assertTrue(result.failures().isEmpty());
        assertEquals(TransitionTrace.INITIALLY_STARTED, result.stateTransitionTrace());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Query Recording behavior
    ///////////////////////////////////////////////////////////////////////////

    @Test(timeout = 10000) public void testRecord_SimpleQuery() throws InterruptedException, ExecutionException {
        final IDatum lbl = new IDatum() {};

        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> future =
                this.run(".", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                    @Override public IFuture<Unit> run(
                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit, List<Scope> roots) {
                        final Scope s = unit.freshScope("s", Arrays.asList(lbl), false, true);
                        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> subResult =
                                unit.add("sub", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                                    @Override public IFuture<Unit> run(
                                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit,
                                            List<Scope> rootScopes) {
                                        final Scope s1 = rootScopes.get(0);
                                        unit.initScope(s1, Arrays.asList(), false);
                                        return unit.runIncremental(restarted -> {
                                            return unit.query(s1, LabelWf.any(), LabelOrder.none(), DataWf.any(),
                                                    DataLeq.any()).thenApply(__ -> Unit.unit);
                                        });
                                    }
                                }, Arrays.asList(s));

                        unit.closeScope(s);

                        return unit.runIncremental(restarted -> {
                            final Scope d = unit.freshScope("d", Arrays.asList(), true, false);
                            unit.addEdge(s, lbl, d);
                            unit.closeEdge(s, lbl);
                            unit.setDatum(d, d);

                            return CompletableFuture.completedFuture(Unit.unit);
                        }).thenCompose(res -> subResult.thenApply(IUnitResult::analysis));
                    }

                }, Set.Immutable.of(lbl));

        IUnitResult<Scope, IDatum, IDatum, Unit> result = future.asJavaCompletion().get();

        assertTrue(result.failures().isEmpty());
        assertEquals(1, result.subUnitResults().get("sub").queries().size());
    }

    @Test(timeout = 10000) public void testRecord_SharedDeclQuery() throws InterruptedException, ExecutionException {
        final IDatum lbl = new IDatum() {};
        final IDatum d = new IDatum() {};

        final IFuture<IUnitResult<Scope, IDatum, IDatum, Integer>> future =
                this.run(".", new ITypeChecker<Scope, IDatum, IDatum, Integer>() {

                    @Override public IFuture<Integer> run(
                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Integer> unit, List<Scope> roots) {
                        final Scope s = unit.freshScope("s", Arrays.asList(), false, true);
                        final IFuture<IUnitResult<Scope, IDatum, IDatum, Integer>> sub1Result =
                                unit.add("sub1", new ITypeChecker<Scope, IDatum, IDatum, Integer>() {

                                    @Override public IFuture<Integer> run(
                                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Integer> unit,
                                            List<Scope> rootScopes) {
                                        final Scope s1 = rootScopes.get(0);
                                        unit.initScope(s1, Arrays.asList(), false);
                                        return unit.runIncremental(restarted -> {
                                            return unit.query(s1, LabelWf.any(), LabelOrder.none(), DataWf.any(),
                                                    DataLeq.any()).thenApply(env -> env.size());
                                        });
                                    }
                                }, Arrays.asList(s));
                        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> sub2Result =
                                unit.add("sub2", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                                    @Override public IFuture<Unit> run(
                                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit,
                                            List<Scope> rootScopes) {
                                        final Scope s1 = rootScopes.get(0);
                                        unit.initScope(s1, Arrays.asList(lbl), false);
                                        return unit.runIncremental(restarted -> {
                                            final Scope sd = unit.freshScope("d", Arrays.asList(), true, false);
                                            unit.setDatum(sd, d);
                                            unit.addEdge(s1, lbl, sd);
                                            unit.closeEdge(s1, lbl);
                                            return CompletableFuture.completedFuture(Unit.unit);
                                        });
                                    }
                                }, Arrays.asList(s));

                        unit.closeScope(s);

                        return unit.runIncremental(restarted -> sub1Result.thenApply(IUnitResult::analysis))
                                .thenCompose(res -> sub2Result.thenApply(__ -> res));
                    }

                }, Set.Immutable.of(lbl));

        IUnitResult<Scope, IDatum, IDatum, Integer> result = future.asJavaCompletion().get();

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
        final IDatum lbl = new IDatum() {};
        final Scope d = new Scope("/.", 1);

        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> parentResult = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/.")
            .scopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
            .analysis(Unit.unit)
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> future =
                this.run(".", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                    @Override public IFuture<Unit> run(
                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit, List<Scope> roots) {
                        final Scope s = unit.freshScope("s", Arrays.asList(), false, true);
                        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> subResult =
                                unit.add("sub", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                                    @Override public IFuture<Unit> run(
                                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit,
                                            List<Scope> rootScopes) {
                                        final Scope s1 = rootScopes.get(0);
                                        unit.initScope(s1, Arrays.asList(), false);
                                        return unit.runIncremental(restarted -> {
                                            return unit.query(s1, LabelWf.any(), LabelOrder.none(), DataWf.any(),
                                                    DataLeq.any()).thenApply(__ -> Unit.unit);
                                        });
                                    }
                                }, Arrays.asList(s));

                        unit.closeScope(s);

                        return unit.runIncremental(restarted -> CompletableFuture.completedFuture(Unit.unit))
                                .thenCompose(res -> subResult).thenApply(IUnitResult::analysis);
                    }

                }, Set.Immutable.of(lbl), false, parentResult);

        final IUnitResult<Scope, IDatum, IDatum, Unit> result = future.asJavaCompletion().get();
        assertTrue(result.failures().isEmpty());

        assertEquals(TransitionTrace.RELEASED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.INITIALLY_STARTED, result.subUnitResults().get("sub").stateTransitionTrace());
    }

    @Test(timeout = 10000) public void testReleasedUnit_ContainsRootScopes()
            throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 123);
        final IDatum lbl = new IDatum() {};
        final Scope d = new Scope("/./sub", 1);

        // @formatter:off
        final ResolutionPath<Scope, IDatum, IDatum> path = new ScopePath<Scope, IDatum>(root).step(lbl, d).get().resolve(d);
        final Env<Scope, IDatum, IDatum> env = Env.of(path);
        final IRecordedQuery<Scope, IDatum, IDatum> query = RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), env);
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> childResult = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/./sub")
            .addRootScopes(root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
            .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), env))
            .analysis(Unit.unit)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> parentResult = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/.")
            .scopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d))
            .localScopeGraph(ScopeGraph.Immutable.of()).addQueries(query)
            .putSubUnitResults("sub", childResult)
            .analysis(Unit.unit)
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> future =
                this.run(".", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                    @Override public IFuture<Unit> run(
                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit, List<Scope> roots) {
                        final Scope s = unit.freshScope("s", Arrays.asList(), false, true);
                        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> subResult =
                                unit.add("sub", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                                    @Override public IFuture<Unit> run(
                                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit,
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
                                }, Arrays.asList(s), false);

                        unit.closeScope(s);

                        return unit.runIncremental(restarted -> CompletableFuture.completedFuture(Unit.unit))
                                .thenCompose(res -> subResult).thenApply(IUnitResult::analysis);
                    }

                }, Set.Immutable.of(lbl), false, parentResult);

        final IUnitResult<Scope, IDatum, IDatum, Unit> result = future.asJavaCompletion().get();
        final IUnitResult<Scope, IDatum, IDatum, ?> subResult = result.subUnitResults().get("sub");

        assertEquals(TransitionTrace.RELEASED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.RELEASED, subResult.stateTransitionTrace());

        assertEquals(1, subResult.rootScopes().size());
        // Root scopes does not necessarily include `root`, but rather its match.
    }

    @Test(timeout = 10000) public void testReleasedUnit_ContainsQueries()
            throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final IDatum lbl = new IDatum() {};
        final Scope d = new Scope("/./sub", 1);

        // @formatter:off
        final ResolutionPath<Scope, IDatum, IDatum> path = new ScopePath<Scope, IDatum>(root).step(lbl, d).get().resolve(d);
        final Env<Scope, IDatum, IDatum> env = Env.of(path);
        final IRecordedQuery<Scope, IDatum, IDatum> query = RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), env);
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> childResult = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/./sub")
            .addRootScopes(root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
            .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), env))
            .analysis(Unit.unit)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> parentResult = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/.")
            .scopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d))
            .localScopeGraph(ScopeGraph.Immutable.of())
            .putSubUnitResults("sub", childResult)
            .addQueries(query)
            .analysis(Unit.unit)
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> future =
                this.run(".", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                    @Override public IFuture<Unit> run(
                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit, List<Scope> roots) {
                        final Scope s = unit.freshScope("s", Arrays.asList(), false, true);
                        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> subResult =
                                unit.add("sub", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                                    @Override public IFuture<Unit> run(
                                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit,
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
                                }, Arrays.asList(s), false);

                        unit.closeScope(s);

                        return unit.runIncremental(restarted -> CompletableFuture.completedFuture(Unit.unit))
                                .thenCompose(res -> subResult).thenApply(IUnitResult::analysis);
                    }

                }, Set.Immutable.of(lbl), false, parentResult);

        final IUnitResult<Scope, IDatum, IDatum, Unit> result = future.asJavaCompletion().get();

        assertEquals(TransitionTrace.RELEASED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.RELEASED, result.subUnitResults().get("sub").stateTransitionTrace());

        assertEquals("Invalid query count.", 1, result.queries().size());
        assertTrue("Query not recorded", result.queries().contains(query));
    }

    @Test(timeout = 10000) public void testReleasedUnit_ContainsLocalSG()
            throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final IDatum lbl = new IDatum() {};
        final Scope d = new Scope("/.", 1);

        // @formatter:off
        final IScopeGraph.Immutable<Scope, IDatum, IDatum> sg = ScopeGraph.Immutable.<Scope, IDatum, IDatum>of()
            .addEdge(root, lbl, d)
            .setDatum(d, d);
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> previousResult = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/.")
            .scopeGraph(sg)
            .localScopeGraph(sg)
            .analysis(Unit.unit)
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> future =
                this.run(".", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                    @Override public IFuture<Unit> run(
                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit, List<Scope> roots) {
                        return unit.runIncremental(restarted -> CompletableFuture.completedFuture(Unit.unit));
                    }

                }, Set.Immutable.of(), false, previousResult);

        final IUnitResult<Scope, IDatum, IDatum, Unit> result = future.asJavaCompletion().get();

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
        final IDatum lbl = new IDatum() {};
        final Scope d1 = new Scope("/./sub", 1);

        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> childResult = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/./sub")
            .addRootScopes(root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .analysis(Unit.unit)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> parentResult = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/.")
            .scopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d1))
            .localScopeGraph(ScopeGraph.Immutable.of())
            .putSubUnitResults("sub", childResult)
            .analysis(Unit.unit)
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> future =
                this.run(".", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                    @Override public IFuture<Unit> run(
                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit, List<Scope> roots) {
                        final Scope s = unit.freshScope("s", Arrays.asList(), false, true);
                        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> subResult =
                                unit.add("sub", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                                    @Override public IFuture<Unit> run(
                                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit,
                                            List<Scope> rootScopes) {
                                        final Scope s1 = rootScopes.get(0);
                                        unit.initScope(s1, Arrays.asList(lbl), false);
                                        return unit.runIncremental(restarted -> {
                                            final Scope dNew = unit.freshScope("d", Arrays.asList(), true, false);
                                            unit.setDatum(dNew, d1);
                                            unit.addEdge(s1, lbl, dNew);
                                            unit.closeEdge(s1, lbl);
                                            return CompletableFuture.completedFuture(Unit.unit);
                                        });
                                    }
                                }, Arrays.asList(s), false);

                        unit.closeScope(s);

                        return unit.runIncremental(restarted -> CompletableFuture.completedFuture(Unit.unit))
                                .thenCompose(res -> subResult).thenApply(IUnitResult::analysis);
                    }

                }, Set.Immutable.of(lbl), false, parentResult);

        final IUnitResult<Scope, IDatum, IDatum, Unit> result = future.asJavaCompletion().get();
        final IUnitResult<Scope, IDatum, IDatum, ?> subResult = result.subUnitResults().get("sub");

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
        final IDatum lbl = new IDatum() {};
        final Scope d1 = new Scope("/.", 1);
        final Scope d2 = new Scope("/.", 2);

        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Integer> parentResult = UnitResult.<Scope, IDatum, IDatum, Integer>builder()
            .id("/.")
            .scopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .analysis(-1)
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, IDatum, IDatum, Integer>> future =
                this.run(".", new ITypeChecker<Scope, IDatum, IDatum, Integer>() {

                    @Override public IFuture<Integer> run(
                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Integer> unit, List<Scope> roots) {
                        final Scope s = unit.freshScope("s", Arrays.asList(lbl), false, true);
                        final IFuture<IUnitResult<Scope, IDatum, IDatum, Integer>> subResult =
                                unit.add("sub", new ITypeChecker<Scope, IDatum, IDatum, Integer>() {

                                    @Override public IFuture<Integer> run(
                                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Integer> unit,
                                            List<Scope> rootScopes) {
                                        final Scope s1 = rootScopes.get(0);
                                        unit.initScope(s1, Arrays.asList(), false);
                                        return unit.runIncremental(restarted -> {
                                            return unit.query(s1, LabelWf.any(), LabelOrder.none(), DataWf.any(),
                                                    DataLeq.any()).thenApply(java.util.Set::size);
                                        });
                                    }
                                }, Arrays.asList(s));

                        unit.closeScope(s);

                        return unit.runIncremental(restarted -> {
                            final Scope d = unit.freshScope("d", Arrays.asList(), true, false);
                            unit.addEdge(s, lbl, d);
                            unit.setDatum(d, d2);
                            unit.closeEdge(s, lbl);

                            return CompletableFuture.completedFuture(-1);
                        }).thenCompose(res -> subResult.thenApply(IUnitResult::analysis));
                    }

                }, Set.Immutable.of(lbl), true, parentResult);

        final IUnitResult<Scope, IDatum, IDatum, Integer> result = future.asJavaCompletion().get();
        assertTrue(result.failures().isEmpty());
        assertEquals(1, (int) result.analysis());
    }

    @Test(timeout = 10000) public void testRestartChild_ParentUpdateSG()
            throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final IDatum lbl = new IDatum() {};
        final Scope d1 = new Scope("/./sub", 1);
        final Scope d2 = new Scope("/./sub", 2);

        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> childResult = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/./sub")
            .addRootScopes(root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), Env.empty()))
            .analysis(Unit.unit)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> parentResult = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/.")
            .scopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d1))
            .localScopeGraph(ScopeGraph.Immutable.of())
            .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.none(), LabelOrder.none(), DataLeq.any(), Env.empty()))
            .putSubUnitResults("sub", childResult)
            .analysis(Unit.unit)
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> future =
                this.run(".", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                    @Override public IFuture<Unit> run(
                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit, List<Scope> roots) {
                        final Scope s = unit.freshScope("s", Arrays.asList(), false, true);
                        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> subResult =
                                unit.add("sub", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                                    @Override public IFuture<Unit> run(
                                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit,
                                            List<Scope> rootScopes) {
                                        final Scope s1 = rootScopes.get(0);
                                        unit.initScope(s1, Arrays.asList(lbl), false);
                                        return unit.runIncremental(restarted -> {
                                            final Scope dNew = unit.freshScope("d", Arrays.asList(), true, false);
                                            unit.setDatum(dNew, d2);
                                            unit.addEdge(s1, lbl, dNew);
                                            unit.closeEdge(s1, lbl);
                                            return CompletableFuture.completedFuture(Unit.unit);
                                        });
                                    }
                                }, Arrays.asList(s), true);

                        unit.closeScope(s);

                        return unit.runIncremental(restarted -> CompletableFuture.completedFuture(Unit.unit))
                                .thenCompose(res -> subResult.thenApply(IUnitResult::analysis));
                    }

                }, Set.Immutable.of(lbl), false, parentResult);

        final IUnitResult<Scope, IDatum, IDatum, Unit> result = future.asJavaCompletion().get();
        final IUnitResult<Scope, IDatum, IDatum, ?> subResult = result.subUnitResults().get("sub");

        assertTrue(result.failures().isEmpty());
        assertEquals(TransitionTrace.RELEASED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.INITIALLY_STARTED, subResult.stateTransitionTrace());

        final Scope newRoot = subResult.rootScopes().get(0);

        // Verify scope graph is correct
        final List<Scope> allTargets = Lists.newArrayList(result.scopeGraph().getEdges(newRoot, lbl));

        assertEquals(1, allTargets.size());
        final Scope tgt = allTargets.get(0);

        assertEquals(d2, subResult.scopeGraph().getData(tgt).get());

        // Verify local part of scope graph is correct
        final List<Scope> parentTargets = Lists.newArrayList(result.localScopeGraph().getEdges(newRoot, lbl));
        assertEquals(0, parentTargets.size());

        final List<Scope> childTargets = Lists.newArrayList(subResult.localScopeGraph().getEdges(newRoot, lbl));
        assertEquals(Arrays.asList(tgt), childTargets);

        assertEquals(d2, subResult.localScopeGraph().getData(tgt).get());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Other
    ///////////////////////////////////////////////////////////////////////////

    @Test(timeout = 10000) public void testActivateOnDelayedUnitAddition()
            throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final IDatum lbl = new IDatum() {};
        final Scope d1 = new Scope("/./sub2", 1);

        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> sub1Result = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/.sub1")
            .addRootScopes(root)
            .scopeGraph(ScopeGraph.Immutable.of())
            .localScopeGraph(ScopeGraph.Immutable.of())
            .addQueries(RecordedQuery.of(d1, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.none(), Env.empty()))
            .analysis(Unit.unit)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> sub2Result = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/./sub2")
            .addRootScopes(root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .analysis(Unit.unit)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> parentResult = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/.")
            .scopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d1))
            .localScopeGraph(ScopeGraph.Immutable.of())
            .putSubUnitResults("sub1", sub1Result)
            .putSubUnitResults("sub2", sub2Result)
            .analysis(Unit.unit)
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> future =
                this.run(".", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                    @Override public IFuture<Unit> run(
                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit, List<Scope> roots) {
                        final Scope s = unit.freshScope("s", Arrays.asList(lbl), false, true);
                        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> sub1Future =
                                unit.add("sub1", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                                    @Override public IFuture<Unit> run(
                                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit,
                                            List<Scope> rootScopes) {
                                        final Scope s1 = rootScopes.get(0);
                                        unit.initScope(s1, Arrays.asList(), false);
                                        return unit.runIncremental(restarted -> {
                                            return CompletableFuture.completedFuture(Unit.unit);
                                        });
                                    }
                                }, Arrays.asList(s), false);

                        // Do some 'heavy work', ensure request for unit 2 ref from unit 1 is executed by now.
                        try {
                            Thread.sleep(100);
                        } catch(InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> sub2Future =
                                unit.add("sub2", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                                    @Override public IFuture<Unit> run(
                                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit,
                                            List<Scope> rootScopes) {
                                        final Scope s1 = rootScopes.get(0);
                                        unit.initScope(s1, Arrays.asList(lbl), false);
                                        return unit.runIncremental(restarted -> {
                                            return CompletableFuture.completedFuture(Unit.unit);
                                        });
                                    }
                                }, Arrays.asList(s), false);

                        unit.closeScope(s);

                        return unit.runIncremental(restarted -> CompletableFuture.completedFuture(Unit.unit))
                                .thenCompose(res -> sub2Future.thenCompose(s2res -> sub1Future)).thenApply(IUnitResult::analysis);
                    }

                }, Set.Immutable.of(lbl), false, parentResult);

        final IUnitResult<Scope, IDatum, IDatum, Unit> result = future.asJavaCompletion().get();

        assertEquals(TransitionTrace.RELEASED, result.stateTransitionTrace());
        assertEquals(TransitionTrace.RELEASED, result.subUnitResults().get("sub1").stateTransitionTrace());
        assertEquals(TransitionTrace.RELEASED, result.subUnitResults().get("sub2").stateTransitionTrace());

        assertTrue(result.failures().isEmpty());
    }

    @Test(timeout = 10000) public void testChildRemoved() throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final IDatum lbl = new IDatum() {};
        final Scope d1 = new Scope("/./sub2/sub", 1);

        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> sub1Result = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/.sub1")
            .addRootScopes(root)
            .scopeGraph(ScopeGraph.Immutable.of())
            .localScopeGraph(ScopeGraph.Immutable.of())
            .addQueries(RecordedQuery.of(d1, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.none(), Env.empty()))
            .analysis(Unit.unit)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> sub2subResult = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/./sub2/sub")
            .addRootScopes(root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .analysis(Unit.unit)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> sub2Result = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/./sub2")
            .addRootScopes(root)
            .scopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .localScopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d1).setDatum(d1, d1))
            .analysis(Unit.unit)
            .putSubUnitResults("sub", sub2subResult)
            .build();
        // @formatter:on

        // @formatter:off
        final IUnitResult<Scope, IDatum, IDatum, Unit> parentResult = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/.")
            .scopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d1))
            .localScopeGraph(ScopeGraph.Immutable.of())
            .putSubUnitResults("sub1", sub1Result)
            .putSubUnitResults("sub2", sub2Result)
            .analysis(Unit.unit)
            .build();
        // @formatter:on

        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> future =
                this.run(".", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                    @Override public IFuture<Unit> run(
                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit, List<Scope> roots) {
                        final Scope s = unit.freshScope("s", Arrays.asList(lbl), false, true);
                        final IFuture<IUnitResult<Scope, IDatum, IDatum, Unit>> sub1Future =
                                unit.add("sub1", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

                                    @Override public IFuture<Unit> run(
                                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit,
                                            List<Scope> rootScopes) {
                                        final Scope s1 = rootScopes.get(0);
                                        unit.initScope(s1, Arrays.asList(), false);
                                        return unit.runIncremental(restarted -> {
                                            return CompletableFuture.completedFuture(Unit.unit);
                                        });
                                    }
                                }, Arrays.asList(s), false);

                        unit.closeScope(s);

                        return unit.runIncremental(restarted -> CompletableFuture.completedFuture(Unit.unit))
                                .thenCompose(res -> sub1Future.thenApply(IUnitResult::analysis));
                    }

                }, Set.Immutable.of(lbl), false, parentResult);

        final IUnitResult<Scope, IDatum, IDatum, Unit> result = future.asJavaCompletion().get();
        assertTrue(result.allFailures().isEmpty());
    }

}
