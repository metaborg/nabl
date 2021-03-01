package mb.statix.concurrent.p_raffrayi;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.metaborg.util.unit.Unit;

import com.google.common.collect.ImmutableList;

import io.usethesource.capsule.Set;
import mb.nabl2.regexp.IRegExp;
import mb.nabl2.regexp.IRegExpMatcher;
import mb.nabl2.regexp.RegExpMatcher;
import mb.nabl2.regexp.impl.RegExpBuilder;
import mb.nabl2.terms.ITerm;
import static mb.nabl2.terms.build.TermBuild.B;
import mb.statix.concurrent.actors.futures.AggregateFuture;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.impl.IInitialState;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataLeq;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataWf;
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelOrder;
import mb.statix.concurrent.p_raffrayi.nameresolution.RegExpLabelWf;
import mb.statix.scopegraph.terms.Scope;

public class PRaffrayiTest extends PRaffrayiTestBase {

    ///////////////////////////////////////////////////////////////////////////


    @Test(timeout = 10000) public void testSingleNoop() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, ITerm, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, ITerm, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, ITerm, ITerm> unit,
                            List<Scope> roots, IInitialState<Scope, ITerm, ITerm, Object> initialState) {
                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, ITerm, ITerm, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testSingleOneScope() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, ITerm, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, ITerm, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, ITerm, ITerm> unit,
                            List<Scope> roots, IInitialState<Scope, ITerm, ITerm, Object> initialState) {
                        Scope s = unit.freshScope("s", Set.Immutable.of(B.newInt(1)), false, false);
                        unit.addEdge(s, B.newInt(1), s);
                        unit.closeEdge(s, B.newInt(1));
                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                }, Arrays.asList(B.newInt(1), B.newInt(2), B.newInt(3)));

        final IUnitResult<Scope, ITerm, ITerm, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testSingleOneScope_NoClose() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, ITerm, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, ITerm, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, ITerm, ITerm> unit,
                            List<Scope> roots, IInitialState<Scope, ITerm, ITerm, Object> initialState) {
                        Scope s = unit.freshScope("s", Set.Immutable.of(B.newInt(1)), false, false);
                        unit.addEdge(s, B.newInt(1), s);
                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, ITerm, ITerm, Object> result = future.asJavaCompletion().get();
        assertNotNull(result.analysis());
        assertFalse(result.failures().isEmpty());
    }


    @Test(timeout = 10000) public void testSingleParentChild() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, ITerm, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, ITerm, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, ITerm, ITerm> unit,
                            List<Scope> roots, IInitialState<Scope, ITerm, ITerm, Object> initialState) {
                        Scope s = unit.freshScope("s", Set.Immutable.of(), false, true);

                        IFuture<IUnitResult<Scope, ITerm, ITerm, Object>> subResult =
                                unit.add("sub", new ITypeChecker<Scope, ITerm, ITerm, Object>() {

                                    @Override public IFuture<Object>
                                            run(ITypeCheckerContext<Scope, ITerm, ITerm> unit, List<Scope> roots,
                                                        IInitialState<Scope, ITerm, ITerm, Object> initialState) {
                                        Scope root = roots.get(0);
                                        unit.initScope(root, Set.Immutable.of(), false);
                                        return CompletableFuture.completedFuture(Unit.unit);
                                    }

                                }, Arrays.asList(s));

                        unit.closeScope(s);

                        return subResult.thenCompose(r -> CompletableFuture.completedFuture(Unit.unit));
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, ITerm, ITerm, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testSingleParentChild_ParentNoClose()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, ITerm, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, ITerm, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, ITerm, ITerm> unit,
                            List<Scope> roots, IInitialState<Scope, ITerm, ITerm, Object> initialState) {
                        Scope s = unit.freshScope("s", Set.Immutable.of(), false, true);

                        IFuture<IUnitResult<Scope, ITerm, ITerm, Object>> subFuture =
                                unit.add("sub", new ITypeChecker<Scope, ITerm, ITerm, Object>() {

                                    @Override public IFuture<Object>
                                            run(ITypeCheckerContext<Scope, ITerm, ITerm> unit, List<Scope> roots,
                                                    IInitialState<Scope, ITerm, ITerm, Object> initialState) {
                                        Scope root = roots.get(0);
                                        unit.initScope(root, Set.Immutable.of(), false);
                                        return CompletableFuture.completedFuture(Unit.unit);
                                    }

                                }, Arrays.asList(s));

                        return subFuture.thenCompose(r -> CompletableFuture.completedFuture(Unit.unit));
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, ITerm, ITerm, Object> result = future.asJavaCompletion().get();

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
        final IFuture<IUnitResult<Scope, ITerm, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, ITerm, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, ITerm, ITerm> unit,
                            List<Scope> roots, IInitialState<Scope, ITerm, ITerm, Object> initialState) {
                        Scope s = unit.freshScope("s", Set.Immutable.of(), false, true);

                        unit.add("sub", new ITypeChecker<Scope, ITerm, ITerm, Object>() {

                            @Override public IFuture<Object> run(ITypeCheckerContext<Scope, ITerm, ITerm> unit,
                                    List<Scope> roots, IInitialState<Scope, ITerm, ITerm, Object> initialState) {
                                return CompletableFuture.completedFuture(Unit.unit);
                            }

                        }, Arrays.asList(s));

                        unit.closeScope(s);

                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, ITerm, ITerm, Object> result = future.asJavaCompletion().get();

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
        final IFuture<IUnitResult<Scope, ITerm, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, ITerm, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, ITerm, ITerm> unit,
                            List<Scope> roots, IInitialState<Scope, ITerm, ITerm, Object> initialState) {
                        Scope s = unit.freshScope("s", Set.Immutable.of(), false, true);

                        unit.add("sub", new ITypeChecker<Scope, ITerm, ITerm, Object>() {

                            @Override public IFuture<Object> run(ITypeCheckerContext<Scope, ITerm, ITerm> unit,
                                    List<Scope> roots, IInitialState<Scope, ITerm, ITerm, Object> initialState) {
                                Scope root = roots.get(0);
                                unit.initScope(root, Set.Immutable.of(B.newInt(1)), false);
                                return CompletableFuture.completedFuture(Unit.unit);
                            }

                        }, Arrays.asList(s));

                        unit.closeScope(s);

                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                }, Set.Immutable.of());

        final IUnitResult<Scope, ITerm, ITerm, Object> result = future.asJavaCompletion().get();

        //        assertEquals(2, result.unitResults().size());
    }


    @Test(timeout = 10000) public void testTwoUnitsCloseEdgeDeadlockCycle()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, ITerm, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, ITerm, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, ITerm, ITerm> unit,
                            List<Scope> roots, IInitialState<Scope, ITerm, ITerm, Object> initialState) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        unit.add("one", new ResolveBeforeCloseEdgeUnit(B.newInt(1), B.newInt(2)), Arrays.asList(s));
                        unit.add("two", new ResolveBeforeCloseEdgeUnit(B.newInt(2), B.newInt(1)), Arrays.asList(s));

                        unit.closeScope(s);

                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                }, Arrays.asList(B.newInt(1), B.newInt(2)));

        final IUnitResult<Scope, ITerm, ITerm, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testThreeUnitsCloseEdgeDeadlockCycle()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, ITerm, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, ITerm, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, ITerm, ITerm> unit,
                            List<Scope> roots, IInitialState<Scope, ITerm, ITerm, Object> initialState) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        unit.add("one", new ResolveBeforeCloseEdgeUnit(B.newInt(1), B.newInt(2)), Arrays.asList(s));
                        unit.add("two", new ResolveBeforeCloseEdgeUnit(B.newInt(2), B.newInt(3)), Arrays.asList(s));
                        unit.add("three", new ResolveBeforeCloseEdgeUnit(B.newInt(3), B.newInt(1)), Arrays.asList(s));

                        unit.closeScope(s);

                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                }, Arrays.asList(B.newInt(1), B.newInt(2), B.newInt(3)));

        final IUnitResult<Scope, ITerm, ITerm, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testTwoUnitsCloseEdgeCycle() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, ITerm, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, ITerm, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, ITerm, ITerm> unit,
                            List<Scope> roots, IInitialState<Scope, ITerm, ITerm, Object> initialState) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        unit.add("one", new CloseEdgeBeforeResolveUnit(B.newInt(1), B.newInt(2)), Arrays.asList(s));
                        unit.add("two", new CloseEdgeBeforeResolveUnit(B.newInt(2), B.newInt(1)), Arrays.asList(s));

                        unit.closeScope(s);

                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                }, Arrays.asList(B.newInt(1), B.newInt(2)));

        final IUnitResult<Scope, ITerm, ITerm, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testThreeUnitsCloseEdgeCycle() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, ITerm, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, ITerm, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, ITerm, ITerm> unit,
                            List<Scope> roots, IInitialState<Scope, ITerm, ITerm, Object> initialState) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        unit.add("one", new CloseEdgeBeforeResolveUnit(B.newInt(1), B.newInt(2)), Arrays.asList(s));
                        unit.add("two", new CloseEdgeBeforeResolveUnit(B.newInt(2), B.newInt(3)), Arrays.asList(s));
                        unit.add("three", new CloseEdgeBeforeResolveUnit(B.newInt(3), B.newInt(1)), Arrays.asList(s));

                        unit.closeScope(s);

                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                }, Arrays.asList(B.newInt(1), B.newInt(2), B.newInt(3)));

        final IUnitResult<Scope, ITerm, ITerm, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testUnitCloseEdgeAndResolve_TargetStuckOnExternalRepresentation()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, ITerm, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, ITerm, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, ITerm, ITerm> unit,
                            List<Scope> roots, IInitialState<Scope, ITerm, ITerm, Object> initialState) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        unit.add("one", new CloseEdgeBeforeResolveUnit(B.newInt(1), B.newInt(2)), Arrays.asList(s));
                        unit.add("two", new CloseBeforeResolveNoExternalRepUnit(B.newInt(2)), Arrays.asList(s));

                        unit.closeScope(s);

                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                }, Arrays.asList(B.newInt(1), B.newInt(2)));

        final IUnitResult<Scope, ITerm, ITerm, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testUnitResolveAndCloseEdge_TargetStuckOnExternalRepresentation()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, ITerm, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, ITerm, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, ITerm, ITerm> unit,
                            List<Scope> roots, IInitialState<Scope, ITerm, ITerm, Object> initialState) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        unit.add("one", new ResolveBeforeCloseEdgeUnit(B.newInt(1), B.newInt(2)), Arrays.asList(s));
                        unit.add("two", new CloseBeforeResolveNoExternalRepUnit(B.newInt(2)), Arrays.asList(s));

                        unit.closeScope(s);

                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                }, Arrays.asList(B.newInt(1), B.newInt(2)));

        final IUnitResult<Scope, ITerm, ITerm, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testTwoUnitsSetDatumDeadlockCycle()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, ITerm, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, ITerm, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, ITerm, ITerm> unit,
                            List<Scope> roots, IInitialState<Scope, ITerm, ITerm, Object> initialState) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        unit.add("one", new ResolveBeforeSetDatumUnit(B.newInt(1), B.newInt(2)), Arrays.asList(s));
                        unit.add("two", new ResolveBeforeSetDatumUnit(B.newInt(2), B.newInt(1)), Arrays.asList(s));

                        unit.closeScope(s);

                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                }, Arrays.asList(B.newInt(1), B.newInt(2)));

        final IUnitResult<Scope, ITerm, ITerm, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testThreeUnitsSetDatumDeadlockCycle()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, ITerm, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, ITerm, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, ITerm, ITerm> unit,
                            List<Scope> roots, IInitialState<Scope, ITerm, ITerm, Object> initialState) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        unit.add("one", new ResolveBeforeSetDatumUnit(B.newInt(1), B.newInt(2)), Arrays.asList(s));
                        unit.add("two", new ResolveBeforeSetDatumUnit(B.newInt(2), B.newInt(3)), Arrays.asList(s));
                        unit.add("three", new ResolveBeforeSetDatumUnit(B.newInt(3), B.newInt(1)), Arrays.asList(s));

                        unit.closeScope(s);

                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                }, Arrays.asList(B.newInt(1), B.newInt(2), B.newInt(3)));

        final IUnitResult<Scope, ITerm, ITerm, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testTwoUnitsSetDatumCycle() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, ITerm, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, ITerm, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, ITerm, ITerm> unit,
                            List<Scope> roots, IInitialState<Scope, ITerm, ITerm, Object> initialState) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        unit.add("one", new SetDatumBeforeResolveUnit(B.newInt(1), B.newInt(2)), Arrays.asList(s));
                        unit.add("two", new SetDatumBeforeResolveUnit(B.newInt(2), B.newInt(1)), Arrays.asList(s));

                        unit.closeScope(s);

                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                }, Arrays.asList(B.newInt(1), B.newInt(2)));

        final IUnitResult<Scope, ITerm, ITerm, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testThreeUnitsSetDatumCycle() throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, ITerm, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, ITerm, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, ITerm, ITerm> unit,
                            List<Scope> roots, IInitialState<Scope, ITerm, ITerm, Object> initialState) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        unit.add("one", new SetDatumBeforeResolveUnit(B.newInt(1), B.newInt(2)), Arrays.asList(s));
                        unit.add("two", new SetDatumBeforeResolveUnit(B.newInt(2), B.newInt(3)), Arrays.asList(s));
                        unit.add("three", new SetDatumBeforeResolveUnit(B.newInt(3), B.newInt(1)), Arrays.asList(s));

                        unit.closeScope(s);

                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                }, Arrays.asList(B.newInt(1), B.newInt(2), B.newInt(3)));

        final IUnitResult<Scope, ITerm, ITerm, Object> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testUnitSetDatumAndResolve_TargetStuckOnDatum()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, ITerm, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, ITerm, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, ITerm, ITerm> unit,
                            List<Scope> roots, IInitialState<Scope, ITerm, ITerm, Object> initialState) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        unit.add("one", new SetDatumBeforeResolveUnit(B.newInt(1), B.newInt(2)), Arrays.asList(s));
                        unit.add("two", new CloseBeforeResolveNoDatumUnit(B.newInt(2)), Arrays.asList(s));

                        unit.closeScope(s);

                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                }, Arrays.asList(B.newInt(1), B.newInt(2)));

        final IUnitResult<Scope, ITerm, ITerm, Object> result = future.asJavaCompletion().get();
    }


    @Test(timeout = 10000) public void testUnitResolveAndSetDatum_TargetStuckOnDatum()
            throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, ITerm, ITerm, Object>> future =
                run(".", new ITypeChecker<Scope, ITerm, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, ITerm, ITerm> unit,
                            List<Scope> roots, IInitialState<Scope, ITerm, ITerm, Object> initialState) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        unit.add("one", new ResolveBeforeSetDatumUnit(B.newInt(1), B.newInt(2)), Arrays.asList(s));
                        unit.add("two", new CloseBeforeResolveNoDatumUnit(B.newInt(2)), Arrays.asList(s));

                        unit.closeScope(s);

                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                }, Arrays.asList(B.newInt(1), B.newInt(2)));

        final IUnitResult<Scope, ITerm, ITerm, Object> result = future.asJavaCompletion().get();
    }


    ///////////////////////////////////////////////////////////////////////////

    private final class ResolveBeforeCloseEdgeUnit implements ITypeChecker<Scope, ITerm, ITerm, Object> {

        private final ITerm ownLabel;
        private final List<ITerm> queryLabels;

        public ResolveBeforeCloseEdgeUnit(ITerm ownLabel, ITerm... queryLabels) {
            this.ownLabel = ownLabel;
            this.queryLabels = ImmutableList.copyOf(queryLabels);
        }

        @Override public IFuture<Object> run(ITypeCheckerContext<Scope, ITerm, ITerm> unit, List<Scope> rootScopes,
                IInitialState<Scope, ITerm, ITerm, Object> initialState) {
            Scope s = rootScopes.get(0);
            unit.initScope(s, Set.Immutable.of(ownLabel), false);

            Scope s1 = unit.freshScope("s'", Collections.emptySet(), false, true);

            unit.addEdge(s, ownLabel, s1);
            unit.closeEdge(s, ownLabel);

            final List<IFuture<?>> results = new ArrayList<>();
            for(ITerm queryLabel : queryLabels) {
                final RegExpBuilder<ITerm> reb = new RegExpBuilder<>();
                final IRegExp<ITerm> re = reb.and(reb.symbol(queryLabel), reb.complement(reb.emptySet()));
                final IRegExpMatcher<ITerm> rem = RegExpMatcher.create(re);
                results.add(unit.query(s, new RegExpLabelWf<>(rem), LabelOrder.none(), DataWf.any(), DataLeq.none()));
            }
            new AggregateFuture<>(results).whenComplete((r, ex) -> {
                unit.closeScope(s1);
            });

            return CompletableFuture.completedFuture(Unit.unit);
        }

    }

    private final class CloseEdgeBeforeResolveUnit implements ITypeChecker<Scope, ITerm, ITerm, Object> {

        private final ITerm ownLabel;
        private final List<ITerm> queryLabels;

        public CloseEdgeBeforeResolveUnit(ITerm ownLabel, ITerm... queryLabels) {
            this.ownLabel = ownLabel;
            this.queryLabels = ImmutableList.copyOf(queryLabels);
        }

        @Override public IFuture<Object> run(ITypeCheckerContext<Scope, ITerm, ITerm> unit, List<Scope> rootScopes,
                IInitialState<Scope, ITerm, ITerm, Object> initialState) {
            Scope s = rootScopes.get(0);
            unit.initScope(s, Set.Immutable.of(ownLabel), false);

            Scope s1 = unit.freshScope("s'", Collections.emptySet(), false, true);

            unit.addEdge(s, ownLabel, s1);
            unit.closeEdge(s, ownLabel);

            unit.closeScope(s1);

            for(ITerm queryLabel : queryLabels) {
                final RegExpBuilder<ITerm> reb = new RegExpBuilder<>();
                final IRegExp<ITerm> re = reb.and(reb.symbol(queryLabel), reb.complement(reb.emptySet()));
                final IRegExpMatcher<ITerm> rem = RegExpMatcher.create(re);
                unit.query(s, new RegExpLabelWf<>(rem), LabelOrder.none(), DataWf.any(), DataLeq.none());
            }

            return CompletableFuture.completedFuture(Unit.unit);
        }

    }

    private final class ResolveBeforeSetDatumUnit implements ITypeChecker<Scope, ITerm, ITerm, Object> {

        private final ITerm ownLabel;
        private final List<ITerm> queryLabels;

        public ResolveBeforeSetDatumUnit(ITerm ownLabel, ITerm... queryLabels) {
            this.ownLabel = ownLabel;
            this.queryLabels = ImmutableList.copyOf(queryLabels);
        }

        @Override public IFuture<Object> run(ITypeCheckerContext<Scope, ITerm, ITerm> unit, List<Scope> rootScopes,
                IInitialState<Scope, ITerm, ITerm, Object> initialState) {
            Scope s = rootScopes.get(0);
            unit.initScope(s, Set.Immutable.of(ownLabel), false);

            Scope s1 = unit.freshScope("s'", Collections.emptySet(), true, false);

            unit.addEdge(s, ownLabel, s1);
            unit.closeEdge(s, ownLabel);

            final List<IFuture<?>> results = new ArrayList<>();
            for(ITerm queryLabel : queryLabels) {
                final RegExpBuilder<ITerm> reb = new RegExpBuilder<>();
                final IRegExp<ITerm> re = reb.and(reb.symbol(queryLabel), reb.complement(reb.emptySet()));
                final IRegExpMatcher<ITerm> rem = RegExpMatcher.create(re);
                results.add(unit.query(s, new RegExpLabelWf<>(rem), LabelOrder.none(), DataWf.any(), DataLeq.none()));
            }
            new AggregateFuture<>(results).whenComplete((r, ex) -> {
                unit.setDatum(s1, s1);
            });

            return CompletableFuture.completedFuture(Unit.unit);
        }

    }

    private final class SetDatumBeforeResolveUnit implements ITypeChecker<Scope, ITerm, ITerm, Object> {

        private final ITerm ownLabel;
        private final List<ITerm> queryLabels;

        public SetDatumBeforeResolveUnit(ITerm ownLabel, ITerm... queryLabels) {
            this.ownLabel = ownLabel;
            this.queryLabels = ImmutableList.copyOf(queryLabels);
        }

        @Override public IFuture<Object> run(ITypeCheckerContext<Scope, ITerm, ITerm> unit, List<Scope> rootScopes,
                IInitialState<Scope, ITerm, ITerm, Object> initialState) {
            Scope s = rootScopes.get(0);
            unit.initScope(s, Set.Immutable.of(ownLabel), false);

            Scope s1 = unit.freshScope("s'", Collections.emptySet(), true, false);

            unit.addEdge(s, ownLabel, s1);
            unit.closeEdge(s, ownLabel);

            unit.setDatum(s1, s1);

            for(ITerm queryLabel : queryLabels) {
                final RegExpBuilder<ITerm> reb = new RegExpBuilder<>();
                final IRegExp<ITerm> re = reb.and(reb.symbol(queryLabel), reb.complement(reb.emptySet()));
                final IRegExpMatcher<ITerm> rem = RegExpMatcher.create(re);
                unit.query(s, new RegExpLabelWf<>(rem), LabelOrder.none(), DataWf.any(), DataLeq.none());
            }

            return CompletableFuture.completedFuture(Unit.unit);
        }

    }

    private final class CloseBeforeResolveNoExternalRepUnit implements ITypeChecker<Scope, ITerm, ITerm, Object> {

        private final ITerm ownLabel;
        private final List<ITerm> queryLabels;

        public CloseBeforeResolveNoExternalRepUnit(ITerm ownLabel, ITerm... queryLabels) {
            this.ownLabel = ownLabel;
            this.queryLabels = ImmutableList.copyOf(queryLabels);
        }

        @Override public IFuture<Object> run(ITypeCheckerContext<Scope, ITerm, ITerm> unit, List<Scope> rootScopes,
                IInitialState<Scope, ITerm, ITerm, Object> initialState) {
            Scope s = rootScopes.get(0);
            unit.initScope(s, Set.Immutable.of(ownLabel), false);

            Scope s1 = unit.freshScope("s'", Collections.emptySet(), true, false);

            unit.addEdge(s, ownLabel, s1);
            unit.closeEdge(s, ownLabel);

            unit.setDatum(s1, s1);

            for(ITerm queryLabel : queryLabels) {
                final RegExpBuilder<ITerm> reb = new RegExpBuilder<>();
                final IRegExp<ITerm> re = reb.and(reb.symbol(queryLabel), reb.complement(reb.emptySet()));
                final IRegExpMatcher<ITerm> rem = RegExpMatcher.create(re);
                unit.query(s, new RegExpLabelWf<>(rem), LabelOrder.none(), DataWf.any(), DataLeq.none());
            }

            return CompletableFuture.completedFuture(Unit.unit);
        }

        @Override public IFuture<ITerm> getExternalDatum(ITerm datum) {
            return new CompletableFuture<>();
        }

    }

    private final class CloseBeforeResolveNoDatumUnit implements ITypeChecker<Scope, ITerm, ITerm, Object> {

        private final ITerm ownLabel;
        private final List<ITerm> queryLabels;

        public CloseBeforeResolveNoDatumUnit(ITerm ownLabel, ITerm... queryLabels) {
            this.ownLabel = ownLabel;
            this.queryLabels = ImmutableList.copyOf(queryLabels);
        }

        @Override public IFuture<Object> run(ITypeCheckerContext<Scope, ITerm, ITerm> unit, List<Scope> rootScopes,
                IInitialState<Scope, ITerm, ITerm, Object> initialState) {
            Scope s = rootScopes.get(0);
            unit.initScope(s, Set.Immutable.of(ownLabel), false);

            Scope s1 = unit.freshScope("s'", Collections.emptySet(), true, false);

            unit.addEdge(s, ownLabel, s1);
            unit.closeEdge(s, ownLabel);

            for(ITerm queryLabel : queryLabels) {
                final RegExpBuilder<ITerm> reb = new RegExpBuilder<>();
                final IRegExp<ITerm> re = reb.and(reb.symbol(queryLabel), reb.complement(reb.emptySet()));
                final IRegExpMatcher<ITerm> rem = RegExpMatcher.create(re);
                unit.query(s, new RegExpLabelWf<>(rem), LabelOrder.none(), DataWf.any(), DataLeq.none());
            }

            return CompletableFuture.completedFuture(Unit.unit);
        }

        @Override public IFuture<ITerm> getExternalDatum(ITerm datum) {
            return new CompletableFuture<>();
        }

    }

}