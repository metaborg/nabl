package mb.statix.concurrent.p_raffrayi;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.Ignore;
import org.junit.Test;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.NullCancel;
import org.metaborg.util.unit.Unit;
import org.spoofax.terms.util.NotImplementedException;

import com.google.common.collect.ImmutableList;

import io.usethesource.capsule.Set;
import mb.nabl2.regexp.IRegExp;
import mb.nabl2.regexp.IRegExpMatcher;
import mb.nabl2.regexp.RegExpMatcher;
import mb.nabl2.regexp.impl.RegExpBuilder;
import mb.nabl2.terms.ITerm;
import mb.statix.concurrent.actors.futures.AggregateFuture;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.impl.Broker;
import mb.statix.concurrent.p_raffrayi.impl.ScopeImpl;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataLeq;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataWf;
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelOrder;
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelWf;
import mb.statix.concurrent.p_raffrayi.nameresolution.RegExpLabelWf;
import mb.statix.scopegraph.Scope;

public class PRaffrayiTest {

    private final IScopeImpl<Scope, ITerm> scopeImpl = new ScopeImpl();

    private <L, R> IFuture<IUnitResult<Scope, L, ITerm, R>> run(String id, ITypeChecker<Scope, L, ITerm, R> typeChecker,
            Iterable<L> edgeLabels) {
        return Broker.debug(id, typeChecker, scopeImpl, edgeLabels, new NullCancel(), 0.3, 50);
        //return Broker.run(id, typeChecker, scopeImpl, edgeLabels, new NullCancel());
    }

    ///////////////////////////////////////////////////////////////////////////


