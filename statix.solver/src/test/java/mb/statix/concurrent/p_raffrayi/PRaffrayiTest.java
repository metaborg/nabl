package mb.statix.concurrent.p_raffrayi;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.metaborg.util.task.NullCancel;
import org.metaborg.util.unit.Unit;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.impl.Broker;
import mb.statix.concurrent.p_raffrayi.impl.ScopeImpl;
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

}