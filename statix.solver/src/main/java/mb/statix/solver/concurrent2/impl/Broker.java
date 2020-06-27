package mb.statix.solver.concurrent2.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.metaborg.util.task.ICancel;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import mb.statix.actors.ActorSystem;
import mb.statix.actors.CompletableFuture;
import mb.statix.actors.IActor;
import mb.statix.actors.IFuture;
import mb.statix.actors.TypeTag;
import mb.statix.solver.concurrent2.IBroker;
import mb.statix.solver.concurrent2.IResult;
import mb.statix.solver.concurrent2.IScopeImpl;
import mb.statix.solver.concurrent2.ITypeChecker;

public class Broker<S, L, D> implements IBroker<S, L, D> {

    private final IScopeImpl<S> scopeImpl;
    private final ActorSystem system;
    private final Map<String, IActor<IUnitProtocol<S, L, D>>> units;

    public Broker(IScopeImpl<S> scopeImpl) {
        this.scopeImpl = scopeImpl;
        this.system = new ActorSystem();
        this.units = Maps.newHashMap();
    }

    @Override public void add(String id, ITypeChecker<S, L, D> unitChecker) {
        @SuppressWarnings("unchecked") final IActor<IUnitProtocol<S, L, D>> actor =
                system.add(id, TypeTag.of(IUnitProtocol.class), self -> new Unit<>(new UnitBroker(id), unitChecker));
        units.put(id, actor);
    }

    @Override public IUnitProtocol<S, L, D> get(String id) {
        return units.get(id).get();
    }

    @Override public IFuture<IResult<S, L, D>> run(ICancel cancel) {
        // start threads for all units
        final List<java.util.concurrent.CompletableFuture<?>> futures = Lists.newArrayList();
        final ExecutorService executor = Executors.newCachedThreadPool();
        for(IActor<IUnitProtocol<S, L, D>> unit : units.values()) {
//            futures.add(java.util.concurrent.CompletableFuture.runAsync(unit, executor));
        }

        //        // start cancel watcher
        //        final Thread watcher = new Thread(() -> {
        //            try {
        //                while(true) {
        //                    Thread.sleep(1000);
        //                    if(cancel.cancelled()) {
        //                        executor.shutdownNow();
        //                        return;
        //                    }
        //                }
        //            } catch(InterruptedException e) {
        //            }
        //        }, "StatixWatcher");
        //        watcher.start();

        java.util.concurrent.CompletableFuture<?>[] futureArray =
                futures.toArray(new java.util.concurrent.CompletableFuture<?>[futures.size()]);
        java.util.concurrent.CompletableFuture<IResult<S, L, D>> result =
                java.util.concurrent.CompletableFuture.allOf(futureArray).thenApply(results -> {
                    //                    watcher.interrupt();
                    return null;
                });
        return new CompletableFuture<>(result);
    }

    private class UnitBroker implements IBrokerProtocol<S, L, D> {

        private final String id;
        private final Map<String, IUnitProtocol<S, L, D>> cachedRefs;

        public UnitBroker(String id) {
            this.id = id;
            this.cachedRefs = Maps.newHashMap();
        }

        @Override public String id() {
            return id;
        }

        @Override public IUnitProtocol<S, L, D> get(String id) {
            return cachedRefs.computeIfAbsent(id, _1 -> units.get(id).get());
        }

        @Override public IScopeImpl<S> scopeImpl() {
            return scopeImpl;
        }

        @Override public void fail() {
            System.out.println(id + " failed");
        }

    }

}