    @Test(timeout = 10000) public void testSingleNoop() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Object, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, Object, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Object, ITerm> unit,
                            List<Scope> roots) {
                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Object, ITerm, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testSingleOneScope() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Set.Immutable.of(1), false, false);
                        unit.addEdge(s, 1, s);
                        unit.closeEdge(s, 1);
                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Integer, ITerm, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testSingleOneScope_NoClose() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Set.Immutable.of(1), false, false);
                        unit.addEdge(s, 1, s);
                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Integer, ITerm, Object> result = future.asJavaCompletion().get();
        assertNotNull(result.analysis());
        assertFalse(result.failures().isEmpty());
    }


    @Test(timeout = 10000) public void testSingleParentChild() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Set.Immutable.of(), false, true);

                        IFuture<IUnitResult<Scope, Integer, ITerm, Object>> subResult =
                                unit.add("sub", new ITypeChecker<Scope, Integer, ITerm, Object>() {

                                    @Override public IFuture<Object>
                                            run(ITypeCheckerContext<Scope, Integer, ITerm> unit, List<Scope> roots) {
                                        Scope root = roots.get(0);
                                        unit.initScope(root, Set.Immutable.of(), false);
                                        return CompletableFuture.completedFuture(Unit.unit);
                                    }

                                }, Arrays.asList(s));

                        unit.closeScope(s);

                        return subResult.thenCompose(r -> CompletableFuture.completedFuture(Unit.unit));
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Integer, ITerm, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testSingleParentChild_ParentNoClose()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Set.Immutable.of(), false, true);

                        IFuture<IUnitResult<Scope, Integer, ITerm, Object>> subFuture =
                                unit.add("sub", new ITypeChecker<Scope, Integer, ITerm, Object>() {

                                    @Override public IFuture<Object>
                                            run(ITypeCheckerContext<Scope, Integer, ITerm> unit, List<Scope> roots) {
                                        Scope root = roots.get(0);
                                        unit.initScope(root, Set.Immutable.of(), false);
                                        return CompletableFuture.completedFuture(Unit.unit);
                                    }

                                }, Arrays.asList(s));

                        return subFuture.thenCompose(r -> CompletableFuture.completedFuture(Unit.unit));
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Integer, ITerm, Object> result = future.asJavaCompletion().get();

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
        final IFuture<IUnitResult<Scope, Integer, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Set.Immutable.of(), false, true);

                        final IFuture<?> subResult = unit.add("sub", new ITypeChecker<Scope, Integer, ITerm, Object>() {

                            @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit,
                                    List<Scope> roots) {
                                return CompletableFuture.completedFuture(Unit.unit);
                            }

                        }, Arrays.asList(s));

                        unit.closeScope(s);

                        return subResult.handle((r, ex) -> Unit.unit);
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Integer, ITerm, Object> result = future.asJavaCompletion().get();

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
        final IFuture<IUnitResult<Scope, Integer, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Set.Immutable.of(), false, true);

                        final IFuture<?> subResult = unit.add("sub", new ITypeChecker<Scope, Integer, ITerm, Object>() {

                            @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit,
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

        final IUnitResult<Scope, Integer, ITerm, Object> result = future.asJavaCompletion().get();

        //        assertEquals(2, result.unitResults().size());
    }


    @Test(timeout = 10000) public void testTwoUnitsCloseEdgeDeadlockCycle()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new ResolveBeforeCloseEdgeUnit(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new ResolveBeforeCloseEdgeUnit(2, 1), Arrays.asList(s)));

                        unit.closeScope(s);

                        return new AggregateFuture<>(subResults).handle((r, ex) -> Unit.unit);
                    }

                }, Arrays.asList(1, 2));

        final IUnitResult<Scope, Integer, ITerm, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testThreeUnitsCloseEdgeDeadlockCycle()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new ResolveBeforeCloseEdgeUnit(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new ResolveBeforeCloseEdgeUnit(2, 3), Arrays.asList(s)));
                        subResults.add(unit.add("three", new ResolveBeforeCloseEdgeUnit(3, 1), Arrays.asList(s)));

                        unit.closeScope(s);

                        return new AggregateFuture<>(subResults).handle((r, ex) -> Unit.unit);
                    }

                }, Arrays.asList(1, 2, 3));

        final IUnitResult<Scope, Integer, ITerm, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testTwoUnitsCloseEdgeCycle() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new CloseEdgeBeforeResolveUnit(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new CloseEdgeBeforeResolveUnit(2, 1), Arrays.asList(s)));

                        unit.closeScope(s);

                        return new AggregateFuture<>(subResults).handle((r, ex) -> Unit.unit);
                    }

                }, Arrays.asList(1, 2));

        final IUnitResult<Scope, Integer, ITerm, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testThreeUnitsCloseEdgeCycle() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new CloseEdgeBeforeResolveUnit(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new CloseEdgeBeforeResolveUnit(2, 3), Arrays.asList(s)));
                        subResults.add(unit.add("three", new CloseEdgeBeforeResolveUnit(3, 1), Arrays.asList(s)));

                        unit.closeScope(s);

                        return new AggregateFuture<>(subResults).handle((r, ex) -> Unit.unit);
                    }

                }, Arrays.asList(1, 2, 3));

        final IUnitResult<Scope, Integer, ITerm, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testUnitCloseEdgeAndResolve_TargetStuckOnExternalRepresentation()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new CloseEdgeBeforeResolveUnit(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new CloseBeforeResolveNoExternalRepUnit(2), Arrays.asList(s)));

                        unit.closeScope(s);

                        return new AggregateFuture<>(subResults).handle((r, ex) -> Unit.unit);
                    }

                }, Arrays.asList(1, 2));

        final IUnitResult<Scope, Integer, ITerm, Object> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testUnitCloseEdgeAndResolve_TargetStuckOnFailingExternalRepresentation()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new CloseEdgeBeforeResolveUnit(1, 2), Arrays.asList(s)));
                        subResults.add(
                                unit.add("two", new CloseBeforeResolveFailingExternalRepUnit(2), Arrays.asList(s)));

                        unit.closeScope(s);

                        return new AggregateFuture<>(subResults).handle((r, ex) -> Unit.unit);
                    }

                }, Arrays.asList(1, 2));

        final IUnitResult<Scope, Integer, ITerm, Object> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testUnitCloseEdgeAndResolve_TargetStuckOnExceptionalExternalRepresentation()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new CloseEdgeBeforeResolveUnit(1, 2), Arrays.asList(s)));
                        subResults.add(
                                unit.add("two", new CloseBeforeResolveExceptionalExternalRepUnit(2), Arrays.asList(s)));

                        unit.closeScope(s);

                        return new AggregateFuture<>(subResults).handle((r, ex) -> Unit.unit);
                    }

                }, Arrays.asList(1, 2));

        final IUnitResult<Scope, Integer, ITerm, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testUnitResolveAndCloseEdge_TargetStuckOnExternalRepresentation()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new ResolveBeforeCloseEdgeUnit(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new CloseBeforeResolveNoExternalRepUnit(2), Arrays.asList(s)));

                        unit.closeScope(s);

                        return new AggregateFuture<>(subResults).handle((r, ex) -> Unit.unit);
                    }

                }, Arrays.asList(1, 2));

        final IUnitResult<Scope, Integer, ITerm, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testTwoUnitsSetDatumDeadlockCycle()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new ResolveBeforeSetDatumUnit(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new ResolveBeforeSetDatumUnit(2, 1), Arrays.asList(s)));

                        unit.closeScope(s);

                        return new AggregateFuture<>(subResults).handle((r, ex) -> Unit.unit);
                    }

                }, Arrays.asList(1, 2));

        final IUnitResult<Scope, Integer, ITerm, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testThreeUnitsSetDatumDeadlockCycle()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new ResolveBeforeSetDatumUnit(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new ResolveBeforeSetDatumUnit(2, 3), Arrays.asList(s)));
                        subResults.add(unit.add("three", new ResolveBeforeSetDatumUnit(3, 1), Arrays.asList(s)));

                        unit.closeScope(s);

                        return new AggregateFuture<>(subResults).handle((r, ex) -> Unit.unit);
                    }

                }, Arrays.asList(1, 2, 3));

        final IUnitResult<Scope, Integer, ITerm, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testTwoUnitsSetDatumCycle() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new SetDatumBeforeResolveUnit(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new SetDatumBeforeResolveUnit(2, 1), Arrays.asList(s)));

                        unit.closeScope(s);

                        return new AggregateFuture<>(subResults).handle((r, ex) -> Unit.unit);
                    }

                }, Arrays.asList(1, 2));

        final IUnitResult<Scope, Integer, ITerm, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testThreeUnitsSetDatumCycle() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new SetDatumBeforeResolveUnit(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new SetDatumBeforeResolveUnit(2, 3), Arrays.asList(s)));
                        subResults.add(unit.add("three", new SetDatumBeforeResolveUnit(3, 1), Arrays.asList(s)));

                        unit.closeScope(s);

                        return new AggregateFuture<>(subResults).handle((r, ex) -> Unit.unit);
                    }

                }, Arrays.asList(1, 2, 3));

        final IUnitResult<Scope, Integer, ITerm, Object> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testUnitSetDatumAndResolve_TargetStuckOnDatum()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new SetDatumBeforeResolveUnit(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new CloseBeforeResolveNoDatumUnit(2), Arrays.asList(s)));

                        unit.closeScope(s);

                        return new AggregateFuture<>(subResults).handle((r, ex) -> Unit.unit);
                    }

                }, Arrays.asList(1, 2));

        final IUnitResult<Scope, Integer, ITerm, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testUnitResolveAndSetDatum_TargetStuckOnDatum()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, Integer, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        List<IFuture<?>> subResults = new ArrayList<>();
                        subResults.add(unit.add("one", new ResolveBeforeSetDatumUnit(1, 2), Arrays.asList(s)));
                        subResults.add(unit.add("two", new CloseBeforeResolveNoDatumUnit(2), Arrays.asList(s)));

                        unit.closeScope(s);

                        return new AggregateFuture<>(subResults).handle((r, ex) -> Unit.unit);
                    }

                }, Arrays.asList(1, 2));

        final IUnitResult<Scope, Integer, ITerm, Object> result = future.asJavaCompletion().get();
    }

    @Ignore @Test(timeout = 10000) public void testFailureInRun() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Object, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, Object, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Object, ITerm> unit,
                            List<Scope> roots) {
                        throw new NotImplementedException();
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Object, ITerm, Object> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testNoRunResult() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Object, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, Object, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Object, ITerm> unit,
                            List<Scope> roots) {
                        return new CompletableFuture<>();
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Object, ITerm, Object> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testExceptionalRunResult() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Object, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, Object, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Object, ITerm> unit,
                            List<Scope> roots) {
                        return CompletableFuture.completedExceptionally(new NotImplementedException());
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Object, ITerm, Object> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testExceptionalRunResult_NoClose()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Object, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, Object, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Object, ITerm> unit,
                            List<Scope> roots) {
                        final Scope s = unit.freshScope("s", Collections.emptySet(), true, false);
                        return CompletableFuture.completedExceptionally(new NotImplementedException());
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Object, ITerm, Object> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testFailureInQueryPredicate() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Object, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, Object, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Object, ITerm> unit,
                            List<Scope> roots) {
                        final Scope s = unit.freshScope("s", Collections.emptyList(), true, true);

                        final IFuture<?> subResult = unit.add("sub", new ITypeChecker<Scope, Object, ITerm, Object>() {

                            @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Object, ITerm> unit,
                                    List<Scope> rootScopes) {
                                final Scope s = rootScopes.get(0);

                                return unit
                                        .query(s, LabelWf.any(), LabelOrder.none(), new DataWf<Scope, Object, ITerm>() {
                                            @Override public IFuture<Boolean> wf(ITerm d,
                                                    ITypeCheckerContext<Scope, Object, ITerm> context, ICancel cancel)
                                                    throws InterruptedException {
                                                throw new NotImplementedException();
                                            }
                                        }, DataLeq.none()).thenApply(r -> null);
                            }

                        }, Collections.singletonList(s));

                        unit.closeScope(s);

                        return subResult.handle((r, ex) -> Unit.unit);
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Object, ITerm, Object> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testExceptionalQueryPredicateResult()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Object, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, Object, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Object, ITerm> unit,
                            List<Scope> roots) {
                        final Scope s = unit.freshScope("s", Collections.emptyList(), true, true);

                        IFuture<?> subResult = unit.add("sub", new ITypeChecker<Scope, Object, ITerm, Object>() {

                            @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Object, ITerm> unit,
                                    List<Scope> rootScopes) {
                                final Scope s = rootScopes.get(0);

                                return unit
                                        .query(s, LabelWf.any(), LabelOrder.none(), new DataWf<Scope, Object, ITerm>() {
                                            @Override public IFuture<Boolean> wf(ITerm d,
                                                    ITypeCheckerContext<Scope, Object, ITerm> context, ICancel cancel)
                                                    throws InterruptedException {
                                                return CompletableFuture
                                                        .completedExceptionally(new NotImplementedException());
                                            }
                                        }, DataLeq.none()).thenApply(r -> null);
                            }

                        }, Collections.singletonList(s));

                        unit.closeScope(s);

                        return subResult.handle((r, ex) -> Unit.unit);
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, Object, ITerm, Object> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testNoQueryPredicateResult() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Object, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, Object, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Object, ITerm> unit,
                            List<Scope> roots) {
                        final Scope s = unit.freshScope("s", Collections.emptyList(), true, true);

                        IFuture<?> subResult = unit.add("sub", new ITypeChecker<Scope, Object, ITerm, Object>() {

                            @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Object, ITerm> unit,
                                    List<Scope> rootScopes) {
                                final Scope s = rootScopes.get(0);

                                return unit
                                        .query(s, LabelWf.any(), LabelOrder.none(), new DataWf<Scope, Object, ITerm>() {
                                            @Override public IFuture<Boolean> wf(ITerm d,
                                                    ITypeCheckerContext<Scope, Object, ITerm> context, ICancel cancel)
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

        final IUnitResult<Scope, Object, ITerm, Object> result = future.asJavaCompletion().get();
    }

    ///////////////////////////////////////////////////////////////////////////

    private final class ResolveBeforeCloseEdgeUnit implements ITypeChecker<Scope, Integer, ITerm, Object> {

        private final Integer ownLabel;
        private final List<Integer> queryLabels;

        public ResolveBeforeCloseEdgeUnit(Integer ownLabel, Integer... queryLabels) {
            this.ownLabel = ownLabel;
            this.queryLabels = ImmutableList.copyOf(queryLabels);
        }

        @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit, List<Scope> rootScopes) {
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

    private final class CloseEdgeBeforeResolveUnit implements ITypeChecker<Scope, Integer, ITerm, Object> {

        private final Integer ownLabel;
        private final List<Integer> queryLabels;

        public CloseEdgeBeforeResolveUnit(Integer ownLabel, Integer... queryLabels) {
            this.ownLabel = ownLabel;
            this.queryLabels = ImmutableList.copyOf(queryLabels);
        }

        @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit, List<Scope> rootScopes) {
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

    private final class ResolveBeforeSetDatumUnit implements ITypeChecker<Scope, Integer, ITerm, Object> {

        private final Integer ownLabel;
        private final List<Integer> queryLabels;

        public ResolveBeforeSetDatumUnit(Integer ownLabel, Integer... queryLabels) {
            this.ownLabel = ownLabel;
            this.queryLabels = ImmutableList.copyOf(queryLabels);
        }

        @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit, List<Scope> rootScopes) {
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

    private final class SetDatumBeforeResolveUnit implements ITypeChecker<Scope, Integer, ITerm, Object> {

        private final Integer ownLabel;
        private final List<Integer> queryLabels;

        public SetDatumBeforeResolveUnit(Integer ownLabel, Integer... queryLabels) {
            this.ownLabel = ownLabel;
            this.queryLabels = ImmutableList.copyOf(queryLabels);
        }

        @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit, List<Scope> rootScopes) {
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

    private final class CloseBeforeResolveNoExternalRepUnit implements ITypeChecker<Scope, Integer, ITerm, Object> {

        private final Integer ownLabel;
        private final List<Integer> queryLabels;

        public CloseBeforeResolveNoExternalRepUnit(Integer ownLabel, Integer... queryLabels) {
            this.ownLabel = ownLabel;
            this.queryLabels = ImmutableList.copyOf(queryLabels);
        }

        @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit, List<Scope> rootScopes) {
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

        @Override public IFuture<ITerm> getExternalDatum(ITerm datum) {
            return new CompletableFuture<>();
        }

    }

    private final class CloseBeforeResolveFailingExternalRepUnit
            implements ITypeChecker<Scope, Integer, ITerm, Object> {

        private final Integer ownLabel;
        private final List<Integer> queryLabels;

        public CloseBeforeResolveFailingExternalRepUnit(Integer ownLabel, Integer... queryLabels) {
            this.ownLabel = ownLabel;
            this.queryLabels = ImmutableList.copyOf(queryLabels);
        }

        @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit, List<Scope> rootScopes) {
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

        @Override public IFuture<ITerm> getExternalDatum(ITerm datum) {
            throw new NotImplementedException();
        }

    }

    private final class CloseBeforeResolveExceptionalExternalRepUnit
            implements ITypeChecker<Scope, Integer, ITerm, Object> {

        private final Integer ownLabel;
        private final List<Integer> queryLabels;

        public CloseBeforeResolveExceptionalExternalRepUnit(Integer ownLabel, Integer... queryLabels) {
            this.ownLabel = ownLabel;
            this.queryLabels = ImmutableList.copyOf(queryLabels);
        }

        @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit, List<Scope> rootScopes) {
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

        @Override public IFuture<ITerm> getExternalDatum(ITerm datum) {
            return CompletableFuture.completedExceptionally(new NotImplementedException());
        }

    }

    private final class CloseBeforeResolveNoDatumUnit implements ITypeChecker<Scope, Integer, ITerm, Object> {

        private final Integer ownLabel;
        private final List<Integer> queryLabels;

        public CloseBeforeResolveNoDatumUnit(Integer ownLabel, Integer... queryLabels) {
            this.ownLabel = ownLabel;
            this.queryLabels = ImmutableList.copyOf(queryLabels);
        }

        @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit, List<Scope> rootScopes) {
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

        @Override public IFuture<ITerm> getExternalDatum(ITerm datum) {
            return new CompletableFuture<>();
        }

    }

}