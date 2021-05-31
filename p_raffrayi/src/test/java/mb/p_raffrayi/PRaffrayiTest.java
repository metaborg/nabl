package mb.p_raffrayi;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import org.junit.Ignore;
import org.junit.Test;
import org.metaborg.util.future.AggregateFuture;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.NullCancel;
import org.metaborg.util.unit.Unit;

import com.google.common.collect.ImmutableList;

import io.usethesource.capsule.Set;
import mb.p_raffrayi.impl.Broker;
import mb.p_raffrayi.nameresolution.DataLeq;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelOrder;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.ecoop21.RegExpLabelWf;
import mb.scopegraph.regexp.IRegExp;
import mb.scopegraph.regexp.IRegExpMatcher;
import mb.scopegraph.regexp.RegExpMatcher;
import mb.scopegraph.regexp.impl.RegExpBuilder;

public class PRaffrayiTest {

    private <L, R> IFuture<IUnitResult<Scope, L, Datum, R>> run(String id, ITypeChecker<Scope, L, Datum, R> typeChecker,
            Iterable<L> edgeLabels) {
        return Broker.debug(id, typeChecker, new ScopeImpl(), edgeLabels, new NullCancel(), 0.3, 50);
        //return Broker.run(id, typeChecker, new ScopeImpl(), edgeLabels, new NullCancel());
    }

    ///////////////////////////////////////////////////////////////////////////

