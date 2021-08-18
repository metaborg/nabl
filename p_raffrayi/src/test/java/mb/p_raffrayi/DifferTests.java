package mb.p_raffrayi;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.unit.Unit;

import io.usethesource.capsule.Set;
import mb.p_raffrayi.impl.UnitResult;
import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.diff.BiMap;
import mb.scopegraph.oopsla20.diff.Edge;
import mb.scopegraph.oopsla20.diff.ScopeGraphDiff;
import mb.scopegraph.oopsla20.reference.ScopeGraph;

public class DifferTests extends PRaffrayiTestBase {

    private static final Integer LBL_1 = 1;
    private static final Integer LBL_2 = 2;
    private static final Integer LBL_3 = 3;

    private static final Scope root = new Scope("/\\/.", 17);

    private static final Set.Immutable<Integer> labels = CapsuleUtil.toSet(LBL_1, LBL_2, LBL_3);

    private static final BiMap.Immutable<?> EMPTY_BIMAP = BiMap.Immutable.of();

    ///////////////////////////////////////////////////////////////////////////
    // Local Differ tests
    ///////////////////////////////////////////////////////////////////////////

    @Test(timeout = 10000) public void testEmptyDiff() throws InterruptedException, ExecutionException {
        final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Integer, Scope>, Unit>> future =
                runSingle(new ITypeChecker<Scope, Integer, IDatum, Result<Integer, Scope>, Unit>() {

                    @Override public IFuture<Result<Integer, Scope>> run(
                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, Result<Integer, Scope>, Unit> unit,
                            List<Scope> rootScopes) {
                        Scope root = rootScopes.get(0);
                        unit.initScope(root, Arrays.asList(), false);
                        return unit.runIncremental(restarted -> {
                            return CompletableFuture.completedFuture(Result.of(root));
                        });
                    }
                }, root, ScopeGraph.Immutable.of());

        IUnitResult<Scope, Integer, IDatum, Result<Integer, Scope>, Unit> result = future.asJavaCompletion().get();

        assertEquals(Arrays.asList(), result.allFailures());
        final ScopeGraphDiff<Scope, Integer, IDatum> diff = result.diff();

        assertEquals(CapsuleUtil.immutableMap(), diff.added().scopes());
        assertEquals(CapsuleUtil.immutableSet(), diff.added().edges());

        assertEquals(CapsuleUtil.immutableMap(), diff.removed().scopes());
        assertEquals(CapsuleUtil.immutableSet(), diff.removed().edges());

        assertEquals(BiMap.Immutable.of(result.analysis().value(), root), diff.matchedScopes());
        assertEquals(EMPTY_BIMAP, diff.matchedEdges());
    }

    @Test(timeout = 10000) public void testAddedEdgeDiff() throws InterruptedException, ExecutionException {
        final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Integer, List<Scope>>, Unit>> future =
                runSingle(new ITypeChecker<Scope, Integer, IDatum, Result<Integer, List<Scope>>, Unit>() {

                    @Override public IFuture<Result<Integer, List<Scope>>> run(
                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, Result<Integer, List<Scope>>, Unit> unit,
                            List<Scope> rootScopes) {
                        Scope root = rootScopes.get(0);
                        unit.initScope(root, Arrays.asList(LBL_1), false);
                        return unit.runIncremental(restarted -> {
                            final Scope d = unit.freshScope("d", Arrays.asList(), false, false);
                            unit.addEdge(root, LBL_1, d);
                            unit.closeEdge(root, LBL_1);
                            return CompletableFuture.completedFuture(Result.of(Arrays.asList(root, d)));
                        });
                    }
                }, root, ScopeGraph.Immutable.of());

        IUnitResult<Scope, Integer, IDatum, Result<Integer, List<Scope>>, Unit> result = future.asJavaCompletion().get();
        List<Scope> resultScopes = result.analysis().value();

        assertEquals(Arrays.asList(), result.allFailures());
        final ScopeGraphDiff<Scope, Integer, IDatum> diff = result.diff();

        assertEquals(CapsuleUtil.immutableMap().__put(resultScopes.get(1), resultScopes.get(1)), diff.added().scopes());
        assertEquals(CapsuleUtil.immutableSet(new Edge<>(resultScopes.get(0), LBL_1, resultScopes.get(1))),
                diff.added().edges());

        assertEquals(CapsuleUtil.immutableMap(), diff.removed().scopes());
        assertEquals(CapsuleUtil.immutableSet(), diff.removed().edges());

        assertEquals(BiMap.Immutable.of(resultScopes.get(0), root), diff.matchedScopes());
        assertEquals(EMPTY_BIMAP, diff.matchedEdges());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Utilities
    ///////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("unchecked") public <R extends IResult<Scope, Integer, IDatum>> IFuture<IUnitResult<Scope, Integer, IDatum, R, Unit>> runSingle(
            ITypeChecker<Scope, Integer, IDatum, R, Unit> typeChecker, Scope prevRoot,
            IScopeGraph.Immutable<Scope, Integer, IDatum> previousGraph) {
        // @formatter:off
        IUnitResult<Scope, Integer, IDatum, R, Unit> childResult = UnitResult.<Scope, Integer, IDatum, R, Unit>builder()
            .id("/./sub")
            .scopeGraph(previousGraph)
            .localScopeGraph(previousGraph)
            .addRootScopes(prevRoot)
            .build();
        // @formatter:on

        // @formatter:off
        IUnitResult<Scope, Integer, IDatum, IResult.Empty<Scope, Integer, IDatum>, Unit> parentResult = UnitResult.<Scope, Integer, IDatum, IResult.Empty<Scope, Integer, IDatum>, Unit>builder()
            .id("/.")
            .scopeGraph(previousGraph) // TODO: remove data?
            .localScopeGraph(ScopeGraph.Immutable.of())
            .putSubUnitResults("sub", childResult)
            .build();
        // @formatter:on

        return run("/.", new ITypeChecker<Scope, Integer, IDatum, IResult.Empty<Scope, Integer, IDatum>, Unit>() {

            @Override public IFuture<IResult.Empty<Scope, Integer, IDatum>> run(IIncrementalTypeCheckerContext<Scope, Integer, IDatum, IResult.Empty<Scope, Integer, IDatum>, Unit> unit,
                    List<Scope> rootScopes) {
                final Scope root = unit.freshScope("s", CapsuleUtil.immutableSet(), false, true);
                final IFuture<IUnitResult<Scope, Integer, IDatum, R, Unit>> subResult =
                        unit.add("sub", typeChecker, Arrays.asList(root));
                unit.closeScope(root);
                return unit.runIncremental(restarted -> {
                    return subResult.thenApply(__ -> IResult.Empty.of());
                });
            }
        }, labels, true, parentResult).thenApply(result -> {
            return (IUnitResult<Scope, Integer, IDatum, R, Unit>) result.subUnitResults().get("sub");
        });

    }

}
