package mb.p_raffrayi;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.unit.Unit;

import io.usethesource.capsule.Set;
import mb.p_raffrayi.impl.AInitialState;
import mb.p_raffrayi.impl.UnitResult;
import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.diff.BiMap;
import mb.scopegraph.oopsla20.diff.Edge;
import mb.scopegraph.oopsla20.diff.ScopeGraphDiff;
import mb.scopegraph.oopsla20.reference.ScopeGraph;

public class DifferTests extends PRaffrayiTestBase {

    private static final IDatum LBL_1 = new IDatum() {};
    private static final IDatum LBL_2 = new IDatum() {};
    private static final IDatum LBL_3 = new IDatum() {};

    private static final Scope root = new Scope("/\\/.", 17);

    private static final Set.Immutable<IDatum> labels = CapsuleUtil.toSet(LBL_1, LBL_2, LBL_3);

    private static final BiMap.Immutable<?> EMPTY_BIMAP = BiMap.Immutable.of();

    ///////////////////////////////////////////////////////////////////////////
    // Local Differ tests
    ///////////////////////////////////////////////////////////////////////////

    @Test public void testEmptyDiff() throws InterruptedException, ExecutionException {
        final IFuture<IUnitResult<Scope, IDatum, IDatum, Scope>> future = runSingle(new ITypeChecker<Scope, IDatum, IDatum, Scope>() {

            @Override public IFuture<Scope> run(IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Scope> unit,
                    List<Scope> rootScopes) {
                Scope root = rootScopes.get(0);
                unit.initScope(root, Arrays.asList(), false);
                return unit.runIncremental(restarted -> {
                    return CompletableFuture.completedFuture(root);
                });
            }}, root, ScopeGraph.Immutable.of());

        IUnitResult<Scope, IDatum, IDatum, Scope> result = future.asJavaCompletion().get();

        assertEquals(Arrays.asList(), result.allFailures());
        final ScopeGraphDiff<Scope, IDatum, IDatum> diff = result.diff();

        assertEquals(CapsuleUtil.immutableMap(), diff.added().scopes());
        assertEquals(CapsuleUtil.immutableSet(), diff.added().edges());

        assertEquals(CapsuleUtil.immutableMap(), diff.removed().scopes());
        assertEquals(CapsuleUtil.immutableSet(), diff.removed().edges());

        assertEquals(BiMap.Immutable.of(result.analysis(), root), diff.matchedScopes());
        assertEquals(EMPTY_BIMAP, diff.matchedEdges());
    }

    @Test public void testAddedEdgeDiff() throws InterruptedException, ExecutionException {
        final IFuture<IUnitResult<Scope, IDatum, IDatum, List<Scope>>> future = runSingle(new ITypeChecker<Scope, IDatum, IDatum, List<Scope>>() {

            @Override public IFuture<List<Scope>> run(IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, List<Scope>> unit,
                    List<Scope> rootScopes) {
                Scope root = rootScopes.get(0);
                unit.initScope(root, Arrays.asList(LBL_1), false);
                return unit.runIncremental(restarted -> {
                    final Scope d = unit.freshScope("d", Arrays.asList(), false, false);
                    unit.addEdge(root, LBL_1, d);
                    unit.closeEdge(root, LBL_1);
                    return CompletableFuture.completedFuture(Arrays.asList(root, d));
                });
            }}, root, ScopeGraph.Immutable.of());

        IUnitResult<Scope, IDatum, IDatum, List<Scope>> result = future.asJavaCompletion().get();
        List<Scope> resultScopes = result.analysis();

        assertEquals(Arrays.asList(), result.allFailures());
        final ScopeGraphDiff<Scope, IDatum, IDatum> diff = result.diff();

        assertEquals(CapsuleUtil.immutableMap().__put(resultScopes.get(1), Optional.empty()), diff.added().scopes());
        assertEquals(CapsuleUtil.immutableSet(new Edge<>(resultScopes.get(0), LBL_1, resultScopes.get(1))), diff.added().edges());

        assertEquals(CapsuleUtil.immutableMap(), diff.removed().scopes());
        assertEquals(CapsuleUtil.immutableSet(), diff.removed().edges());

        assertEquals(BiMap.Immutable.of(resultScopes.get(0), root), diff.matchedScopes());
        assertEquals(EMPTY_BIMAP, diff.matchedEdges());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Utilities
    ///////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("unchecked")
    public <R> IFuture<IUnitResult<Scope, IDatum, IDatum, R>> runSingle(ITypeChecker<Scope, IDatum, IDatum, R> typeChecker,
            Scope prevRoot, IScopeGraph.Immutable<Scope, IDatum, IDatum> previousGraph) {
        // @formatter:off
        IUnitResult<Scope, IDatum, IDatum, R> childResult = UnitResult.<Scope, IDatum, IDatum, R>builder()
            .id("/./sub")
            .scopeGraph(previousGraph)
            .localScopeGraph(previousGraph)
            .addRootScopes(prevRoot)
            .build();
        // @formatter:on

        // @formatter:off
        IUnitResult<Scope, IDatum, IDatum, Unit> parentResult = UnitResult.<Scope, IDatum, IDatum, Unit>builder()
            .id("/.")
            .scopeGraph(previousGraph) // TODO: remove data?
            .localScopeGraph(ScopeGraph.Immutable.of())
            .putSubUnitResults("sub", childResult)
            .build();
        // @formatter:on

        return run("/.", new ITypeChecker<Scope, IDatum, IDatum, Unit>() {

            @Override public IFuture<Unit> run(IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Unit> unit,
                    List<Scope> rootScopes) {
                final Scope root = unit.freshScope("s", CapsuleUtil.immutableSet(), false, true);
                final IFuture<IUnitResult<Scope, IDatum, IDatum, R>> subResult = unit.add("sub", typeChecker, Arrays.asList(root));
                unit.closeScope(root);
                return unit.runIncremental(restarted -> {
                    return subResult.thenApply(__ -> Unit.unit);
                });
            }}, labels, AInitialState.changed(parentResult)).thenApply(result -> {
                return (IUnitResult<Scope, IDatum, IDatum, R>) result.subUnitResults().get("sub");
            });

    }

}
