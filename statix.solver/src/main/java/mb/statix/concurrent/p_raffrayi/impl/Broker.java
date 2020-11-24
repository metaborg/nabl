package mb.statix.concurrent.p_raffrayi.impl;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.util.Tuple2;
import mb.nabl2.util.collections.MultiSet;
import mb.statix.concurrent.actors.IActor;
import mb.statix.concurrent.actors.IActorMonitor;
import mb.statix.concurrent.actors.IActorRef;
import mb.statix.concurrent.actors.TypeTag;
import mb.statix.concurrent.actors.deadlock.Clock;
import mb.statix.concurrent.actors.deadlock.Deadlock;
import mb.statix.concurrent.actors.deadlock.DeadlockBatcher;
import mb.statix.concurrent.actors.deadlock.DeadlockMonitor;
import mb.statix.concurrent.actors.deadlock.IDeadlockMonitor;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.ICompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.actors.impl.ActorSystem;
import mb.statix.concurrent.p_raffrayi.IBroker;
import mb.statix.concurrent.p_raffrayi.IScopeImpl;
import mb.statix.concurrent.p_raffrayi.ITypeChecker;
import mb.statix.concurrent.p_raffrayi.IUnitResult;
import mb.statix.concurrent.p_raffrayi.impl.tokens.IWaitFor;

public class Broker<S, L, D> implements IBroker<S, L, D>, IActorMonitor {

    static final String ID_SEP = "/";

    private static final ILogger logger = LoggerUtils.logger(Broker.class);

    private final IScopeImpl<S> scopeImpl;
    private final Set<L> edgeLabels;
    private final ICancel cancel;

    private final ActorSystem system;
    private final IActor<IDeadlockMonitor<IUnit<S, L, D, ?>>> dlm;

    private final Map<String, IActorRef<? extends IUnit<S, L, D, ?>>> units;
    private final AtomicInteger unfinishedUnits;
    private final AtomicInteger totalUnits;
    private final ICompletableFuture<org.metaborg.util.unit.Unit> result;

    public Broker(IScopeImpl<S> scopeImpl, Iterable<L> edgeLabels, ICancel cancel) {
        this(scopeImpl, edgeLabels, cancel, Runtime.getRuntime().availableProcessors());
    }

    public Broker(IScopeImpl<S> scopeImpl, Iterable<L> edgeLabels, ICancel cancel, int parallelism) {
        this.scopeImpl = scopeImpl;
        this.edgeLabels = ImmutableSet.copyOf(edgeLabels);
        this.cancel = cancel;

        this.system = new ActorSystem(parallelism);
        this.dlm = system.add("<DLM>", TypeTag.of(IDeadlockMonitor.class),
                self -> new DeadlockMonitor<>(self, this::handleDeadlock));
        dlm.addMonitor(this);

        this.units = new ConcurrentHashMap<>();
        this.unfinishedUnits = new AtomicInteger();
        this.totalUnits = new AtomicInteger();

        this.result = new CompletableFuture<>();
    }

    @Override public <R> IFuture<IUnitResult<S, L, D, R>> add(String id, ITypeChecker<S, L, D, R> unitChecker) {
        if(system.running()) {
            throw new IllegalStateException("Cannot add units when already running.");
        }
        final IActor<IUnit<S, L, D, R>> unit = system.add(id, TypeTag.of(IUnit.class),
                self -> new Unit<>(self, null, new UnitContext(self), unitChecker, edgeLabels));
        addUnit(unit);
        return system.async(unit)._start(null).whenComplete((r, ex) -> {
            if(ex != null) {
                fail(ex);
            } else {
                finished(unit);
            }
        });
    }

    private void addUnit(IActor<? extends IUnit<S, L, D, ?>> unit) {
        unfinishedUnits.incrementAndGet();
        totalUnits.incrementAndGet();
        unit.addMonitor(this);
        units.put(unit.id(), unit);
    }

    private void finished(IActor<?> self) {
        logger.info("Unit {} finished ({} of {} remaining).", self.id(), unfinishedUnits.decrementAndGet(),
                totalUnits.get());
        if(unfinishedUnits.get() == 0) {
            logger.info("All {} units finished.", totalUnits.get());
            result.complete(org.metaborg.util.unit.Unit.unit);
            system.stop();
        }
    }

    @Override public void failed(IActor<?> self, Throwable ex) {
        fail(ex);
    }

    private void fail(Throwable ex) {
        logger.error("Unit failed.", ex);
        system.stop();
        result.completeExceptionally(ex);
    }

