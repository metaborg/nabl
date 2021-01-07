package mb.statix.concurrent.p_raffrayi;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.metaborg.util.task.NullCancel;
import org.metaborg.util.unit.Unit;

import io.usethesource.capsule.Set;
import mb.nabl2.regexp.IRegExp;
import mb.nabl2.regexp.IRegExpMatcher;
import mb.nabl2.regexp.RegExpMatcher;
import mb.nabl2.regexp.impl.RegExpBuilder;
import mb.nabl2.terms.ITerm;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.impl.Broker;
import mb.statix.concurrent.p_raffrayi.impl.ScopeImpl;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataLeq;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataWf;
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelOrder;
import mb.statix.concurrent.p_raffrayi.nameresolution.RegExpLabelWf;
import mb.statix.scopegraph.terms.Scope;

public class PRaffrayiTest {

    private final IScopeImpl<Scope, ITerm> scopeImpl = new ScopeImpl();

    @Test(timeout = 10000) public void testSingleNoop() throws ExecutionException, InterruptedException {
        final Broker<Scope, Object, ITerm> broker = new Broker<>(scopeImpl, Set.Immutable.of(), new NullCancel());

        final IFuture<IUnitResult<Scope, Object, ITerm, Object>> future =
                broker.add(".", new ITypeChecker<Scope, Object, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Object, ITerm> unit,
                            List<Scope> roots) {
                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                });

