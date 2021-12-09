package mb.p_raffrayi;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.future.AggregateFuture;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.task.ICancel;

import com.google.common.collect.ImmutableList;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.Set.Immutable;
import mb.p_raffrayi.nameresolution.DataLeq;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelOrder;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.ecoop21.RegExpLabelWf;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.scopegraph.patching.IPatchCollection;
import mb.scopegraph.regexp.IRegExp;
import mb.scopegraph.regexp.IRegExpMatcher;
import mb.scopegraph.regexp.RegExpMatcher;
import mb.scopegraph.regexp.impl.RegExpBuilder;

public class PRaffrayiTest extends PRaffrayiTestBase {

    ///////////////////////////////////////////////////////////////////////////

    @Test(timeout = 10000) public void testSingleNoop() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Object, IDatum, EmptyO, EmptyO>> future =
                this.run(".", new ITypeChecker<Scope, Object, IDatum, EmptyO, EmptyO>() {

                    @Override public IFuture<EmptyO> run(
                            IIncrementalTypeCheckerContext<Scope, Object, IDatum, EmptyO, EmptyO> unit,
                            List<Scope> roots) {
                        return CompletableFuture.completedFuture(EmptyO.of());
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Object, IDatum, EmptyO, EmptyO> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testSingleOneScope() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI>> future =
                run(".", new ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI>() {

                    @Override public IFuture<EmptyI> run(
                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Set.Immutable.of(1), false, false);
                        unit.addEdge(s, 1, s);
                        unit.closeEdge(s, 1);
                        return CompletableFuture.completedFuture(EmptyI.of());
                    }

                }, Set.Immutable.of(1));

        final IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testSingleOneScope_NoClose() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI>> future =
                run(".", new ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI>() {

                    @Override public IFuture<EmptyI> run(
                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Set.Immutable.of(1), false, false);
                        unit.addEdge(s, 1, s);
                        return CompletableFuture.completedFuture(EmptyI.of());
                    }

                }, Set.Immutable.of(1));

        final IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI> result = future.asJavaCompletion().get();
        assertNotNull(result.analysis());
        assertFalse(result.failures().isEmpty());
    }