    private void handleDeadlock(IActor<?> dlm, Deadlock<IActorRef<? extends IUnit<S, L, D, ?>>> deadlock) {
        for(Entry<IActorRef<? extends IUnit<S, L, D, ?>>, Clock<IActorRef<? extends IUnit<S, L, D, ?>>>> entry : deadlock
                .nodes().entrySet()) {
            final IActorRef<? extends IUnit<S, L, D, ?>> unit = entry.getKey();
            final Clock<IActorRef<? extends IUnit<S, L, D, ?>>> clock = entry.getValue();
            logger.debug("deadlock detected: {}", deadlock);
            dlm.async(unit)._deadlocked(clock, deadlock.nodes().keySet());
        }
    }

    @Override public IFuture<org.metaborg.util.unit.Unit> run() {
        system.start();

        // start cancel watcher
        final Thread watcher = new Thread(() -> {
            try {
                while(true) {
                    if(!system.running()) {
                        return;
                    } else if(cancel.cancelled()) {
                        system.cancel();
                        return;
                    } else {
                        Thread.sleep(1000);
                    }
                }
            } catch(InterruptedException e) {
            }
        }, "PRaffrayiWatcher");
        watcher.start();

        return result;
    }

    private class UnitContext implements IUnitContext<S, L, D> {

        private final IActor<? extends IUnit<S, L, D, ?>> self;
        private final DeadlockBatcher<IUnit<S, L, D, ?>, IWaitFor<S, L, D>> udlm;

        public UnitContext(IActor<? extends IUnit<S, L, D, ?>> self) {
            this.self = self;
            this.udlm = new DeadlockBatcher<>(self, dlm);
        }

        @Override public ICancel cancel() {
            return cancel;
        }

        @Override public S makeScope(String name) {
            return scopeImpl.make(self.id(), name);
        }

        @Override public IActorRef<? extends IUnit<S, L, D, ?>> owner(S scope) {
            return units.get(scopeImpl.id(scope));
        }

        @Override public <R> Tuple2<IFuture<IUnitResult<S, L, D, R>>, IActorRef<? extends IUnit<S, L, D, R>>>
                add(String id, ITypeChecker<S, L, D, R> unitChecker, S root) {
            final IActor<IUnit<S, L, D, R>> unit = self.add(id, TypeTag.of(IUnit.class),
                    self -> new Unit<>(self, UnitContext.this.self, new UnitContext(self), unitChecker, edgeLabels));
            Broker.this.addUnit(unit);
            final IFuture<IUnitResult<S, L, D, R>> result = self.async(unit)._start(root).whenComplete((r, ex) -> {
                finished(unit);
            });
            return Tuple2.of(result, unit);
        }

        @Override public void waitFor(IWaitFor<S, L, D> token, IActorRef<? extends IUnit<S, L, D, ?>> unit) {
            udlm.waitFor(unit, token);
        }

        @Override public void granted(IWaitFor<S, L, D> token, IActorRef<? extends IUnit<S, L, D, ?>> unit) {
            udlm.granted(unit, token);
        }

        @Override public boolean isWaiting() {
            return udlm.isWaiting();
        }

        @Override public boolean isWaitingFor(IWaitFor<S, L, D> token) {
            return udlm.isWaitingFor(token);
        }

        @Override public MultiSet.Immutable<IWaitFor<S, L, D>> getTokens(IActorRef<? extends IUnit<S, L, D, ?>> unit) {
            return udlm.getTokens(unit);
        }

        @Override public void suspended(Clock<IActorRef<? extends IUnit<S, L, D, ?>>> clock) {
            udlm.suspended(clock);
        }

    }

    public static <S, L, D, R> IFuture<IUnitResult<S, L, D, R>> singleShot(IScopeImpl<S> scopeImpl,
            Iterable<L> edgeLabels, String id, ITypeChecker<S, L, D, R> unitChecker, ICancel cancel) {
        return singleShot(scopeImpl, edgeLabels, id, unitChecker, Runtime.getRuntime().availableProcessors(), cancel);
    }

    public static <S, L, D, R> IFuture<IUnitResult<S, L, D, R>> singleShot(IScopeImpl<S> scopeImpl,
            Iterable<L> edgeLabels, String id, ITypeChecker<S, L, D, R> unitChecker, int parallelism, ICancel cancel) {
        final Broker<S, L, D> broker = new Broker<>(scopeImpl, edgeLabels, cancel);
        final IFuture<IUnitResult<S, L, D, R>> result = broker.add(id, unitChecker);
        return broker.run().compose((r, ex) -> result);
    }

}