        final IUnitResult<Scope, Object, ITerm, Object> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testSingleOneScope() throws ExecutionException, InterruptedException {
        final Broker<Scope, Integer, ITerm> broker = new Broker<>(scopeImpl, Set.Immutable.of(), new NullCancel());

        final IFuture<IUnitResult<Scope, Integer, ITerm, Object>> future =
                broker.add(".", new ITypeChecker<Scope, Integer, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Set.Immutable.of(1), false, false);
                        unit.addEdge(s, 1, s);
                        unit.closeEdge(s, 1);
                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                });

        final IUnitResult<Scope, Integer, ITerm, Object> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testSingleOneScope_NoClose() throws ExecutionException, InterruptedException {
        final Broker<Scope, Integer, ITerm> broker = new Broker<>(scopeImpl, Set.Immutable.of(), new NullCancel());

        final IFuture<IUnitResult<Scope, Integer, ITerm, Object>> future =
                broker.add(".", new ITypeChecker<Scope, Integer, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Set.Immutable.of(1), false, false);
                        unit.addEdge(s, 1, s);
                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                });

        final IUnitResult<Scope, Integer, ITerm, Object> result = future.asJavaCompletion().get();
        assertNotNull(result.analysis());
        assertFalse(result.failures().isEmpty());
    }

    @Test(timeout = 10000) public void testSingleParentChild() throws ExecutionException, InterruptedException {
        final Broker<Scope, Integer, ITerm> broker = new Broker<>(scopeImpl, Set.Immutable.of(), new NullCancel());

        final IFuture<IUnitResult<Scope, Integer, ITerm, Object>> future =
                broker.add(".", new ITypeChecker<Scope, Integer, ITerm, Object>() {

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

                });

        final IUnitResult<Scope, Integer, ITerm, Object> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testSingleParentChild_ParentNoClose()
            throws ExecutionException, InterruptedException {
        final Broker<Scope, Integer, ITerm> broker = new Broker<>(scopeImpl, Set.Immutable.of(), new NullCancel());

        final IFuture<IUnitResult<Scope, Integer, ITerm, Object>> future =
                broker.add(".", new ITypeChecker<Scope, Integer, ITerm, Object>() {

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

                });

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
        final Broker<Scope, Integer, ITerm> broker = new Broker<>(scopeImpl, Set.Immutable.of(), new NullCancel());

        final IFuture<IUnitResult<Scope, Integer, ITerm, Object>> future =
                broker.add(".", new ITypeChecker<Scope, Integer, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Set.Immutable.of(), false, true);

                        unit.add("sub", new ITypeChecker<Scope, Integer, ITerm, Object>() {

                            @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit,
                                    List<Scope> roots) {
                                return CompletableFuture.completedFuture(Unit.unit);
                            }

                        }, Arrays.asList(s));

                        unit.closeScope(s);

                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                });

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
        final Broker<Scope, Integer, ITerm> broker = new Broker<>(scopeImpl, Set.Immutable.of(), new NullCancel());

        final IFuture<IUnitResult<Scope, Integer, ITerm, Object>> future =
                broker.add(".", new ITypeChecker<Scope, Integer, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Set.Immutable.of(), false, true);

                        unit.add("sub", new ITypeChecker<Scope, Integer, ITerm, Object>() {

                            @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit,
                                    List<Scope> roots) {
                                Scope root = roots.get(0);
                                unit.initScope(root, Set.Immutable.of(1), false);
                                return CompletableFuture.completedFuture(Unit.unit);
                            }

                        }, Arrays.asList(s));

                        unit.closeScope(s);

                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                });

        final IUnitResult<Scope, Integer, ITerm, Object> result = future.asJavaCompletion().get();

        //        assertEquals(2, result.unitResults().size());
    }

    @Test(timeout = 10000) public void testTwoUnitsDeadlockCycle() throws ExecutionException, InterruptedException {
        final Broker<Scope, Integer, ITerm> broker = new Broker<>(scopeImpl, Set.Immutable.of(1, 2), new NullCancel());

        final IFuture<IUnitResult<Scope, Integer, ITerm, Object>> future =
                broker.add(".", new ITypeChecker<Scope, Integer, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        unit.add("one", new ResolveBeforeCloseUnit(1, 2), Arrays.asList(s));
                        unit.add("two", new ResolveBeforeCloseUnit(2, 1), Arrays.asList(s));

                        unit.closeScope(s);

                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                });

        final IUnitResult<Scope, Integer, ITerm, Object> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testThreeUnitsDeadlockCycle() throws ExecutionException, InterruptedException {
        final Broker<Scope, Integer, ITerm> broker = new Broker<>(scopeImpl, Arrays.asList(1, 2, 3), new NullCancel());

        final IFuture<IUnitResult<Scope, Integer, ITerm, Object>> future =
                broker.add(".", new ITypeChecker<Scope, Integer, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        unit.add("one", new ResolveBeforeCloseUnit(1, 2), Arrays.asList(s));
                        unit.add("two", new ResolveBeforeCloseUnit(2, 3), Arrays.asList(s));
                        unit.add("three", new ResolveBeforeCloseUnit(3, 1), Arrays.asList(s));

                        unit.closeScope(s);

                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                });

        final IUnitResult<Scope, Integer, ITerm, Object> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testTwoUnitsCycle() throws ExecutionException, InterruptedException {
        final Broker<Scope, Integer, ITerm> broker = new Broker<>(scopeImpl, Set.Immutable.of(1, 2), new NullCancel());

        final IFuture<IUnitResult<Scope, Integer, ITerm, Object>> future =
                broker.add(".", new ITypeChecker<Scope, Integer, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        unit.add("one", new CloseBeforeResolveUnit(1, 2), Arrays.asList(s));
                        unit.add("two", new CloseBeforeResolveUnit(2, 1), Arrays.asList(s));

                        unit.closeScope(s);

                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                });

        final IUnitResult<Scope, Integer, ITerm, Object> result = future.asJavaCompletion().get();
    }

    @Test(timeout = 10000) public void testThreeUnitsCycle() throws ExecutionException, InterruptedException {
        final Broker<Scope, Integer, ITerm> broker = new Broker<>(scopeImpl, Arrays.asList(1, 2, 3), new NullCancel());

        final IFuture<IUnitResult<Scope, Integer, ITerm, Object>> future =
                broker.add(".", new ITypeChecker<Scope, Integer, ITerm, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit,
                            List<Scope> roots) {
                        Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                        unit.add("one", new CloseBeforeResolveUnit(1, 2), Arrays.asList(s));
                        unit.add("two", new CloseBeforeResolveUnit(2, 3), Arrays.asList(s));
                        unit.add("three", new CloseBeforeResolveUnit(3, 1), Arrays.asList(s));

                        unit.closeScope(s);

                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                });

        final IUnitResult<Scope, Integer, ITerm, Object> result = future.asJavaCompletion().get();
    }

    private final class ResolveBeforeCloseUnit implements ITypeChecker<Scope, Integer, ITerm, Object> {

        private final Integer ownLabel;
        private final Integer queryLabel;

        public ResolveBeforeCloseUnit(Integer ownLabel, Integer queryLabel) {
            this.ownLabel = ownLabel;
            this.queryLabel = queryLabel;
        }

        @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit, List<Scope> rootScopes) {
            Scope s = rootScopes.get(0);
            unit.initScope(s, Set.Immutable.of(ownLabel), false);

            Scope s1 = unit.freshScope("s'", Collections.emptySet(), false, true);

            unit.addEdge(s, ownLabel, s1);
            unit.closeEdge(s, ownLabel);

            final RegExpBuilder<Integer> reb = new RegExpBuilder<>();
            final IRegExp<Integer> re = reb.and(reb.symbol(queryLabel), reb.complement(reb.emptySet()));
            final IRegExpMatcher<Integer> rem = RegExpMatcher.create(re);

            unit.query(s, new RegExpLabelWf<>(rem), LabelOrder.none(), DataWf.any(), DataLeq.none())
                    .whenComplete((r, ex) -> {
                        unit.closeScope(s1);
                    });

            return CompletableFuture.completedFuture(Unit.unit);
        }

    }

    private final class CloseBeforeResolveUnit implements ITypeChecker<Scope, Integer, ITerm, Object> {

        private final Integer ownLabel;
        private final Integer queryLabel;

        public CloseBeforeResolveUnit(Integer ownLabel, Integer queryLabel) {
            this.ownLabel = ownLabel;
            this.queryLabel = queryLabel;
        }

        @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, ITerm> unit, List<Scope> rootScopes) {
            Scope s = rootScopes.get(0);
            unit.initScope(s, Set.Immutable.of(ownLabel), false);

            Scope s1 = unit.freshScope("s'", Collections.emptySet(), false, true);

            unit.addEdge(s, ownLabel, s1);
            unit.closeEdge(s, ownLabel);

            final RegExpBuilder<Integer> reb = new RegExpBuilder<>();
            final IRegExp<Integer> re = reb.and(reb.symbol(queryLabel), reb.complement(reb.emptySet()));
            final IRegExpMatcher<Integer> rem = RegExpMatcher.create(re);

            unit.closeScope(s1);

            unit.query(s, new RegExpLabelWf<>(rem), LabelOrder.none(), DataWf.any(), DataLeq.none());

            return CompletableFuture.completedFuture(Unit.unit);
        }

    }


}