    @Test(timeout = 10000) public void testSingleParentChild() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI>> future =
                run(".", new ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI>() {

                    @Override public IFuture<EmptyI> run(
                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Set.Immutable.of(), false, true);

                        IFuture<IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI>> subResult =
                                unit.add("sub", new ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI>() {

                                    @Override public IFuture<EmptyI> run(
                                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit,
                                            List<Scope> roots) {
                                        Scope root = roots.get(0);
                                        unit.initScope(root, Set.Immutable.of(), false);
                                        return CompletableFuture.completedFuture(EmptyI.of());
                                    }

                                }, Arrays.asList(s));

                        unit.closeScope(s);

                        return subResult.thenApply(r -> EmptyI.of());
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testSingleParentChild_ParentNoClose()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI>> future =
                run(".", new ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI>() {

                    @Override public IFuture<EmptyI> run(
                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Set.Immutable.of(), false, true);

                        IFuture<IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI>> subFuture =
                                unit.add("sub", new ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI>() {

                                    @Override public IFuture<EmptyI> run(
                                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit,
                                            List<Scope> roots) {
                                        Scope root = roots.get(0);
                                        unit.initScope(root, Set.Immutable.of(), false);
                                        return CompletableFuture.completedFuture(EmptyI.of());
                                    }

                                }, Arrays.asList(s));

                        return subFuture.thenApply(r -> EmptyI.of());
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI> result = future.asJavaCompletion().get();

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
        final IFuture<IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI>> future =
                run(".", new ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI>() {

                    @Override public IFuture<EmptyI> run(
                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Set.Immutable.of(), false, true);

                        final IFuture<?> subResult =
                                unit.add("sub", new ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI>() {

                                    @Override public IFuture<EmptyI> run(
                                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit,
                                            List<Scope> roots) {
                                        return CompletableFuture.completedFuture(EmptyI.of());
                                    }

                                }, Arrays.asList(s));

                        unit.closeScope(s);

                        return subResult.handle((r, ex) -> EmptyI.of());
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI> result = future.asJavaCompletion().get();

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
        final IFuture<IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI>> future =
                run(".", new ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI>() {

                    @Override public IFuture<EmptyI> run(
                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Set.Immutable.of(), false, true);

                        final IFuture<?> subResult =
                                unit.add("sub", new ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI>() {

                                    @Override public IFuture<EmptyI> run(
                                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit,
                                            List<Scope> roots) {
                                        Scope root = roots.get(0);
                                        unit.initScope(root, Set.Immutable.of(1), false);
                                        return CompletableFuture.completedFuture(EmptyI.of());
                                    }

                                }, Arrays.asList(s));

                        unit.closeScope(s);

                        return subResult.handle((r, ex) -> EmptyI.of());
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI> result = future.asJavaCompletion().get();

        //        assertEquals(2, result.unitResults().size());
    }


    @Test(timeout = 10000) public void testTwoDatumsCloseEdgeDeadlockCycle()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI>> future =
                run(".", new ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI>() {

                    @Override public IFuture<EmptyI> run(
                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new ResolveBeforeCloseEdgeDatum(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new ResolveBeforeCloseEdgeDatum(2, 1), Arrays.asList(s)));

                        unit.closeScope(s);

                        return AggregateFuture.of(subResults).handle((r, ex) -> EmptyI.of());
                    }

                }, Arrays.asList(1, 2));

        final IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testThreeDatumsCloseEdgeDeadlockCycle()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI>> future =
                run(".", new ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI>() {

                    @Override public IFuture<EmptyI> run(
                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new ResolveBeforeCloseEdgeDatum(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new ResolveBeforeCloseEdgeDatum(2, 3), Arrays.asList(s)));
                        subResults.add(unit.add("three", new ResolveBeforeCloseEdgeDatum(3, 1), Arrays.asList(s)));

                        unit.closeScope(s);

                        return AggregateFuture.of(subResults).handle((r, ex) -> EmptyI.of());
                    }

                }, Arrays.asList(1, 2, 3));

        final IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testTwoDatumsCloseEdgeCycle() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI>> future =
                run(".", new ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI>() {

                    @Override public IFuture<EmptyI> run(
                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new CloseEdgeBeforeResolveDatum(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new CloseEdgeBeforeResolveDatum(2, 1), Arrays.asList(s)));

                        unit.closeScope(s);

                        return AggregateFuture.of(subResults).handle((r, ex) -> EmptyI.of());
                    }

                }, Arrays.asList(1, 2));

        final IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testThreeDatumsCloseEdgeCycle() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI>> future =
                run(".", new ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI>() {

                    @Override public IFuture<EmptyI> run(
                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new CloseEdgeBeforeResolveDatum(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new CloseEdgeBeforeResolveDatum(2, 3), Arrays.asList(s)));
                        subResults.add(unit.add("three", new CloseEdgeBeforeResolveDatum(3, 1), Arrays.asList(s)));

                        unit.closeScope(s);

                        return AggregateFuture.of(subResults).handle((r, ex) -> EmptyI.of());
                    }

                }, Arrays.asList(1, 2, 3));

        final IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testDatumCloseEdgeAndResolve_TargetStuckOnExternalRepresentation()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI>> future =
                run(".", new ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI>() {

                    @Override public IFuture<EmptyI> run(
                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new CloseEdgeBeforeResolveDatum(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new CloseBeforeResolveNoExternalRepDatum(2), Arrays.asList(s)));

                        unit.closeScope(s);

                        return AggregateFuture.of(subResults).handle((r, ex) -> EmptyI.of());
                    }

                }, Arrays.asList(1, 2));

        final IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testDatumCloseEdgeAndResolve_TargetStuckOnFailingExternalRepresentation()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI>> future =
                run(".", new ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI>() {

                    @Override public IFuture<EmptyI> run(
                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new CloseEdgeBeforeResolveDatum(1, 2), Arrays.asList(s)));
                        subResults.add(
                                unit.add("two", new CloseBeforeResolveFailingExternalRepDatum(2), Arrays.asList(s)));

                        unit.closeScope(s);

                        return AggregateFuture.of(subResults).handle((r, ex) -> EmptyI.of());
                    }

                }, Arrays.asList(1, 2));

