package mb.p_raffrayi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;

import io.usethesource.capsule.Set;
import mb.p_raffrayi.impl.AInitialState;
import mb.p_raffrayi.impl.IInitialState;
import mb.p_raffrayi.impl.RecordedQuery;
import mb.p_raffrayi.impl.UnitResult;
import mb.p_raffrayi.nameresolution.DataLeq;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelOrder;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.oopsla20.reference.Env;
import mb.scopegraph.oopsla20.reference.ScopeGraph;
import mb.scopegraph.oopsla20.terms.newPath.ResolutionPath;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

public class IncrementalTest extends PRaffrayiTestBase {

    ///////////////////////////////////////////////////////////////////////////
    // Release
    ///////////////////////////////////////////////////////////////////////////

    @Test(timeout = 10000) public void testSimpleRelease() throws InterruptedException, ExecutionException {
        final IUnitResult<Scope, IDatum, IDatum, Boolean> previousResult = UnitResult.<Scope, IDatum, IDatum, Boolean>builder()
                .id("/.")
                .scopeGraph(ScopeGraph.Immutable.of())
                .analysis(false)
                .build();

        final IFuture<IUnitResult<Scope, IDatum, IDatum, Boolean>> future =
                this.run(".", new ITypeChecker<Scope, IDatum, IDatum, Boolean>() {

                    @Override public IFuture<Boolean> run(
                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Boolean> unit, List<Scope> roots,
                            IInitialState<Scope, IDatum, IDatum, Boolean> initialState) {
                        return unit.runIncremental(restarted -> CompletableFuture.completedFuture(true));
                    }

                }, Set.Immutable.of(), AInitialState.cached(previousResult));

        final IUnitResult<Scope, IDatum, IDatum, Boolean> result = future.asJavaCompletion().get();

        assertFalse(result.analysis());
        assertTrue(result.failures().isEmpty());
    }