    @Test(timeout = 10000) public void testSingleNoop() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Object, Datum, Object>> future =
                run(".", new ITypeChecker<Scope, Object, Datum, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Object, Datum> unit,
                            List<Scope> roots) {
                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Object, Datum, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testSingleOneScope() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, Datum, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, Datum, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Datum> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Set.Immutable.of(1), false, false);
                        unit.addEdge(s, 1, s);
                        unit.closeEdge(s, 1);
                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Integer, Datum, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testSingleOneScope_NoClose() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, Datum, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, Datum, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Datum> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Set.Immutable.of(1), false, false);
                        unit.addEdge(s, 1, s);
                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Integer, Datum, Object> result = future.asJavaCompletion().get();
        assertNotNull(result.analysis());
        assertFalse(result.failures().isEmpty());
    }


    @Test(timeout = 10000) public void testSingleParentChild() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, Datum, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, Datum, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Datum> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Set.Immutable.of(), false, true);

                        IFuture<IUnitResult<Scope, Integer, Datum, Object>> subResult =
                                unit.add("sub", new ITypeChecker<Scope, Integer, Datum, Object>() {

                                    @Override public IFuture<Object>
                                            run(ITypeCheckerContext<Scope, Integer, Datum> unit, List<Scope> roots) {
                                        Scope root = roots.get(0);
                                        unit.initScope(root, Set.Immutable.of(), false);
                                        return CompletableFuture.completedFuture(Unit.unit);
                                    }

                                }, Arrays.asList(s));

                        unit.closeScope(s);

                        return subResult.thenCompose(r -> CompletableFuture.completedFuture(Unit.unit));
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Integer, Datum, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testSingleParentChild_ParentNoClose()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, Datum, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, Datum, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Datum> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Set.Immutable.of(), false, true);

                        IFuture<IUnitResult<Scope, Integer, Datum, Object>> subFuture =
                                unit.add("sub", new ITypeChecker<Scope, Integer, Datum, Object>() {

                                    @Override public IFuture<Object>
                                            run(ITypeCheckerContext<Scope, Integer, Datum> unit, List<Scope> roots) {
                                        Scope root = roots.get(0);
                                        unit.initScope(root, Set.Immutable.of(), false);
                                        return CompletableFuture.completedFuture(Unit.unit);
                                    }

                                }, Arrays.asList(s));

                        return subFuture.thenCompose(r -> CompletableFuture.completedFuture(Unit.unit));
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Integer, Datum, Object> result = future.asJavaCompletion().get();

        //        assertEquals(2, result.unitResults().size());
        //
        //        assertNotNull(result.unitResults().get(".").analysis());
        //        assertFalse(result.unitResults().get(".").failures().isEmpty());
        //
        //        assertNotNull(result.unitResults().get("sub").analysis());
        //        assertTrue(result.unitResults().get("sub").failures().isEmpty());
    }


    @Test(timeout = 10000) public void testSingleParentChild_ChildNoInitRoot()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, Datum, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, Datum, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Datum> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Set.Immutable.of(), false, true);

                        final IFuture<?> subResult = unit.add("sub", new ITypeChecker<Scope, Integer, Datum, Object>() {

                            @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Datum> unit,
                                    List<Scope> roots) {
                                return CompletableFuture.completedFuture(Unit.unit);
                            }

                        }, Arrays.asList(s));

                        unit.closeScope(s);

                        return subResult.handle((r, ex) -> Unit.unit);
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Integer, Datum, Object> result = future.asJavaCompletion().get();

        //        assertEquals(2, result.unitResults().size());
        //
        //        assertNotNull(result.unitResults().get(".").analysis());
        //        assertTrue(result.unitResults().get(".").failures().isEmpty());
        //
        //        assertNotNull(result.unitResults().get("sub").analysis());
        //        assertFalse(result.unitResults().get("sub").failures().isEmpty());
    }


    @Test(timeout = 10000) public void testSingleParentChild_ChildNoCloseEdge()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, Datum, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, Datum, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Datum> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Set.Immutable.of(), false, true);

                        final IFuture<?> subResult = unit.add("sub", new ITypeChecker<Scope, Integer, Datum, Object>() {

                            @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Datum> unit,
                                    List<Scope> roots) {
                                Scope root = roots.get(0);
                                unit.initScope(root, Set.Immutable.of(1), false);
                                return CompletableFuture.completedFuture(Unit.unit);
                            }

                        }, Arrays.asList(s));

                        unit.closeScope(s);

                        return subResult.handle((r, ex) -> Unit.unit);
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Integer, Datum, Object> result = future.asJavaCompletion().get();

        //        assertEquals(2, result.unitResults().size());
    }


    @Test(timeout = 10000) public void testTwoDatumsCloseEdgeDeadlockCycle()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, Datum, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, Datum, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Datum> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new ResolveBeforeCloseEdgeDatum(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new ResolveBeforeCloseEdgeDatum(2, 1), Arrays.asList(s)));

                        unit.closeScope(s);

                        return new AggregateFuture<>(subResults).handle((r, ex) -> Unit.unit);
                    }

                }, Arrays.asList(1, 2));

        final IUnitResult<Scope, Integer, Datum, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testThreeDatumsCloseEdgeDeadlockCycle()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, Datum, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, Datum, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Datum> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new ResolveBeforeCloseEdgeDatum(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new ResolveBeforeCloseEdgeDatum(2, 3), Arrays.asList(s)));
                        subResults.add(unit.add("three", new ResolveBeforeCloseEdgeDatum(3, 1), Arrays.asList(s)));

                        unit.closeScope(s);

                        return new AggregateFuture<>(subResults).handle((r, ex) -> Unit.unit);
                    }

                }, Arrays.asList(1, 2, 3));

        final IUnitResult<Scope, Integer, Datum, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testTwoDatumsCloseEdgeCycle() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, Datum, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, Datum, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Datum> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new CloseEdgeBeforeResolveDatum(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new CloseEdgeBeforeResolveDatum(2, 1), Arrays.asList(s)));

                        unit.closeScope(s);

                        return new AggregateFuture<>(subResults).handle((r, ex) -> Unit.unit);
                    }

                }, Arrays.asList(1, 2));

        final IUnitResult<Scope, Integer, Datum, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testThreeDatumsCloseEdgeCycle() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, Datum, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, Datum, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Datum> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new CloseEdgeBeforeResolveDatum(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new CloseEdgeBeforeResolveDatum(2, 3), Arrays.asList(s)));
                        subResults.add(unit.add("three", new CloseEdgeBeforeResolveDatum(3, 1), Arrays.asList(s)));

                        unit.closeScope(s);

                        return new AggregateFuture<>(subResults).handle((r, ex) -> Unit.unit);
                    }

                }, Arrays.asList(1, 2, 3));

        final IUnitResult<Scope, Integer, Datum, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testDatumCloseEdgeAndResolve_TargetStuckOnExternalRepresentation()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, Datum, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, Datum, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Datum> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new CloseEdgeBeforeResolveDatum(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new CloseBeforeResolveNoExternalRepDatum(2), Arrays.asList(s)));

                        unit.closeScope(s);

                        return new AggregateFuture<>(subResults).handle((r, ex) -> Unit.unit);
                    }

                }, Arrays.asList(1, 2));

        final IUnitResult<Scope, Integer, Datum, Object> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testDatumCloseEdgeAndResolve_TargetStuckOnFailingExternalRepresentation()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, Datum, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, Datum, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Datum> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new CloseEdgeBeforeResolveDatum(1, 2), Arrays.asList(s)));
                        subResults.add(
                                unit.add("two", new CloseBeforeResolveFailingExternalRepDatum(2), Arrays.asList(s)));

                        unit.closeScope(s);

                        return new AggregateFuture<>(subResults).handle((r, ex) -> Unit.unit);
                    }

                }, Arrays.asList(1, 2));

        final IUnitResult<Scope, Integer, Datum, Object> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testDatumCloseEdgeAndResolve_TargetStuckOnExceptionalExternalRepresentation()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, Datum, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, Datum, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Datum> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new CloseEdgeBeforeResolveDatum(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new CloseBeforeResolveExceptionalExternalRepDatum(2),
                                Arrays.asList(s)));

                        unit.closeScope(s);

                        return new AggregateFuture<>(subResults).handle((r, ex) -> Unit.unit);
                    }

                }, Arrays.asList(1, 2));

        final IUnitResult<Scope, Integer, Datum, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testDatumResolveAndCloseEdge_TargetStuckOnExternalRepresentation()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, Datum, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, Datum, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Datum> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new ResolveBeforeCloseEdgeDatum(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new CloseBeforeResolveNoExternalRepDatum(2), Arrays.asList(s)));

                        unit.closeScope(s);

                        return new AggregateFuture<>(subResults).handle((r, ex) -> Unit.unit);
                    }

                }, Arrays.asList(1, 2));

        final IUnitResult<Scope, Integer, Datum, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testTwoDatumsSetDatumDeadlockCycle()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, Datum, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, Datum, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Datum> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new ResolveBeforeSetDatumDatum(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new ResolveBeforeSetDatumDatum(2, 1), Arrays.asList(s)));

                        unit.closeScope(s);

                        return new AggregateFuture<>(subResults).handle((r, ex) -> Unit.unit);
                    }

                }, Arrays.asList(1, 2));

        final IUnitResult<Scope, Integer, Datum, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testThreeDatumsSetDatumDeadlockCycle()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, Datum, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, Datum, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Datum> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new ResolveBeforeSetDatumDatum(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new ResolveBeforeSetDatumDatum(2, 3), Arrays.asList(s)));
                        subResults.add(unit.add("three", new ResolveBeforeSetDatumDatum(3, 1), Arrays.asList(s)));

                        unit.closeScope(s);

                        return new AggregateFuture<>(subResults).handle((r, ex) -> Unit.unit);
                    }

                }, Arrays.asList(1, 2, 3));

        final IUnitResult<Scope, Integer, Datum, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testTwoDatumsSetDatumCycle() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, Datum, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, Datum, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Datum> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new SetDatumBeforeResolveDatum(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new SetDatumBeforeResolveDatum(2, 1), Arrays.asList(s)));

                        unit.closeScope(s);

                        return new AggregateFuture<>(subResults).handle((r, ex) -> Unit.unit);
                    }

                }, Arrays.asList(1, 2));

        final IUnitResult<Scope, Integer, Datum, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testThreeDatumsSetDatumCycle() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, Datum, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, Datum, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Datum> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new SetDatumBeforeResolveDatum(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new SetDatumBeforeResolveDatum(2, 3), Arrays.asList(s)));
                        subResults.add(unit.add("three", new SetDatumBeforeResolveDatum(3, 1), Arrays.asList(s)));

                        unit.closeScope(s);

                        return new AggregateFuture<>(subResults).handle((r, ex) -> Unit.unit);
                    }

                }, Arrays.asList(1, 2, 3));

        final IUnitResult<Scope, Integer, Datum, Object> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testDatumSetDatumAndResolve_TargetStuckOnDatum()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, Datum, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, Datum, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Datum> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new SetDatumBeforeResolveDatum(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new CloseBeforeResolveNoDatumDatum(2), Arrays.asList(s)));

                        unit.closeScope(s);

                        return new AggregateFuture<>(subResults).handle((r, ex) -> Unit.unit);
                    }

                }, Arrays.asList(1, 2));

        final IUnitResult<Scope, Integer, Datum, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testDatumResolveAndSetDatum_TargetStuckOnDatum()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, Datum, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, Datum, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Datum> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new ResolveBeforeSetDatumDatum(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new CloseBeforeResolveNoDatumDatum(2), Arrays.asList(s)));

                        unit.closeScope(s);

                        return new AggregateFuture<>(subResults).handle((r, ex) -> Unit.unit);
                    }

                }, Arrays.asList(1, 2));

        final IUnitResult<Scope, Integer, Datum, Object> result = future.asJavaCompletion().get();
    }

    @Ignore @Test(timeout = 10000) public void testFailureInRun() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Object, Datum, Object>> future =
                run(".", new ITypeChecker<Scope, Object, Datum, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Object, Datum> unit,
                            List<Scope> roots) {
                        throw new ExpectedFailure();
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Object, Datum, Object> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testNoRunResult() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Object, Datum, Object>> future =
                run(".", new ITypeChecker<Scope, Object, Datum, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Object, Datum> unit,
                            List<Scope> roots) {
                        return new CompletableFuture<>();
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Object, Datum, Object> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testExceptionalRunResult() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Object, Datum, Object>> future =
                run(".", new ITypeChecker<Scope, Object, Datum, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Object, Datum> unit,
                            List<Scope> roots) {
                        return CompletableFuture.completedExceptionally(new ExpectedFailure());
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Object, Datum, Object> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testExceptionalRunResult_NoClose()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Object, Datum, Object>> future =
                run(".", new ITypeChecker<Scope, Object, Datum, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Object, Datum> unit,
                            List<Scope> roots) {
                        final Scope s = unit.freshScope("s", Collections.emptySet(), true, false);
                        return CompletableFuture.completedExceptionally(new ExpectedFailure());
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Object, Datum, Object> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testFailureInQueryPredicate() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Object, Datum, Object>> future =
                run(".", new ITypeChecker<Scope, Object, Datum, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Object, Datum> unit,
                            List<Scope> roots) {
                        final Scope s = unit.freshScope("s", Collections.emptyList(), true, true);

                        final IFuture<?> subResult = unit.add("sub", new ITypeChecker<Scope, Object, Datum, Object>() {

                            @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Object, Datum> unit,
                                    List<Scope> rootScopes) {
                                final Scope s = rootScopes.get(0);

                                return unit
                                        .query(s, LabelWf.any(), LabelOrder.none(), new DataWf<Scope, Object, Datum>() {
                                            @Override public IFuture<Boolean> wf(Datum d,
                                                    ITypeCheckerContext<Scope, Object, Datum> context, ICancel cancel)
                                                    throws InterruptedException {
                                                throw new ExpectedFailure();
                                            }
                                        }, DataLeq.none()).thenApply(r -> null);
                            }

                        }, Collections.singletonList(s));

                        unit.closeScope(s);

                        return subResult.handle((r, ex) -> Unit.unit);
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Object, Datum, Object> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testExceptionalQueryPredicateResult()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Object, Datum, Object>> future =
                run(".", new ITypeChecker<Scope, Object, Datum, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Object, Datum> unit,
                            List<Scope> roots) {
                        final Scope s = unit.freshScope("s", Collections.emptyList(), true, true);

                        IFuture<?> subResult = unit.add("sub", new ITypeChecker<Scope, Object, Datum, Object>() {

                            @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Object, Datum> unit,
                                    List<Scope> rootScopes) {
                                final Scope s = rootScopes.get(0);

                                return unit
                                        .query(s, LabelWf.any(), LabelOrder.none(), new DataWf<Scope, Object, Datum>() {
                                            @Override public IFuture<Boolean> wf(Datum d,
                                                    ITypeCheckerContext<Scope, Object, Datum> context, ICancel cancel)
                                                    throws InterruptedException {
                                                return CompletableFuture.completedExceptionally(new ExpectedFailure());
                                            }
                                        }, DataLeq.none()).thenApply(r -> null);
                            }

                        }, Collections.singletonList(s));

                        unit.closeScope(s);

                        return subResult.handle((r, ex) -> Unit.unit);
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Object, Datum, Object> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testNoQueryPredicateResult() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Object, Datum, Object>> future =
                run(".", new ITypeChecker<Scope, Object, Datum, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Object, Datum> unit,
                            List<Scope> roots) {
                        final Scope s = unit.freshScope("s", Collections.emptyList(), true, true);

                        IFuture<?> subResult = unit.add("sub", new ITypeChecker<Scope, Object, Datum, Object>() {

                            @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Object, Datum> unit,
                                    List<Scope> rootScopes) {
                                final Scope s = rootScopes.get(0);

                                return unit
                                        .query(s, LabelWf.any(), LabelOrder.none(), new DataWf<Scope, Object, Datum>() {
                                            @Override public IFuture<Boolean> wf(Datum d,
                                                    ITypeCheckerContext<Scope, Object, Datum> context, ICancel cancel)
                                                    throws InterruptedException {
                                                return new CompletableFuture<>();
                                            }
                                        }, DataLeq.none()).thenApply(r -> null);
                            }

                        }, Collections.singletonList(s));

                        unit.closeScope(s);

                        return subResult.handle((r, ex) -> Unit.unit);
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Object, Datum, Object> result = future.asJavaCompletion().get();
    }

    ///////////////////////////////////////////////////////////////////////////

    private final class ResolveBeforeCloseEdgeDatum implements ITypeChecker<Scope, Integer, Datum, Object> {

        private final Integer ownLabel;
        private final List<Integer> queryLabels;

        public ResolveBeforeCloseEdgeDatum(Integer ownLabel, Integer... queryLabels) {
            this.ownLabel = ownLabel;
            this.queryLabels = ImmutableList.copyOf(queryLabels);
        }

        @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Datum> unit, List<Scope> rootScopes) {
            Scope s = rootScopes.get(0);
            unit.initScope(s, Set.Immutable.of(ownLabel), false);

            Scope s1 = unit.freshScope("s'", Collections.emptySet(), false, true);

            unit.addEdge(s, ownLabel, s1);
            unit.closeEdge(s, ownLabel);

            final List<IFuture<?>> results = new ArrayList<>();
            for(Integer queryLabel : queryLabels) {
                final RegExpBuilder<Integer> reb = new RegExpBuilder<>();
                final IRegExp<Integer> re = reb.and(reb.symbol(queryLabel), reb.complement(reb.emptySet()));
                final IRegExpMatcher<Integer> rem = RegExpMatcher.create(re);
                results.add(unit.query(s, new RegExpLabelWf<>(rem), LabelOrder.none(), DataWf.any(), DataLeq.none()));
            }
            final IFuture<List<Object>> result = new AggregateFuture<>(results).whenComplete((r, ex) -> {
                unit.closeScope(s1);
            });

            return result.handle((r, ex) -> Unit.unit);
        }

    }

    private final class CloseEdgeBeforeResolveDatum implements ITypeChecker<Scope, Integer, Datum, Object> {

        private final Integer ownLabel;
        private final List<Integer> queryLabels;

        public CloseEdgeBeforeResolveDatum(Integer ownLabel, Integer... queryLabels) {
            this.ownLabel = ownLabel;
            this.queryLabels = ImmutableList.copyOf(queryLabels);
        }

        @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Datum> unit, List<Scope> rootScopes) {
            Scope s = rootScopes.get(0);
            unit.initScope(s, Set.Immutable.of(ownLabel), false);

            Scope s1 = unit.freshScope("s'", Collections.emptySet(), false, true);

            unit.addEdge(s, ownLabel, s1);
            unit.closeEdge(s, ownLabel);

            unit.closeScope(s1);

            final List<IFuture<?>> results = new ArrayList<>();
            for(Integer queryLabel : queryLabels) {
                final RegExpBuilder<Integer> reb = new RegExpBuilder<>();
                final IRegExp<Integer> re = reb.and(reb.symbol(queryLabel), reb.complement(reb.emptySet()));
                final IRegExpMatcher<Integer> rem = RegExpMatcher.create(re);
                results.add(unit.query(s, new RegExpLabelWf<>(rem), LabelOrder.none(), DataWf.any(), DataLeq.none()));
            }
            final IFuture<List<Object>> result = new AggregateFuture<>(results);

            return result.handle((r, ex) -> Unit.unit);
        }

    }

    private final class ResolveBeforeSetDatumDatum implements ITypeChecker<Scope, Integer, Datum, Object> {

        private final Integer ownLabel;
        private final List<Integer> queryLabels;

        public ResolveBeforeSetDatumDatum(Integer ownLabel, Integer... queryLabels) {
            this.ownLabel = ownLabel;
            this.queryLabels = ImmutableList.copyOf(queryLabels);
        }

        @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Datum> unit, List<Scope> rootScopes) {
            Scope s = rootScopes.get(0);
            unit.initScope(s, Set.Immutable.of(ownLabel), false);

            Scope s1 = unit.freshScope("s'", Collections.emptySet(), true, false);

            unit.addEdge(s, ownLabel, s1);
            unit.closeEdge(s, ownLabel);

            final List<IFuture<?>> results = new ArrayList<>();
            for(Integer queryLabel : queryLabels) {
                final RegExpBuilder<Integer> reb = new RegExpBuilder<>();
                final IRegExp<Integer> re = reb.and(reb.symbol(queryLabel), reb.complement(reb.emptySet()));
                final IRegExpMatcher<Integer> rem = RegExpMatcher.create(re);
                results.add(unit.query(s, new RegExpLabelWf<>(rem), LabelOrder.none(), DataWf.any(), DataLeq.none()));
            }
            final IFuture<List<Object>> result = new AggregateFuture<>(results).whenComplete((r, ex) -> {
                unit.setDatum(s1, s1);
            });

            return result.handle((r, ex) -> Unit.unit);
        }

    }

    private final class SetDatumBeforeResolveDatum implements ITypeChecker<Scope, Integer, Datum, Object> {

        private final Integer ownLabel;
        private final List<Integer> queryLabels;

        public SetDatumBeforeResolveDatum(Integer ownLabel, Integer... queryLabels) {
            this.ownLabel = ownLabel;
            this.queryLabels = ImmutableList.copyOf(queryLabels);
        }

        @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Datum> unit, List<Scope> rootScopes) {
            Scope s = rootScopes.get(0);
            unit.initScope(s, Set.Immutable.of(ownLabel), false);

            Scope s1 = unit.freshScope("s'", Collections.emptySet(), true, false);

            unit.addEdge(s, ownLabel, s1);
            unit.closeEdge(s, ownLabel);

            unit.setDatum(s1, s1);

            final List<IFuture<?>> results = new ArrayList<>();
            for(Integer queryLabel : queryLabels) {
                final RegExpBuilder<Integer> reb = new RegExpBuilder<>();
                final IRegExp<Integer> re = reb.and(reb.symbol(queryLabel), reb.complement(reb.emptySet()));
                final IRegExpMatcher<Integer> rem = RegExpMatcher.create(re);
                results.add(unit.query(s, new RegExpLabelWf<>(rem), LabelOrder.none(), DataWf.any(), DataLeq.none()));
            }
            final IFuture<List<Object>> result = new AggregateFuture<>(results);

            return result.handle((r, ex) -> Unit.unit);
        }

    }

    private final class CloseBeforeResolveNoExternalRepDatum implements ITypeChecker<Scope, Integer, Datum, Object> {

        private final Integer ownLabel;
        private final List<Integer> queryLabels;

        public CloseBeforeResolveNoExternalRepDatum(Integer ownLabel, Integer... queryLabels) {
            this.ownLabel = ownLabel;
            this.queryLabels = ImmutableList.copyOf(queryLabels);
        }

        @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Datum> unit, List<Scope> rootScopes) {
            Scope s = rootScopes.get(0);
            unit.initScope(s, Set.Immutable.of(ownLabel), false);

            Scope s1 = unit.freshScope("s'", Collections.emptySet(), true, false);

            unit.addEdge(s, ownLabel, s1);
            unit.closeEdge(s, ownLabel);

            unit.setDatum(s1, s1);

            final List<IFuture<?>> results = new ArrayList<>();
            for(Integer queryLabel : queryLabels) {
                final RegExpBuilder<Integer> reb = new RegExpBuilder<>();
                final IRegExp<Integer> re = reb.and(reb.symbol(queryLabel), reb.complement(reb.emptySet()));
                final IRegExpMatcher<Integer> rem = RegExpMatcher.create(re);
                results.add(unit.query(s, new RegExpLabelWf<>(rem), LabelOrder.none(), DataWf.any(), DataLeq.none()));
            }
            final IFuture<List<Object>> result = new AggregateFuture<>(results);

            return result.handle((r, ex) -> Unit.unit);
        }

        @Override public IFuture<Datum> getExternalDatum(Datum datum) {
            return new CompletableFuture<>();
        }

    }

    private final class CloseBeforeResolveFailingExternalRepDatum
            implements ITypeChecker<Scope, Integer, Datum, Object> {

        private final Integer ownLabel;
        private final List<Integer> queryLabels;

        public CloseBeforeResolveFailingExternalRepDatum(Integer ownLabel, Integer... queryLabels) {
            this.ownLabel = ownLabel;
            this.queryLabels = ImmutableList.copyOf(queryLabels);
        }

        @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Datum> unit, List<Scope> rootScopes) {
            Scope s = rootScopes.get(0);
            unit.initScope(s, Set.Immutable.of(ownLabel), false);

            Scope s1 = unit.freshScope("s'", Collections.emptySet(), true, false);

            unit.addEdge(s, ownLabel, s1);
            unit.closeEdge(s, ownLabel);

            unit.setDatum(s1, s1);

            final List<IFuture<?>> results = new ArrayList<>();
            for(Integer queryLabel : queryLabels) {
                final RegExpBuilder<Integer> reb = new RegExpBuilder<>();
                final IRegExp<Integer> re = reb.and(reb.symbol(queryLabel), reb.complement(reb.emptySet()));
                final IRegExpMatcher<Integer> rem = RegExpMatcher.create(re);
                results.add(unit.query(s, new RegExpLabelWf<>(rem), LabelOrder.none(), DataWf.any(), DataLeq.none()));
            }
            final IFuture<List<Object>> result = new AggregateFuture<>(results);

            return result.handle((r, ex) -> Unit.unit);
        }

        @Override public IFuture<Datum> getExternalDatum(Datum datum) {
            throw new ExpectedFailure();
        }

    }

    private final class CloseBeforeResolveExceptionalExternalRepDatum
            implements ITypeChecker<Scope, Integer, Datum, Object> {

        private final Integer ownLabel;
        private final List<Integer> queryLabels;

        public CloseBeforeResolveExceptionalExternalRepDatum(Integer ownLabel, Integer... queryLabels) {
            this.ownLabel = ownLabel;
            this.queryLabels = ImmutableList.copyOf(queryLabels);
        }

        @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Datum> unit, List<Scope> rootScopes) {
            Scope s = rootScopes.get(0);
            unit.initScope(s, Set.Immutable.of(ownLabel), false);

            Scope s1 = unit.freshScope("s'", Collections.emptySet(), true, false);

            unit.addEdge(s, ownLabel, s1);
            unit.closeEdge(s, ownLabel);

            unit.setDatum(s1, s1);

            final List<IFuture<?>> results = new ArrayList<>();
            for(Integer queryLabel : queryLabels) {
                final RegExpBuilder<Integer> reb = new RegExpBuilder<>();
                final IRegExp<Integer> re = reb.and(reb.symbol(queryLabel), reb.complement(reb.emptySet()));
                final IRegExpMatcher<Integer> rem = RegExpMatcher.create(re);
                results.add(unit.query(s, new RegExpLabelWf<>(rem), LabelOrder.none(), DataWf.any(), DataLeq.none()));
            }
            final IFuture<List<Object>> result = new AggregateFuture<>(results);

            return result.handle((r, ex) -> Unit.unit);
        }

        @Override public IFuture<Datum> getExternalDatum(Datum datum) {
            return CompletableFuture.completedExceptionally(new ExpectedFailure());
        }

    }

    private final class CloseBeforeResolveNoDatumDatum implements ITypeChecker<Scope, Integer, Datum, Object> {

        private final Integer ownLabel;
        private final List<Integer> queryLabels;

        public CloseBeforeResolveNoDatumDatum(Integer ownLabel, Integer... queryLabels) {
            this.ownLabel = ownLabel;
            this.queryLabels = ImmutableList.copyOf(queryLabels);
        }

        @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Datum> unit, List<Scope> rootScopes) {
            Scope s = rootScopes.get(0);
            unit.initScope(s, Set.Immutable.of(ownLabel), false);

            Scope s1 = unit.freshScope("s'", Collections.emptySet(), true, false);

            unit.addEdge(s, ownLabel, s1);
            unit.closeEdge(s, ownLabel);

            final List<IFuture<?>> results = new ArrayList<>();
            for(Integer queryLabel : queryLabels) {
                final RegExpBuilder<Integer> reb = new RegExpBuilder<>();
                final IRegExp<Integer> re = reb.and(reb.symbol(queryLabel), reb.complement(reb.emptySet()));
                final IRegExpMatcher<Integer> rem = RegExpMatcher.create(re);
                results.add(unit.query(s, new RegExpLabelWf<>(rem), LabelOrder.none(), DataWf.any(), DataLeq.none()));
            }
            final IFuture<List<Object>> result = new AggregateFuture<>(results);

            return result.handle((r, ex) -> Unit.unit);
        }

        @Override public IFuture<Datum> getExternalDatum(Datum datum) {
            return new CompletableFuture<>();
        }

    }

    ///////////////////////////////////////////////////////////////////////////

    private class ScopeImpl implements IScopeImpl<Scope, Datum> {

        private int count = 0;
        private java.util.Set<Scope> scopes = new HashSet<>();

        @Override public Scope make(String id, String name) {
            Scope s = new Scope(id, count++);
            scopes.add(s);
            return s;
        }

        @Override public String id(Scope scope) {
            return scope.id;
        }

        @Override public Collection<Scope> getAllScopes(Datum datum) {
            return Collections.unmodifiableSet(scopes);
        }

        @Override public Datum substituteScopes(Datum datum, Map<Scope, Scope> substitution) {
            throw new RuntimeException("Not implemented for tests.");
        }

    };

    private final static class Scope implements Datum {

        private final String id;
        private final int index;

        public Scope(String id, int index) {
            this.id = id;
            this.index = index;
        }

        @Override public int hashCode() {
            return Objects.hash(id, index);
        }

        @Override public boolean equals(Object obj) {
            if(this == obj)
                return true;
            if(obj == null)
                return false;
            if(getClass() != obj.getClass())
                return false;
            Scope other = (Scope) obj;
            return Objects.equals(id, other.id) && index == other.index;
        }

    }

    private static interface Datum {

    }

    ///////////////////////////////////////////////////////////////////////////

    private static class ExpectedFailure extends RuntimeException {

    }

}