        final IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testDatumCloseEdgeAndResolve_TargetStuckOnExceptionalExternalRepresentation()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI>> future =
                run(".", new ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI>() {

                    @Override public IFuture<EmptyI> run(
                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new CloseEdgeBeforeResolveDatum(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new CloseBeforeResolveExceptionalExternalRepDatum(2),
                                Arrays.asList(s)));

                        unit.closeScope(s);

                        return AggregateFuture.of(subResults).handle((r, ex) -> EmptyI.of());
                    }

                }, Arrays.asList(1, 2));

        final IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testDatumResolveAndCloseEdge_TargetStuckOnExternalRepresentation()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI>> future =
                run(".", new ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI>() {

                    @Override public IFuture<EmptyI> run(
                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new ResolveBeforeCloseEdgeDatum(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new CloseBeforeResolveNoExternalRepDatum(2), Arrays.asList(s)));

                        unit.closeScope(s);

                        return AggregateFuture.of(subResults).handle((r, ex) -> EmptyI.of());
                    }

                }, Arrays.asList(1, 2));

        final IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testTwoDatumsSetDatumDeadlockCycle()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI>> future =
                run(".", new ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI>() {

                    @Override public IFuture<EmptyI> run(
                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new ResolveBeforeSetDatumDatum(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new ResolveBeforeSetDatumDatum(2, 1), Arrays.asList(s)));

                        unit.closeScope(s);

                        return AggregateFuture.of(subResults).handle((r, ex) -> EmptyI.of());
                    }

                }, Arrays.asList(1, 2));

        final IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testThreeDatumsSetDatumDeadlockCycle()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI>> future =
                run(".", new ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI>() {

                    @Override public IFuture<EmptyI> run(
                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new ResolveBeforeSetDatumDatum(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new ResolveBeforeSetDatumDatum(2, 3), Arrays.asList(s)));
                        subResults.add(unit.add("three", new ResolveBeforeSetDatumDatum(3, 1), Arrays.asList(s)));

                        unit.closeScope(s);

                        return AggregateFuture.of(subResults).handle((r, ex) -> EmptyI.of());
                    }

                }, Arrays.asList(1, 2, 3));

        final IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testTwoDatumsSetDatumCycle() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI>> future =
                run(".", new ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI>() {

                    @Override public IFuture<EmptyI> run(
                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new SetDatumBeforeResolveDatum(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new SetDatumBeforeResolveDatum(2, 1), Arrays.asList(s)));

                        unit.closeScope(s);

                        return AggregateFuture.of(subResults).handle((r, ex) -> EmptyI.of());
                    }

                }, Arrays.asList(1, 2));

        final IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testThreeDatumsSetDatumCycle() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI>> future =
                run(".", new ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI>() {

                    @Override public IFuture<EmptyI> run(
                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new SetDatumBeforeResolveDatum(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new SetDatumBeforeResolveDatum(2, 3), Arrays.asList(s)));
                        subResults.add(unit.add("three", new SetDatumBeforeResolveDatum(3, 1), Arrays.asList(s)));

                        unit.closeScope(s);

                        return AggregateFuture.of(subResults).handle((r, ex) -> EmptyI.of());
                    }

                }, Arrays.asList(1, 2, 3));

        final IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testDatumSetDatumAndResolve_TargetStuckOnDatum()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI>> future =
                run(".", new ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI>() {

                    @Override public IFuture<EmptyI> run(
                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new SetDatumBeforeResolveDatum(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new CloseBeforeResolveNoDatumDatum(2), Arrays.asList(s)));

                        unit.closeScope(s);

                        return AggregateFuture.of(subResults).handle((r, ex) -> EmptyI.of());
                    }

                }, Arrays.asList(1, 2));

        final IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testDatumResolveAndSetDatum_TargetStuckOnDatum()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI>> future =
                run(".", new ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI>() {

                    @Override public IFuture<EmptyI> run(
                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new ResolveBeforeSetDatumDatum(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new CloseBeforeResolveNoDatumDatum(2), Arrays.asList(s)));

                        unit.closeScope(s);

                        return AggregateFuture.of(subResults).handle((r, ex) -> EmptyI.of());
                    }

                }, Arrays.asList(1, 2));

        final IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testFailureInRun() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Object, IDatum, EmptyO, EmptyO>> future =
                run(".", new ITypeChecker<Scope, Object, IDatum, EmptyO, EmptyO>() {

                    @Override public IFuture<EmptyO> run(
                            IIncrementalTypeCheckerContext<Scope, Object, IDatum, EmptyO, EmptyO> unit,
                            List<Scope> roots) {
                        throw new ExpectedFailure();
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Object, IDatum, EmptyO, EmptyO> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testFailureInIncrementalRun() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Object, IDatum, EmptyO, EmptyO>> future =
                run(".", new ITypeChecker<Scope, Object, IDatum, EmptyO, EmptyO>() {

                    @Override public IFuture<EmptyO> run(
                            IIncrementalTypeCheckerContext<Scope, Object, IDatum, EmptyO, EmptyO> unit,
                            List<Scope> roots) {
                        return unit.runIncremental(__ -> {
                            throw new ExpectedFailure();
                        });
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Object, IDatum, EmptyO, EmptyO> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testFailureInExtractLocal() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Object, IDatum, EmptyO, EmptyO>> future =
                run(".", new ITypeChecker<Scope, Object, IDatum, EmptyO, EmptyO>() {

                    @Override public IFuture<EmptyO> run(
                            IIncrementalTypeCheckerContext<Scope, Object, IDatum, EmptyO, EmptyO> unit,
                            List<Scope> roots) {
                        // formatter:off
                        return unit.<EmptyO>runIncremental(__ -> CompletableFuture.completedFuture(EmptyO.of()), x -> {
                            throw new ExpectedFailure();
                        }, CompletableFuture::completed);
                        // formatter:on
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Object, IDatum, EmptyO, EmptyO> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testFailureInCombine() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Object, IDatum, EmptyO, EmptyO>> future =
                run(".", new ITypeChecker<Scope, Object, IDatum, EmptyO, EmptyO>() {

                    @Override public IFuture<EmptyO> run(
                            IIncrementalTypeCheckerContext<Scope, Object, IDatum, EmptyO, EmptyO> unit,
                            List<Scope> roots) {
                        // formatter:off
                        return unit.<EmptyO>runIncremental(__ -> CompletableFuture.completedFuture(EmptyO.of()), x -> x,
                                (r, ex) -> {
                                    throw new ExpectedFailure();
                                });
                        // formatter:on
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Object, IDatum, EmptyO, EmptyO> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testFailurePatch() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Object, IDatum, EmptyO, EmptyO>> future =
                run(".", new ITypeChecker<Scope, Object, IDatum, EmptyO, EmptyO>() {

                    @Override public IFuture<EmptyO> run(
                            IIncrementalTypeCheckerContext<Scope, Object, IDatum, EmptyO, EmptyO> unit,
                            List<Scope> roots) {
                        // formatter:off
                        return unit.<EmptyO>runIncremental(__ -> CompletableFuture.completedFuture(EmptyO.of()), x -> x,
                                (r, ptcs) -> {
                                    throw new ExpectedFailure();
                                }, CompletableFuture::completed);
                        // formatter:on
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Object, IDatum, EmptyO, EmptyO> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testNoRunResult() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Object, IDatum, EmptyO, EmptyO>> future =
                run(".", new ITypeChecker<Scope, Object, IDatum, EmptyO, EmptyO>() {

                    @Override public IFuture<EmptyO> run(
                            IIncrementalTypeCheckerContext<Scope, Object, IDatum, EmptyO, EmptyO> unit,
                            List<Scope> roots) {
                        return new CompletableFuture<>();
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Object, IDatum, EmptyO, EmptyO> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testExceptionalRunResult() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Object, IDatum, EmptyO, EmptyO>> future =
                run(".", new ITypeChecker<Scope, Object, IDatum, EmptyO, EmptyO>() {

                    @Override public IFuture<EmptyO> run(
                            IIncrementalTypeCheckerContext<Scope, Object, IDatum, EmptyO, EmptyO> unit,
                            List<Scope> roots) {
                        return CompletableFuture.completedExceptionally(new ExpectedFailure());
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Object, IDatum, EmptyO, EmptyO> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testExceptionalChildRunResult() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Object, IDatum, EmptyO, EmptyO>> future =
                run(".", new ITypeChecker<Scope, Object, IDatum, EmptyO, EmptyO>() {

                    @Override public IFuture<EmptyO> run(
                            IIncrementalTypeCheckerContext<Scope, Object, IDatum, EmptyO, EmptyO> unit,
                            List<Scope> roots) {
                        return unit.add("sub", new ITypeChecker<Scope, Object, IDatum, EmptyO, EmptyO>() {

                            @Override public IFuture<EmptyO> run(
                                    IIncrementalTypeCheckerContext<Scope, Object, IDatum, EmptyO, EmptyO> unit,
                                    List<Scope> rootScopes) {
                                return CompletableFuture.completedExceptionally(new ExpectedFailure());
                            }
                        }, Arrays.asList()).thenApply(IUnitResult::analysis);
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Object, IDatum, EmptyO, EmptyO> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testExceptionalRunResult_NoClose()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Object, IDatum, EmptyO, EmptyO>> future =
                run(".", new ITypeChecker<Scope, Object, IDatum, EmptyO, EmptyO>() {

                    @Override public IFuture<EmptyO> run(
                            IIncrementalTypeCheckerContext<Scope, Object, IDatum, EmptyO, EmptyO> unit,
                            List<Scope> roots) {
                        final Scope s = unit.freshScope("s", Collections.emptySet(), true, false);
                        return CompletableFuture.completedExceptionally(new ExpectedFailure());
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Object, IDatum, EmptyO, EmptyO> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testFailureInQueryPredicate() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Object, IDatum, EmptyO, EmptyO>> future =
                run(".", new ITypeChecker<Scope, Object, IDatum, EmptyO, EmptyO>() {

                    @Override public IFuture<EmptyO> run(
                            IIncrementalTypeCheckerContext<Scope, Object, IDatum, EmptyO, EmptyO> unit,
                            List<Scope> roots) {
                        final Scope s = unit.freshScope("s", Collections.emptyList(), true, true);

                        final IFuture<?> subResult =
                                unit.add("sub", new ITypeChecker<Scope, Object, IDatum, EmptyO, EmptyO>() {

                                    @Override public IFuture<EmptyO> run(
                                            IIncrementalTypeCheckerContext<Scope, Object, IDatum, EmptyO, EmptyO> unit,
                                            List<Scope> rootScopes) {
                                        final Scope s = rootScopes.get(0);

                                        return unit.query(s, LabelWf.any(), LabelOrder.none(),
                                                new DataWf<Scope, Object, IDatum>() {
                                                    @Override public IFuture<Boolean> wf(IDatum d,
                                                            ITypeCheckerContext<Scope, Object, IDatum> context,
                                                            ICancel cancel) throws InterruptedException {
                                                        throw new ExpectedFailure();
                                                    }
                                                }, DataLeq.none()).thenApply(r -> null);
                                    }

                                }, Collections.singletonList(s));

                        unit.closeScope(s);

                        return subResult.handle((r, ex) -> EmptyO.of());
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Object, IDatum, EmptyO, EmptyO> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testExceptionalQueryPredicateResult()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Object, IDatum, EmptyO, EmptyO>> future =
                run(".", new ITypeChecker<Scope, Object, IDatum, EmptyO, EmptyO>() {

                    @Override public IFuture<EmptyO> run(
                            IIncrementalTypeCheckerContext<Scope, Object, IDatum, EmptyO, EmptyO> unit,
                            List<Scope> roots) {
                        final Scope s = unit.freshScope("s", Collections.emptyList(), true, true);

                        IFuture<?> subResult =
                                unit.add("sub", new ITypeChecker<Scope, Object, IDatum, EmptyO, EmptyO>() {

                                    @Override public IFuture<EmptyO> run(
                                            IIncrementalTypeCheckerContext<Scope, Object, IDatum, EmptyO, EmptyO> unit,
                                            List<Scope> rootScopes) {
                                        final Scope s = rootScopes.get(0);

                                        return unit.query(s, LabelWf.any(), LabelOrder.none(),
                                                new DataWf<Scope, Object, IDatum>() {
                                                    @Override public IFuture<Boolean> wf(IDatum d,
                                                            ITypeCheckerContext<Scope, Object, IDatum> context,
                                                            ICancel cancel) throws InterruptedException {
                                                        return CompletableFuture
                                                                .completedExceptionally(new ExpectedFailure());
                                                    }
                                                }, DataLeq.none()).thenApply(r -> null);
                                    }

                                }, Collections.singletonList(s));

                        unit.closeScope(s);

                        return subResult.handle((r, ex) -> EmptyO.of());
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Object, IDatum, EmptyO, EmptyO> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testNoQueryPredicateResult() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Object, IDatum, EmptyO, EmptyO>> future =
                run(".", new ITypeChecker<Scope, Object, IDatum, EmptyO, EmptyO>() {

                    @Override public IFuture<EmptyO> run(
                            IIncrementalTypeCheckerContext<Scope, Object, IDatum, EmptyO, EmptyO> unit,
                            List<Scope> roots) {
                        final Scope s = unit.freshScope("s", Collections.emptyList(), true, true);

                        IFuture<?> subResult =
                                unit.add("sub", new ITypeChecker<Scope, Object, IDatum, EmptyO, EmptyO>() {

                                    @Override public IFuture<EmptyO> run(
                                            IIncrementalTypeCheckerContext<Scope, Object, IDatum, EmptyO, EmptyO> unit,
                                            List<Scope> rootScopes) {
                                        final Scope s = rootScopes.get(0);

                                        return unit.query(s, LabelWf.any(), LabelOrder.none(),
                                                new DataWf<Scope, Object, IDatum>() {
                                                    @Override public IFuture<Boolean> wf(IDatum d,
                                                            ITypeCheckerContext<Scope, Object, IDatum> context,
                                                            ICancel cancel) throws InterruptedException {
                                                        return new CompletableFuture<>();
                                                    }
                                                }, DataLeq.none()).thenApply(r -> null);
                                    }

                                }, Collections.singletonList(s));

                        unit.closeScope(s);

                        return subResult.handle((r, ex) -> EmptyO.of());
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Object, IDatum, EmptyO, EmptyO> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testExceptionalCompletionInDWF()
            throws ExecutionException, InterruptedException {
        final Integer lbl1 = 1;
        final Integer lbl2 = 2;

        final IFuture<IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI>> future =
                run(".", new ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI>() {

                    @Override public IFuture<EmptyI> run(
                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit,
                            List<Scope> roots) {
                        final Scope s = unit.freshScope("s", Arrays.asList(), false, true);

                        final IFuture<?> subUnitResult =
                                unit.add("sub", new ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI>() {

                                    @Override public IFuture<EmptyI> run(
                                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit,
                                            List<Scope> rootScopes) {
                                        final Scope s1 = rootScopes.get(0);
                                        unit.initScope(s1, Arrays.asList(lbl1, lbl2), false);

                                        final Scope d = unit.freshScope("d", Arrays.asList(), true, false);
                                        unit.setDatum(d, d);

                                        unit.addEdge(s, lbl1, d);
                                        unit.closeEdge(s, lbl1);

                                        return unit.query(s1, LabelWf.any(), LabelOrder.none(), DataWf.any(),
                                                DataLeq.none()).thenApply(env -> EmptyI.of());
                                    }
                                }, Arrays.asList(s));

                        unit.closeScope(s);
                        return unit.query(s, new SingleStepLabelWf(lbl1), LabelOrder.none(), new QueryDataWf(s, lbl2),
                                DataLeq.none()).thenCompose(r -> subUnitResult).thenApply(__ -> EmptyI.of());

                    }

                }, CapsuleUtil.toSet(lbl1, lbl2));

        final IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testExceptionalCompletionShadow()
            throws ExecutionException, InterruptedException {
        final Integer lbl1 = 1;
        final Integer lbl2 = 2;
        final Integer lbl3 = 3;

        final IFuture<IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI>> future =
                run(".", new ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI>() {

                    @Override public IFuture<EmptyI> run(
                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit,
                            List<Scope> roots) {
                        final Scope s = unit.freshScope("s", Arrays.asList(), false, true);

                        final IFuture<?> subUnitResult =
                                unit.add("sub", new ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI>() {

                                    @Override public IFuture<EmptyI> run(
                                            IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit,
                                            List<Scope> rootScopes) {
                                        final Scope s = rootScopes.get(0);
                                        unit.initScope(s, Arrays.asList(lbl1, lbl2, lbl3), false);

                                        final Scope d1 = unit.freshScope("d", Arrays.asList(), true, false);
                                        unit.setDatum(d1, d1);

                                        final Scope d2 = unit.freshScope("d", Arrays.asList(), true, false);
                                        unit.setDatum(d2, d2);

                                        unit.addEdge(s, lbl1, d1);
                                        unit.addEdge(s, lbl2, d2);

                                        unit.closeEdge(s, lbl1);
                                        unit.closeEdge(s, lbl2);

                                        return unit.query(s, LabelWf.any(), LabelOrder.none(), DataWf.any(),
                                                DataLeq.none()).thenApply(env -> EmptyI.of());
                                    }
                                }, Arrays.asList(s));

                        unit.closeScope(s);
                        return unit
                                .query(s, LabelWf.any(), new IntegerLabelOrder(), DataWf.any(),
                                        new QueryDataLeq(s, lbl3))
                                .thenCompose(r -> subUnitResult).thenApply(__ -> EmptyI.of());

                    }

                }, CapsuleUtil.toSet(lbl1, lbl2, lbl3));

        final IUnitResult<Scope, Integer, IDatum, EmptyI, EmptyI> result = future.asJavaCompletion().get();
    }

    ///////////////////////////////////////////////////////////////////////////

    private final class ResolveBeforeCloseEdgeDatum implements ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI> {

        private final Integer ownLabel;
        private final List<Integer> queryLabels;

        public ResolveBeforeCloseEdgeDatum(Integer ownLabel, Integer... queryLabels) {
            this.ownLabel = ownLabel;
            this.queryLabels = ImmutableList.copyOf(queryLabels);
        }

        @Override public IFuture<EmptyI> run(
                IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit, List<Scope> rootScopes) {
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
            final IFuture<List<Object>> result = AggregateFuture.of(results).whenComplete((r, ex) -> {
                unit.closeScope(s1);
            });

            return result.handle((r, ex) -> EmptyI.of());
        }

    }

    private final class CloseEdgeBeforeResolveDatum implements ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI> {

        private final Integer ownLabel;
        private final List<Integer> queryLabels;

        public CloseEdgeBeforeResolveDatum(Integer ownLabel, Integer... queryLabels) {
            this.ownLabel = ownLabel;
            this.queryLabels = ImmutableList.copyOf(queryLabels);
        }

        @Override public IFuture<EmptyI> run(
                IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit, List<Scope> rootScopes) {
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
            final IFuture<List<Object>> result = AggregateFuture.of(results);

            return result.handle((r, ex) -> EmptyI.of());
        }

    }

    private final class ResolveBeforeSetDatumDatum implements ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI> {

        private final Integer ownLabel;
        private final List<Integer> queryLabels;

        public ResolveBeforeSetDatumDatum(Integer ownLabel, Integer... queryLabels) {
            this.ownLabel = ownLabel;
            this.queryLabels = ImmutableList.copyOf(queryLabels);
        }

        @Override public IFuture<EmptyI> run(
                IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit, List<Scope> rootScopes) {
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
            final IFuture<List<Object>> result = AggregateFuture.of(results).whenComplete((r, ex) -> {
                unit.setDatum(s1, s1);
            });

            return result.handle((r, ex) -> EmptyI.of());
        }

    }

    private final class SetDatumBeforeResolveDatum implements ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI> {

        private final Integer ownLabel;
        private final List<Integer> queryLabels;

        public SetDatumBeforeResolveDatum(Integer ownLabel, Integer... queryLabels) {
            this.ownLabel = ownLabel;
            this.queryLabels = ImmutableList.copyOf(queryLabels);
        }

        @Override public IFuture<EmptyI> run(
                IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit, List<Scope> rootScopes) {
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
            final IFuture<List<Object>> result = AggregateFuture.of(results);

            return result.handle((r, ex) -> EmptyI.of());
        }

    }

    private final class CloseBeforeResolveNoExternalRepDatum
            implements ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI> {

        private final Integer ownLabel;
        private final List<Integer> queryLabels;

        public CloseBeforeResolveNoExternalRepDatum(Integer ownLabel, Integer... queryLabels) {
            this.ownLabel = ownLabel;
            this.queryLabels = ImmutableList.copyOf(queryLabels);
        }

        @Override public IFuture<EmptyI> run(
                IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit, List<Scope> rootScopes) {
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
            final IFuture<List<Object>> result = AggregateFuture.of(results);

            return result.handle((r, ex) -> EmptyI.of());
        }

        @Override public IFuture<IDatum> getExternalDatum(IDatum datum) {
            return new CompletableFuture<>();
        }

    }

    private final class CloseBeforeResolveFailingExternalRepDatum
            implements ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI> {

        private final Integer ownLabel;
        private final List<Integer> queryLabels;

        public CloseBeforeResolveFailingExternalRepDatum(Integer ownLabel, Integer... queryLabels) {
            this.ownLabel = ownLabel;
            this.queryLabels = ImmutableList.copyOf(queryLabels);
        }

        @Override public IFuture<EmptyI> run(
                IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit, List<Scope> rootScopes) {
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
            final IFuture<List<Object>> result = AggregateFuture.of(results);

            return result.handle((r, ex) -> EmptyI.of());
        }

        @Override public IFuture<IDatum> getExternalDatum(IDatum datum) {
            throw new ExpectedFailure();
        }

    }

    private final class CloseBeforeResolveExceptionalExternalRepDatum
            implements ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI> {

        private final Integer ownLabel;
        private final List<Integer> queryLabels;

        public CloseBeforeResolveExceptionalExternalRepDatum(Integer ownLabel, Integer... queryLabels) {
            this.ownLabel = ownLabel;
            this.queryLabels = ImmutableList.copyOf(queryLabels);
        }

        @Override public IFuture<EmptyI> run(
                IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit, List<Scope> rootScopes) {
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
            final IFuture<List<Object>> result = AggregateFuture.of(results);

            return result.handle((r, ex) -> EmptyI.of());
        }

        @Override public IFuture<IDatum> getExternalDatum(IDatum datum) {
            return CompletableFuture.completedExceptionally(new ExpectedFailure());
        }

    }

    private final class CloseBeforeResolveNoDatumDatum implements ITypeChecker<Scope, Integer, IDatum, EmptyI, EmptyI> {

        private final Integer ownLabel;
        private final List<Integer> queryLabels;

        public CloseBeforeResolveNoDatumDatum(Integer ownLabel, Integer... queryLabels) {
            this.ownLabel = ownLabel;
            this.queryLabels = ImmutableList.copyOf(queryLabels);
        }

        @Override public IFuture<EmptyI> run(
                IIncrementalTypeCheckerContext<Scope, Integer, IDatum, EmptyI, EmptyI> unit, List<Scope> rootScopes) {
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
            final IFuture<List<Object>> result = AggregateFuture.of(results);

            return result.handle((r, ex) -> EmptyI.of());
        }

        @Override public IFuture<IDatum> getExternalDatum(IDatum datum) {
            return new CompletableFuture<>();
        }

    }

    ///////////////////////////////////////////////////////////////////////////

    private class IntegerLabelOrder implements LabelOrder<Integer> {

        @Override public boolean lt(EdgeOrData<Integer> lbl1, EdgeOrData<Integer> lbl2) {
            return lbl1.match(() -> lbl2.match(() -> false, __ -> true),
                    l1 -> lbl2.match(() -> false, l2 -> (l1 < l2)));
        }

    }

    private class SingleStepLabelWf implements LabelWf<Integer> {

        private Integer label;

        public SingleStepLabelWf(Integer label) {
            this.label = label;
        }

        @Override public Optional<LabelWf<Integer>> step(Integer l) {
            return l.equals(label) ? Optional.of(new EOPLabelWf()) : Optional.empty();
        }

        @Override public boolean accepting() {
            return false;
        }

        @Override public String toString() {
            return label.toString();
        }

    }

    private class EOPLabelWf implements LabelWf<Integer> {

        @Override public Optional<LabelWf<Integer>> step(Integer l) {
            return Optional.empty();
        }

        @Override public boolean accepting() {
            return true;
        }

        @Override public String toString() {
            return "$";
        }

    }

    private class QueryDataWf implements DataWf<Scope, Integer, IDatum> {

        private Scope scope;
        private Integer label;

        protected QueryDataWf(Scope scope, Integer label) {
            this.scope = scope;
            this.label = label;
        }

        @Override public IFuture<Boolean> wf(IDatum datum, ITypeCheckerContext<Scope, Integer, IDatum> context,
                ICancel cancel) throws InterruptedException {
            final RegExpBuilder<Integer> reb = new RegExpBuilder<>();
            final IRegExp<Integer> re = reb.and(reb.symbol(label), reb.complement(reb.emptySet()));
            final IRegExpMatcher<Integer> rem = RegExpMatcher.create(re);
            return context.query(scope, new RegExpLabelWf<Integer>(rem), LabelOrder.none(), new EqualsDataWf(datum),
                    DataLeq.none()).thenApply(env -> !env.isEmpty());
        }

        @Override public Immutable<Scope> scopes() {
            return CapsuleUtil.immutableSet(scope);
        }

        @Override public DataWf<Scope, Integer, IDatum> patch(IPatchCollection.Immutable<Scope> patches) {
            return new QueryDataWf(patches.patch(scope), label);
        }

    }

    private class QueryDataLeq implements DataLeq<Scope, Integer, IDatum> {

        private Scope scope;
        private Integer label;

        protected QueryDataLeq(Scope scope, Integer label) {
            this.scope = scope;
            this.label = label;
        }

        @Override public IFuture<Boolean> leq(IDatum d1, IDatum d2, ITypeCheckerContext<Scope, Integer, IDatum> context,
                ICancel cancel) throws InterruptedException {
            final RegExpBuilder<Integer> reb = new RegExpBuilder<>();
            final IRegExp<Integer> re = reb.and(reb.symbol(label), reb.complement(reb.emptySet()));
            final IRegExpMatcher<Integer> rem = RegExpMatcher.create(re);
            return context
                    .query(scope, new RegExpLabelWf<Integer>(rem), LabelOrder.none(),
                            new OrDataWf(new EqualsDataWf(d1), new EqualsDataWf(d2)), DataLeq.none())
                    .thenApply(env -> !env.isEmpty());
        }
    }

    private class EqualsDataWf implements DataWf<Scope, Integer, IDatum> {

        private final IDatum datum;

        public EqualsDataWf(IDatum datum) {
            this.datum = datum;
        }

        @Override public IFuture<Boolean> wf(IDatum d, ITypeCheckerContext<Scope, Integer, IDatum> context,
                ICancel cancel) throws InterruptedException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override public Immutable<Scope> scopes() {
            if(datum instanceof Scope) {
                return CapsuleUtil.immutableSet((Scope) datum);
            }
            return CapsuleUtil.immutableSet();
        }

        @Override public DataWf<Scope, Integer, IDatum> patch(IPatchCollection.Immutable<Scope> patches) {
            if(datum instanceof Scope) {
                return new EqualsDataWf(patches.patch((Scope) datum));
            }
            return this;
        }

    }

    private class OrDataWf implements DataWf<Scope, Integer, IDatum> {

        private final DataWf<Scope, Integer, IDatum> dwf1;

        private final DataWf<Scope, Integer, IDatum> dwf2;

        public OrDataWf(DataWf<Scope, Integer, IDatum> dwf1, DataWf<Scope, Integer, IDatum> dwf2) {
            this.dwf1 = dwf1;
            this.dwf2 = dwf2;
        }

        @Override public IFuture<Boolean> wf(IDatum d, ITypeCheckerContext<Scope, Integer, IDatum> context,
                ICancel cancel) throws InterruptedException {
            return AggregateFuture.apply(dwf1.wf(d, context, cancel), dwf2.wf(d, context, cancel))
                    .thenApply(values -> values._1() || values._2());
        }

        @Override public Immutable<Scope> scopes() {
            return dwf1.scopes().__insertAll(dwf2.scopes());
        }

        @Override public DataWf<Scope, Integer, IDatum> patch(IPatchCollection.Immutable<Scope> patches) {
            return new OrDataWf(dwf1.patch(patches), dwf2.patch(patches));
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    private static class ExpectedFailure extends RuntimeException {

    }

}