    @Test(timeout = 10000) public void testReleaseChild_ParentCached() throws InterruptedException, ExecutionException {
        final IUnitResult<Scope, IDatum, IDatum, Boolean> parentResult = UnitResult.<Scope, IDatum, IDatum, Boolean>builder()
                .id("/.")
                .scopeGraph(ScopeGraph.Immutable.of())
                .analysis(false)
                .build();

        final Scope root = new Scope("/.", 0);

        final IUnitResult<Scope, IDatum, IDatum, Boolean> childResult = UnitResult.<Scope, IDatum, IDatum, Boolean>builder()
                .id("/./sub")
                .addRootScopes(root)
                .scopeGraph(ScopeGraph.Immutable.of())
                .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), Env.empty()))
                .analysis(false)
                .build();

        final IFuture<IUnitResult<Scope, IDatum, IDatum, Boolean>> future =
                this.run(".", new ITypeChecker<Scope, IDatum, IDatum, Boolean>() {

                    @Override public IFuture<Boolean> run(
                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Boolean> unit, List<Scope> roots,
                            IInitialState<Scope, IDatum, IDatum, Boolean> initialState) {
                        final Scope s = unit.freshScope("s", Arrays.asList(), false, true);
                        final IFuture<IUnitResult<Scope, IDatum, IDatum, Boolean>> subResult = unit.add("sub", new ITypeChecker<Scope, IDatum, IDatum, Boolean>() {

                            @Override public IFuture<Boolean> run(
                                    IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Boolean> unit,
                                    List<Scope> rootScopes, IInitialState<Scope, IDatum, IDatum, Boolean> initialState) {
                                final Scope s1 = rootScopes.get(0);
                                unit.initScope(s1, Arrays.asList(), false);
                                return unit.runIncremental(restarted -> {
                                    return unit.query(s1, LabelWf.any(), LabelOrder.none(), DataWf.any(), DataLeq.any()).thenApply(__ -> true);
                                });
                            }}, Arrays.asList(s), AInitialState.cached(childResult));

                        unit.closeScope(s);

                        return unit.runIncremental(restarted -> CompletableFuture.completedFuture(true))
                                .thenCompose(res -> subResult.thenApply(sRes -> res || sRes.analysis()));
                    }

                }, Set.Immutable.of(), AInitialState.cached(parentResult));

        final IUnitResult<Scope, IDatum, IDatum, Boolean> result = future.asJavaCompletion().get();
        assertFalse(result.analysis());
        assertTrue(result.failures().isEmpty());
    }

    @Test(timeout = 10000) public void testReleaseChild_ParentChanged() throws InterruptedException, ExecutionException {
        final IUnitResult<Scope, IDatum, IDatum, Boolean> parentResult = UnitResult.<Scope, IDatum, IDatum, Boolean>builder()
                .id("/.")
                .scopeGraph(ScopeGraph.Immutable.of())
                .analysis(false)
                .build();

        final Scope root = new Scope("/.", 0);

        final IUnitResult<Scope, IDatum, IDatum, Boolean> childResult = UnitResult.<Scope, IDatum, IDatum, Boolean>builder()
                .id("/./sub")
                .addRootScopes(root)
                .scopeGraph(ScopeGraph.Immutable.of())
                .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), Env.empty()))
                .analysis(false)
                .build();

        final IFuture<IUnitResult<Scope, IDatum, IDatum, Boolean>> future =
                this.run(".", new ITypeChecker<Scope, IDatum, IDatum, Boolean>() {

                    @Override public IFuture<Boolean> run(
                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Boolean> unit, List<Scope> roots,
                            IInitialState<Scope, IDatum, IDatum, Boolean> initialState) {
                        final Scope s = unit.freshScope("s", Arrays.asList(), false, true);
                        final IFuture<IUnitResult<Scope, IDatum, IDatum, Boolean>> subResult = unit.add("sub", new ITypeChecker<Scope, IDatum, IDatum, Boolean>() {

                            @Override public IFuture<Boolean> run(
                                    IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Boolean> unit,
                                    List<Scope> rootScopes, IInitialState<Scope, IDatum, IDatum, Boolean> initialState) {
                                final Scope s1 = rootScopes.get(0);
                                unit.initScope(s1, Arrays.asList(), false);
                                return unit.runIncremental(restarted -> {
                                    return unit.query(s1, LabelWf.any(), LabelOrder.none(), DataWf.any(), DataLeq.any()).thenApply(__ -> true);
                                });
                            }}, Arrays.asList(s), AInitialState.cached(childResult));

                        unit.closeScope(s);

                        return unit.runIncremental(restarted -> CompletableFuture.completedFuture(true))
                                .thenCompose(res -> subResult.thenApply(sRes -> {
                                    return (!res || sRes.analysis()) && sRes.failures().isEmpty();
                                }));
                    }

                }, Set.Immutable.of(), AInitialState.changed(parentResult));

        final IUnitResult<Scope, IDatum, IDatum, Boolean> result = future.asJavaCompletion().get();
        assertFalse(result.analysis());
        assertTrue(result.failures().isEmpty());
    }

    @Test(timeout = 10000) public void testRelease_MutualDep_Cached() throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final IDatum lbl = new IDatum() {};
        final Scope d = new Scope("/./sub", 1);

        final ResolutionPath<Scope, IDatum, IDatum> path = new ScopePath<Scope, IDatum>(root).step(lbl, d).get().resolve(d);
        final Env<Scope, IDatum, IDatum> env = Env.of(path);

        final IUnitResult<Scope, IDatum, IDatum, Boolean> parentResult = UnitResult.<Scope, IDatum, IDatum, Boolean>builder()
                .id("/.")
                .scopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d))
                .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), env))
                .analysis(false)
                .build();

        final IUnitResult<Scope, IDatum, IDatum, Boolean> childResult = UnitResult.<Scope, IDatum, IDatum, Boolean>builder()
                .id("/./sub")
                .addRootScopes(root)
                .scopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
                .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), env))
                .analysis(false)
                .build();

        final IFuture<IUnitResult<Scope, IDatum, IDatum, Boolean>> future =
                this.run(".", new ITypeChecker<Scope, IDatum, IDatum, Boolean>() {

                    @Override public IFuture<Boolean> run(
                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Boolean> unit, List<Scope> roots,
                            IInitialState<Scope, IDatum, IDatum, Boolean> initialState) {
                        final Scope s = unit.freshScope("s", Arrays.asList(), false, true);
                        final IFuture<IUnitResult<Scope, IDatum, IDatum, Boolean>> subResult = unit.add("sub", new ITypeChecker<Scope, IDatum, IDatum, Boolean>() {

                            @Override public IFuture<Boolean> run(
                                    IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Boolean> unit,
                                    List<Scope> rootScopes, IInitialState<Scope, IDatum, IDatum, Boolean> initialState) {
                                final Scope s1 = rootScopes.get(0);
                                unit.initScope(s1, Arrays.asList(lbl), false);
                                return unit.runIncremental(restarted -> {
                                    final Scope d = unit.freshScope("d", Arrays.asList(), true, false);
                                    unit.setDatum(d, d);
                                    unit.addEdge(s1, lbl, d);
                                    unit.closeEdge(s1, lbl);

                                    return unit.query(s1, LabelWf.any(), LabelOrder.none(), DataWf.any(), DataLeq.any()).thenApply(__ -> true);
                                });
                            }}, Arrays.asList(s), AInitialState.cached(childResult));

                        unit.closeScope(s);

                        return unit.runIncremental(restarted -> CompletableFuture.completedFuture(true))
                                .thenCompose(res -> subResult.thenApply(sRes -> {
                                    return !res && !sRes.analysis() && sRes.failures().isEmpty();
                                }));
                    }

                }, Set.Immutable.of(lbl), AInitialState.cached(parentResult));

        final IUnitResult<Scope, IDatum, IDatum, Boolean> result = future.asJavaCompletion().get();
        assertTrue(result.analysis());
        assertTrue(result.failures().isEmpty());
    }

    @Test(timeout = 10000) public void testRelease_MutualDep_ParentChanged() throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final IDatum lbl = new IDatum() {};
        final Scope d = new Scope("/./sub", 1);

        final ResolutionPath<Scope, IDatum, IDatum> path = new ScopePath<Scope, IDatum>(root).step(lbl, d).get().resolve(d);
        final Env<Scope, IDatum, IDatum> env = Env.of(path);

        final IUnitResult<Scope, IDatum, IDatum, Boolean> parentResult = UnitResult.<Scope, IDatum, IDatum, Boolean>builder()
                .id("/.")
                .scopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d))
                .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), env))
                .analysis(false)
                .build();

        final IUnitResult<Scope, IDatum, IDatum, Boolean> childResult = UnitResult.<Scope, IDatum, IDatum, Boolean>builder()
                .id("/./sub")
                .addRootScopes(root)
                .scopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
                .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), env))
                .analysis(false)
                .build();

        final IFuture<IUnitResult<Scope, IDatum, IDatum, Boolean>> future =
                this.run(".", new ITypeChecker<Scope, IDatum, IDatum, Boolean>() {

                    @Override public IFuture<Boolean> run(
                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Boolean> unit, List<Scope> roots,
                            IInitialState<Scope, IDatum, IDatum, Boolean> initialState) {
                        final Scope s = unit.freshScope("s", Arrays.asList(), false, true);
                        final IFuture<IUnitResult<Scope, IDatum, IDatum, Boolean>> subResult = unit.add("sub", new ITypeChecker<Scope, IDatum, IDatum, Boolean>() {

                            @Override public IFuture<Boolean> run(
                                    IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Boolean> unit,
                                    List<Scope> rootScopes, IInitialState<Scope, IDatum, IDatum, Boolean> initialState) {
                                final Scope s1 = rootScopes.get(0);
                                unit.initScope(s1, Arrays.asList(lbl), false);
                                return unit.runIncremental(restarted -> {
                                    final Scope d = unit.freshScope("d", Arrays.asList(), true, false);
                                    unit.setDatum(d, d);
                                    unit.addEdge(s1, lbl, d);
                                    unit.closeEdge(s1, lbl);

                                    return unit.query(s1, LabelWf.any(), LabelOrder.none(), DataWf.any(), DataLeq.any()).thenApply(__ -> true);
                                });
                            }}, Arrays.asList(s), AInitialState.cached(childResult));

                        unit.closeScope(s);

                        return unit.runIncremental(restarted -> CompletableFuture.completedFuture(true))
                                .thenCompose(res -> subResult.thenApply(sRes -> {
                                    return res && sRes.analysis() && sRes.failures().isEmpty();
                                }));
                    }

                }, Set.Immutable.of(lbl), AInitialState.changed(parentResult));

        final IUnitResult<Scope, IDatum, IDatum, Boolean> result = future.asJavaCompletion().get();
        assertTrue(result.analysis());
        assertTrue(result.failures().isEmpty());
    }

    @Test(timeout = 10000) public void testRelease_MutualDep_ChildChanged() throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final IDatum lbl = new IDatum() {};
        final Scope d = new Scope("/./sub", 1);

        final ResolutionPath<Scope, IDatum, IDatum> path = new ScopePath<Scope, IDatum>(root).step(lbl, d).get().resolve(d);
        final Env<Scope, IDatum, IDatum> env = Env.of(path);

        final IUnitResult<Scope, IDatum, IDatum, Boolean> parentResult = UnitResult.<Scope, IDatum, IDatum, Boolean>builder()
                .id("/.")
                .scopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d))
                .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), env))
                .analysis(false)
                .build();

        final IUnitResult<Scope, IDatum, IDatum, Boolean> childResult = UnitResult.<Scope, IDatum, IDatum, Boolean>builder()
                .id("/./sub")
                .addRootScopes(root)
                .scopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
                .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), env))
                .analysis(false)
                .build();

        final IFuture<IUnitResult<Scope, IDatum, IDatum, Boolean>> future =
                this.run(".", new ITypeChecker<Scope, IDatum, IDatum, Boolean>() {

                    @Override public IFuture<Boolean> run(
                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Boolean> unit, List<Scope> roots,
                            IInitialState<Scope, IDatum, IDatum, Boolean> initialState) {
                        final Scope s = unit.freshScope("s", Arrays.asList(), false, true);
                        final IFuture<IUnitResult<Scope, IDatum, IDatum, Boolean>> subResult = unit.add("sub", new ITypeChecker<Scope, IDatum, IDatum, Boolean>() {

                            @Override public IFuture<Boolean> run(
                                    IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Boolean> unit,
                                    List<Scope> rootScopes, IInitialState<Scope, IDatum, IDatum, Boolean> initialState) {
                                final Scope s1 = rootScopes.get(0);
                                unit.initScope(s1, Arrays.asList(lbl), false);
                                return unit.runIncremental(restarted -> {
                                    final Scope d = unit.freshScope("d", Arrays.asList(), true, false);
                                    unit.setDatum(d, d);
                                    unit.addEdge(s1, lbl, d);
                                    unit.closeEdge(s1, lbl);

                                    return unit.query(s1, LabelWf.any(), LabelOrder.none(), DataWf.any(), DataLeq.any()).thenApply(__ -> true);
                                });
                            }}, Arrays.asList(s), AInitialState.changed(childResult));

                        unit.closeScope(s);

                        return unit.runIncremental(restarted -> CompletableFuture.completedFuture(true))
                                .thenCompose(res -> subResult.thenApply(sRes -> {
                                    return res && sRes.analysis() && sRes.failures().isEmpty();
                                }));
                    }

                }, Set.Immutable.of(lbl), AInitialState.cached(parentResult));

        final IUnitResult<Scope, IDatum, IDatum, Boolean> result = future.asJavaCompletion().get();
        assertTrue(result.analysis());
        assertTrue(result.failures().isEmpty());
    }

    @Test(timeout = 10000) public void testQueryInReleasedUnit() throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final IDatum lbl = new IDatum() {};
        final Scope d = new Scope("/./sub", 1);

        final IUnitResult<Scope, IDatum, IDatum, Boolean> parentResult = UnitResult.<Scope, IDatum, IDatum, Boolean>builder()
                .id("/.")
                .scopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
                .analysis(false)
                .build();

        final IFuture<IUnitResult<Scope, IDatum, IDatum, Boolean>> future =
                this.run(".", new ITypeChecker<Scope, IDatum, IDatum, Boolean>() {

                    @Override public IFuture<Boolean> run(
                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Boolean> unit, List<Scope> roots,
                            IInitialState<Scope, IDatum, IDatum, Boolean> initialState) {
                        final Scope s = unit.freshScope("s", Arrays.asList(), false, true);
                        final IFuture<IUnitResult<Scope, IDatum, IDatum, Boolean>> subResult = unit.add("sub", new ITypeChecker<Scope, IDatum, IDatum, Boolean>() {

                            @Override public IFuture<Boolean> run(
                                    IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Boolean> unit,
                                    List<Scope> rootScopes, IInitialState<Scope, IDatum, IDatum, Boolean> initialState) {
                                final Scope s1 = rootScopes.get(0);
                                unit.initScope(s1, Arrays.asList(), false);
                                return unit.runIncremental(restarted -> {
                                    return unit.query(s1, LabelWf.any(), LabelOrder.none(), DataWf.any(), DataLeq.any()).thenApply(__ -> true);
                                });
                            }}, Arrays.asList(s), AInitialState.added());

                        unit.closeScope(s);

                        return unit.runIncremental(restarted -> CompletableFuture.completedFuture(true))
                                .thenCompose(res -> subResult.thenApply(sRes -> !res && sRes.analysis() && sRes.failures().isEmpty()));
                    }

                }, Set.Immutable.of(lbl), AInitialState.cached(parentResult));

        final IUnitResult<Scope, IDatum, IDatum, Boolean> result = future.asJavaCompletion().get();
        assertTrue(result.analysis());
        assertTrue(result.failures().isEmpty());
    }

    @Test(timeout = 10000) public void testReleasedUnit_ContainsRootScopes() throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 123);
        final IDatum lbl = new IDatum() {};
        final Scope d = new Scope("/./sub", 1);

        final ResolutionPath<Scope, IDatum, IDatum> path = new ScopePath<Scope, IDatum>(root).step(lbl, d).get().resolve(d);
        final Env<Scope, IDatum, IDatum> env = Env.of(path);
        final IRecordedQuery<Scope, IDatum, IDatum> query = RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), env);

        final IUnitResult<Scope, IDatum, IDatum, Boolean> parentResult = UnitResult.<Scope, IDatum, IDatum, Boolean>builder()
                .id("/.")
                .scopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d))
                .addQueries(query)
                .analysis(false)
                .build();

        final IUnitResult<Scope, IDatum, IDatum, Boolean> childResult = UnitResult.<Scope, IDatum, IDatum, Boolean>builder()
                .id("/./sub")
                .addRootScopes(root)
                .scopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
                .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), env))
                .analysis(false)
                .build();

        final IFuture<IUnitResult<Scope, IDatum, IDatum, Boolean>> future =
                this.run(".", new ITypeChecker<Scope, IDatum, IDatum, Boolean>() {

                    @Override public IFuture<Boolean> run(
                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Boolean> unit, List<Scope> roots,
                            IInitialState<Scope, IDatum, IDatum, Boolean> initialState) {
                        final Scope s = unit.freshScope("s", Arrays.asList(), false, true);
                        final IFuture<IUnitResult<Scope, IDatum, IDatum, Boolean>> subResult = unit.add("sub", new ITypeChecker<Scope, IDatum, IDatum, Boolean>() {

                            @Override public IFuture<Boolean> run(
                                    IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Boolean> unit,
                                    List<Scope> rootScopes, IInitialState<Scope, IDatum, IDatum, Boolean> initialState) {
                                final Scope s1 = rootScopes.get(0);
                                unit.initScope(s1, Arrays.asList(lbl), false);
                                return unit.runIncremental(restarted -> {
                                    final Scope d = unit.freshScope("d", Arrays.asList(), true, false);
                                    unit.setDatum(d, d);
                                    unit.addEdge(s1, lbl, d);
                                    unit.closeEdge(s1, lbl);

                                    return unit.query(s1, LabelWf.any(), LabelOrder.none(), DataWf.any(), DataLeq.any()).thenApply(__ -> true);
                                });
                            }}, Arrays.asList(s), AInitialState.cached(childResult));

                        unit.closeScope(s);

                        return unit.runIncremental(restarted -> CompletableFuture.completedFuture(true))
                                .thenCompose(res -> subResult.thenApply(sRes -> {
                                    return !res && !sRes.analysis() && sRes.failures().isEmpty();
                                }));
                    }

                }, Set.Immutable.of(lbl), AInitialState.cached(parentResult));

        final IUnitResult<Scope, IDatum, IDatum, Boolean> result = future.asJavaCompletion().get();
        final IUnitResult<Scope, IDatum, IDatum, ?> subResult = result.subUnitResults().get("sub");
        assertFalse((Boolean) subResult.analysis());
        assertEquals(1, subResult.rootScopes().size());
        // Root scopes does not necessarily include `root`, but rather its match.
    }

    @Test(timeout = 10000) public void testReleasedUnit_ContainsQueries() throws InterruptedException, ExecutionException {
        final Scope root = new Scope("/.", 0);
        final IDatum lbl = new IDatum() {};
        final Scope d = new Scope("/./sub", 1);

        final ResolutionPath<Scope, IDatum, IDatum> path = new ScopePath<Scope, IDatum>(root).step(lbl, d).get().resolve(d);
        final Env<Scope, IDatum, IDatum> env = Env.of(path);
        final IRecordedQuery<Scope, IDatum, IDatum> query = RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), env);

        final IUnitResult<Scope, IDatum, IDatum, Boolean> parentResult = UnitResult.<Scope, IDatum, IDatum, Boolean>builder()
                .id("/.")
                .scopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d))
                .addQueries(query)
                .analysis(false)
                .build();

        final IUnitResult<Scope, IDatum, IDatum, Boolean> childResult = UnitResult.<Scope, IDatum, IDatum, Boolean>builder()
                .id("/./sub")
                .addRootScopes(root)
                .scopeGraph(ScopeGraph.Immutable.<Scope, IDatum, IDatum>of().addEdge(root, lbl, d).setDatum(d, d))
                .addQueries(RecordedQuery.of(root, LabelWf.any(), DataWf.any(), LabelOrder.none(), DataLeq.any(), env))
                .analysis(false)
                .build();

        final IFuture<IUnitResult<Scope, IDatum, IDatum, Boolean>> future =
                this.run(".", new ITypeChecker<Scope, IDatum, IDatum, Boolean>() {

                    @Override public IFuture<Boolean> run(
                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Boolean> unit, List<Scope> roots,
                            IInitialState<Scope, IDatum, IDatum, Boolean> initialState) {
                        final Scope s = unit.freshScope("s", Arrays.asList(), false, true);
                        final IFuture<IUnitResult<Scope, IDatum, IDatum, Boolean>> subResult = unit.add("sub", new ITypeChecker<Scope, IDatum, IDatum, Boolean>() {

                            @Override public IFuture<Boolean> run(
                                    IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Boolean> unit,
                                    List<Scope> rootScopes, IInitialState<Scope, IDatum, IDatum, Boolean> initialState) {
                                final Scope s1 = rootScopes.get(0);
                                unit.initScope(s1, Arrays.asList(lbl), false);
                                return unit.runIncremental(restarted -> {
                                    final Scope d = unit.freshScope("d", Arrays.asList(), true, false);
                                    unit.setDatum(d, d);
                                    unit.addEdge(s1, lbl, d);
                                    unit.closeEdge(s1, lbl);

                                    return unit.query(s1, LabelWf.any(), LabelOrder.none(), DataWf.any(), DataLeq.any()).thenApply(__ -> true);
                                });
                            }}, Arrays.asList(s), AInitialState.cached(childResult));

                        unit.closeScope(s);

                        return unit.runIncremental(restarted -> CompletableFuture.completedFuture(true))
                                .thenCompose(res -> subResult.thenApply(sRes -> {
                                    return !res && !sRes.analysis() && sRes.failures().isEmpty();
                                }));
                    }

                }, Set.Immutable.of(lbl), AInitialState.cached(parentResult));

        final IUnitResult<Scope, IDatum, IDatum, Boolean> result = future.asJavaCompletion().get();
        assertTrue("Incorrect analysis", result.analysis());
        assertEquals("Invalid query count.", 1, result.queries().size());
        assertTrue("Query not recorded", result.queries().contains(query));
    }

    ///////////////////////////////////////////////////////////////////////////
    // Restart
    ///////////////////////////////////////////////////////////////////////////

    @Test(timeout = 10000) public void testSimpleRestart() throws InterruptedException, ExecutionException {
        final IUnitResult<Scope, IDatum, IDatum, Boolean> previousResult = UnitResult.<Scope, IDatum, IDatum, Boolean>builder()
                .id("/.")
                .scopeGraph(ScopeGraph.Immutable.of())
                .analysis(false)
                .build();

        final IFuture<IUnitResult<Scope, IDatum, IDatum, Boolean>> future =
                this.run(".", new ITypeChecker<Scope, IDatum, IDatum, Boolean>() {

                    @Override public IFuture<Boolean> run(
                            IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Boolean> unit, List<Scope> roots,
                            IInitialState<Scope, IDatum, IDatum, Boolean> initialState) {
                        return unit.runIncremental(restarted -> CompletableFuture.completedFuture(true));
                    }

                }, Set.Immutable.of(), AInitialState.changed(previousResult));

        final IUnitResult<Scope, IDatum, IDatum, Boolean> result = future.asJavaCompletion().get();

        assertTrue(result.analysis());
        assertTrue(result.failures().isEmpty());
    }

}
