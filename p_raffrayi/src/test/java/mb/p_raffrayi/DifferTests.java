package mb.p_raffrayi;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;

import com.google.common.collect.ImmutableSet;

import io.usethesource.capsule.Set;
import mb.p_raffrayi.ITypeChecker.IOutput;
import mb.p_raffrayi.impl.Result;
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

    public DifferTests() {
        super(PRaffrayiSettings.incremental());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Local Differ tests
    ///////////////////////////////////////////////////////////////////////////

    @Test(timeout = 10000) public void testEmptyDiff() throws InterruptedException, ExecutionException {
        final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Scope, Integer, IDatum, Output<Integer, Scope>, EmptyI>>> future =
                runSingle(new ITypeChecker<Scope, Integer, IDatum, Output<Integer, Scope>, EmptyI>() {

                    @Override public IFuture<Output<Integer, Scope>> run(
                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, Output<Integer, Scope>, EmptyI> unit,
                            List<Scope> rootScopes) {
                        Scope root = rootScopes.get(0);
                        unit.initScope(root, Arrays.asList(), false);
                        return unit.runIncremental(restarted -> {
                            return CompletableFuture.completedFuture(Output.of(root));
                        });
                    }

                    @Override public EmptyI snapshot() {
                        return EmptyI.of();
                    }
                }, root, ScopeGraph.Immutable.of());

        IUnitResult<Scope, Integer, IDatum, Result<Scope, Integer, IDatum, Output<Integer, Scope>, EmptyI>> result = future.asJavaCompletion().get();

        assertEquals(Arrays.asList(), result.allFailures());
        final ScopeGraphDiff<Scope, Integer, IDatum> diff = result.diff();

        assertEquals(CapsuleUtil.immutableMap(), diff.added().scopes());
        assertEquals(CapsuleUtil.immutableSet(), diff.added().edges());

        assertEquals(CapsuleUtil.immutableMap(), diff.removed().scopes());
        assertEquals(CapsuleUtil.immutableSet(), diff.removed().edges());

        assertEquals(BiMap.Immutable.of(result.result().analysis().value(), root), diff.matchedScopes());
        assertEquals(EMPTY_BIMAP, diff.matchedEdges());
    }

    @Test(timeout = 10000) public void testAddedEdgeDiff() throws InterruptedException, ExecutionException {
        final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Scope, Integer, IDatum, Output<Integer, List<Scope>>, EmptyI>>> future =
                runSingle(new ITypeChecker<Scope, Integer, IDatum, Output<Integer, List<Scope>>, EmptyI>() {

                    @Override public IFuture<Output<Integer, List<Scope>>> run(
                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, Output<Integer, List<Scope>>, EmptyI> unit,
                            List<Scope> rootScopes) {
                        Scope root = rootScopes.get(0);
                        unit.initScope(root, Arrays.asList(LBL_1), false);
                        return unit.runIncremental(restarted -> {
                            final Scope d = unit.freshScope("d", Arrays.asList(), false, false);
                            unit.addEdge(root, LBL_1, d);
                            unit.closeEdge(root, LBL_1);
                            return CompletableFuture.completedFuture(Output.of(Arrays.asList(root, d)));
                        });
                    }

                    @Override public EmptyI snapshot() {
                        return EmptyI.of();
                    }

                }, root, ScopeGraph.Immutable.of());

        IUnitResult<Scope, Integer, IDatum, Result<Scope, Integer, IDatum, Output<Integer, List<Scope>>, EmptyI>> result = future.asJavaCompletion().get();
        List<Scope> resultScopes = result.result().analysis().value();

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

    @SuppressWarnings("unchecked") public <R extends IOutput<Scope, Integer, IDatum>> IFuture<IUnitResult<Scope, Integer, IDatum, Result<Scope, Integer, IDatum, R, EmptyI>>> runSingle(
            ITypeChecker<Scope, Integer, IDatum, R, EmptyI> typeChecker, Scope prevRoot,
            IScopeGraph.Immutable<Scope, Integer, IDatum> previousGraph) {
        // @formatter:off
        IUnitResult<Scope, Integer, IDatum, Result<Scope, Integer, IDatum, R, EmptyI>> childResult = UnitResult.<Scope, Integer, IDatum, Result<Scope, Integer, IDatum, R, EmptyI>>builder()
            .id("/./sub")
            .scopeGraph(previousGraph)
            .result(Result.of(null, null, previousGraph, ImmutableSet.of()))
            .addRootScopes(prevRoot)
            .build();
        // @formatter:on

        // @formatter:off
        IUnitResult<Scope, Integer, IDatum, Result<Scope, Integer, IDatum, EmptyResult, EmptyI>> parentResult = UnitResult.<Scope, Integer, IDatum, Result<Scope, Integer, IDatum, EmptyResult, EmptyI>>builder()
            .id("/.")
            .scopeGraph(previousGraph) // TODO: remove data?
            .result(Result.of(null, null, ScopeGraph.Immutable.of(), ImmutableSet.of()))
            .putSubUnitResults("sub", childResult)
            .build();
        // @formatter:on

        return run("/.", new ITypeChecker<Scope, Integer, IDatum, EmptyResult, EmptyI>() {

            @Override public IFuture<EmptyResult> run(IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyResult, EmptyI> unit,
                    List<Scope> rootScopes) {
                final Scope root = unit.stableFreshScope("s", CapsuleUtil.immutableSet(), false);
                final IFuture<IUnitResult<Scope, Integer, IDatum, Result<Scope, Integer, IDatum, R, EmptyI>>> subResult =
                        unit.add("sub", typeChecker, Arrays.asList(root));
                unit.closeScope(root);
                return unit.runIncremental(restarted -> {
                    return subResult.thenApply(__ -> EmptyResult.of());
                });
            }

            @Override public EmptyI snapshot() {
                return EmptyI.of();
            }

        }, labels, true, parentResult).thenApply(result -> {
            return (IUnitResult<Scope, Integer, IDatum, Result<Scope, Integer, IDatum, R, EmptyI>>) result.subUnitResults().get("sub");
        });

    }

}
