package mb.statix.concurrent.p_raffrayi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.metaborg.util.task.NullCancel;
import org.metaborg.util.unit.Unit;

import io.usethesource.capsule.Set;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.impl.Broker;
import mb.statix.concurrent.p_raffrayi.impl.ScopeImpl;
import mb.statix.scopegraph.terms.Scope;

//@Ignore
public class PRaffrayiTest {

    private final IScopeImpl<Scope> scopeImpl = new ScopeImpl();

    @Test(timeout = 10000) public void testEmpty() throws ExecutionException, InterruptedException {
        final Broker<Scope, Object, Object, Object> broker =
                new Broker<>(scopeImpl, Set.Immutable.of(), new NullCancel());

        broker.run();
        final IBrokerResult<Scope, Object, Object, Object> result = broker.result().get();

        assertTrue(result.unitResults().isEmpty());
    }

    @Test(timeout = 10000) public void testSingleNoop() throws ExecutionException, InterruptedException {
        final Broker<Scope, Object, Object, Object> broker =
                new Broker<>(scopeImpl, Set.Immutable.of(), new NullCancel());

        broker.add("/", new ITypeChecker<Scope, Object, Object, Object>() {

            @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Object, Object, Object> unit, Scope root) {
                return CompletableFuture.completedFuture(Unit.unit);
            }

        });

        broker.run();
        final IBrokerResult<Scope, Object, Object, Object> result = broker.result().get();

        assertEquals(1, result.unitResults().size());
    }

    @Test(timeout = 10000) public void testSingleOneScope() throws ExecutionException, InterruptedException {
        final Broker<Scope, Integer, Object, Object> broker =
                new Broker<>(scopeImpl, Set.Immutable.of(), new NullCancel());

        broker.add("/", new ITypeChecker<Scope, Integer, Object, Object>() {

            @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Object, Object> unit, Scope root) {
                Scope s = unit.freshScope("s", Set.Immutable.of(1), false, false);
                unit.addEdge(s, 1, s);
                unit.closeEdge(s, 1);
                return CompletableFuture.completedFuture(Unit.unit);
            }

        });

        broker.run();
        final IBrokerResult<Scope, Integer, Object, Object> result = broker.result().get();

        assertEquals(1, result.unitResults().size());
    }

    @Test(timeout = 10000) public void testSingleOneScope_NoClose() throws ExecutionException, InterruptedException {
        final Broker<Scope, Integer, Object, Object> broker =
                new Broker<>(scopeImpl, Set.Immutable.of(), new NullCancel());

        broker.add("/", new ITypeChecker<Scope, Integer, Object, Object>() {

            @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Object, Object> unit, Scope root) {
                Scope s = unit.freshScope("s", Set.Immutable.of(1), false, false);
                unit.addEdge(s, 1, s);
                return CompletableFuture.completedFuture(Unit.unit);
            }

        });

        broker.run();
        final IBrokerResult<Scope, Integer, Object, Object> result = broker.result().get();
        assertNotNull(result.unitResults().get("/").analysis());
        assertFalse(result.unitResults().get("/").failures().isEmpty());
    }

    @Test(timeout = 10000) public void testSingleParentChild() throws ExecutionException, InterruptedException {
        final Broker<Scope, Integer, Object, Object> broker =
                new Broker<>(scopeImpl, Set.Immutable.of(), new NullCancel());

        broker.add("/", new ITypeChecker<Scope, Integer, Object, Object>() {

            @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Object, Object> unit, Scope root) {
                Scope s = unit.freshScope("s", Set.Immutable.of(), false, true);

                unit.add("/sub", new ITypeChecker<Scope, Integer, Object, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Object, Object> unit,
                            Scope root) {
                        unit.initRoot(root, Set.Immutable.of(), false);
                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                }, s);

                unit.closeScope(s);

                return CompletableFuture.completedFuture(Unit.unit);
            }

        });

        broker.run();
        final IBrokerResult<Scope, Integer, Object, Object> result = broker.result().get();

        assertEquals(2, result.unitResults().size());
    }

    @Test(timeout = 10000) public void testSingleParentChild_ParentNoClose()
            throws ExecutionException, InterruptedException {
        final Broker<Scope, Integer, Object, Object> broker =
                new Broker<>(scopeImpl, Set.Immutable.of(), new NullCancel());

        broker.add("/", new ITypeChecker<Scope, Integer, Object, Object>() {

            @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Object, Object> unit, Scope root) {
                Scope s = unit.freshScope("s", Set.Immutable.of(), false, true);

                unit.add("/sub", new ITypeChecker<Scope, Integer, Object, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Object, Object> unit,
                            Scope root) {
                        unit.initRoot(root, Set.Immutable.of(), false);
                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                }, s);

                return CompletableFuture.completedFuture(Unit.unit);
            }

        });

        broker.run();
        final IBrokerResult<Scope, Integer, Object, Object> result = broker.result().get();

        assertEquals(2, result.unitResults().size());

        assertNotNull(result.unitResults().get("/").analysis());
        assertFalse(result.unitResults().get("/").failures().isEmpty());

        assertNotNull(result.unitResults().get("/sub").analysis());
        assertTrue(result.unitResults().get("/sub").failures().isEmpty());
    }

    @Test(timeout = 10000) public void testSingleParentChild_ChildNoInitRoot()
            throws ExecutionException, InterruptedException {
        final Broker<Scope, Integer, Object, Object> broker =
                new Broker<>(scopeImpl, Set.Immutable.of(), new NullCancel());

        broker.add("/", new ITypeChecker<Scope, Integer, Object, Object>() {

            @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Object, Object> unit, Scope root) {
                Scope s = unit.freshScope("s", Set.Immutable.of(), false, true);

                unit.add("/sub", new ITypeChecker<Scope, Integer, Object, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Object, Object> unit,
                            Scope root) {
                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                }, s);

                unit.closeScope(s);

                return CompletableFuture.completedFuture(Unit.unit);
            }

        });

        broker.run();
        final IBrokerResult<Scope, Integer, Object, Object> result = broker.result().get();

        assertEquals(2, result.unitResults().size());

        assertNotNull(result.unitResults().get("/").analysis());
        assertTrue(result.unitResults().get("/").failures().isEmpty());

        assertNotNull(result.unitResults().get("/sub").analysis());
        assertFalse(result.unitResults().get("/sub").failures().isEmpty());
    }

    @Test(timeout = 10000) public void testSingleParentChild_ChildNoCloseEdge()
            throws ExecutionException, InterruptedException {
        final Broker<Scope, Integer, Object, Object> broker =
                new Broker<>(scopeImpl, Set.Immutable.of(), new NullCancel());

        broker.add("/", new ITypeChecker<Scope, Integer, Object, Object>() {

            @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Object, Object> unit, Scope root) {
                Scope s = unit.freshScope("s", Set.Immutable.of(), false, true);

                unit.add("/sub", new ITypeChecker<Scope, Integer, Object, Object>() {

                    @Override public IFuture<Object> run(ITypeCheckerContext<Scope, Integer, Object, Object> unit,
                            Scope root) {
                        unit.initRoot(root, Set.Immutable.of(1), false);
                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                }, s);

                unit.closeScope(s);

                return CompletableFuture.completedFuture(Unit.unit);
            }

        });

        broker.run();
        final IBrokerResult<Scope, Integer, Object, Object> result = broker.result().get();

        assertEquals(2, result.unitResults().size());
    }

}