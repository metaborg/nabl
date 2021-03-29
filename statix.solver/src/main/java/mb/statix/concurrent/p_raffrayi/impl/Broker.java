package mb.statix.concurrent.p_raffrayi.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.metaborg.util.functions.Function2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.util.Tuple2;
import mb.statix.concurrent.actors.IActor;
import mb.statix.concurrent.actors.IActorRef;
import mb.statix.concurrent.actors.TypeTag;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.actors.impl.ActorSystem;
import mb.statix.concurrent.actors.impl.IActorScheduler;
import mb.statix.concurrent.actors.impl.WonkyScheduler;
import mb.statix.concurrent.actors.impl.WorkStealingScheduler;
import mb.statix.concurrent.p_raffrayi.IScopeImpl;
import mb.statix.concurrent.p_raffrayi.ITypeChecker;
import mb.statix.concurrent.p_raffrayi.IUnitResult;

public class Broker<S, L, D, R> {

    private static final ILogger logger = LoggerUtils.logger(Broker.class);

    private static final int INACTIVE_TIMEOUT = 5;

    private final String id;
    private final ITypeChecker<S, L, D, R> typeChecker;
    private final IScopeImpl<S, D> scopeImpl;
    private final Set<L> edgeLabels;
    private final ICancel cancel;

    private final IActorScheduler scheduler;
    private final ActorSystem system;

    private final Map<String, IActorRef<? extends IUnit<S, L, D, ?>>> units;
    private final AtomicInteger unfinishedUnits;
    private final AtomicInteger totalUnits;

    private Broker(String id, ITypeChecker<S, L, D, R> typeChecker, IScopeImpl<S, D> scopeImpl, Iterable<L> edgeLabels,
            ICancel cancel, IActorScheduler scheduler) {
        this.id = id;
        this.typeChecker = typeChecker;
        this.scopeImpl = scopeImpl;
        this.edgeLabels = ImmutableSet.copyOf(edgeLabels);
        this.cancel = cancel;

        this.scheduler = scheduler;
        this.system = new ActorSystem(scheduler);

        this.units = new ConcurrentHashMap<>();
        this.unfinishedUnits = new AtomicInteger();
        this.totalUnits = new AtomicInteger();
    }

    private IFuture<IUnitResult<S, L, D, R>> run() {
        final IActor<IUnit<S, L, D, R>> unit = system.add(id, TypeTag.of(IUnit.class),
                self -> new TypeCheckerUnit<>(self, null, new UnitContext(self), typeChecker, edgeLabels));
        addUnit(unit);

        final IFuture<IUnitResult<S, L, D, R>> unitResult = system.async(unit)._start(Collections.emptyList());

        final IFuture<IUnitResult<S, L, D, R>> runResult = unitResult.compose((r, ex) -> {
            finalizeUnit(unit, ex);
            return system.stop().compose((r2, ex2) -> CompletableFuture.completed(r, ex));
        });

        startWatcherThread();

        return runResult;
    }

    private void addUnit(IActorRef<? extends IUnit<S, L, D, ?>> unit) {
        unfinishedUnits.incrementAndGet();
        totalUnits.incrementAndGet();
        units.put(unit.id(), unit);
    }

    private void finalizeUnit(IActorRef<? extends IUnit<S, L, D, ?>> unit, Throwable ex) {
        final String event = ex != null ? "failed" : "finished";
        logger.info("Unit {} {} ({} of {} remaining).", event, unit.id(), unfinishedUnits.decrementAndGet(),
                totalUnits.get());
    }

    private void startWatcherThread() {
        final AtomicInteger inactive = new AtomicInteger();
        final Thread watcher = new Thread(() -> {
            try {
                while(true) {
                    if(!system.running()) {
                        return;
                    } else if(cancel.cancelled()) {
                        system.cancel();
                        return;
                    } else if(!scheduler.isActive()) {
                        if(inactive.incrementAndGet() < INACTIVE_TIMEOUT) {
                            logger.error("Potential deadlock...");
                        } else {
                            logger.error("Deadlock detected.");
                            system.cancel();
                            return;
                        }
                    } else {
                        if(inactive.getAndSet(0) != 0) {
                            logger.error("False deadlock.");
                        }
                    }
                    Thread.sleep(1000);
                }
            } catch(InterruptedException e) {
            }
        }, "PRaffrayiWatcher");
        watcher.start();
    }

    private class UnitContext implements IUnitContext<S, L, D> {

        private final IActor<? extends IUnit<S, L, D, ?>> self;

        public UnitContext(IActor<? extends IUnit<S, L, D, ?>> self) {
            this.self = self;
        }

        @Override public ICancel cancel() {
            return cancel;
        }

        @Override public S makeScope(String name) {
            return scopeImpl.make(self.id(), name);
        }

        @Override public D substituteScopes(D datum, Map<S, S> substitution) {
            return scopeImpl.subtituteScopes(datum, substitution);
        }

        @Override public IActorRef<? extends IUnit<S, L, D, ?>> owner(S scope) {
            return units.get(scopeImpl.id(scope));
        }

        @Override public <Q> Tuple2<IFuture<IUnitResult<S, L, D, Q>>, IActorRef<? extends IUnit<S, L, D, Q>>> add(
                String id, Function2<IActor<IUnit<S, L, D, Q>>, IUnitContext<S, L, D>, IUnit<S, L, D, Q>> unitProvider,
                List<S> rootScopes) {
            final IActorRef<IUnit<S, L, D, Q>> unit = self.add(id, TypeTag.of(IUnit.class),
                    (subself) -> unitProvider.apply(subself, new UnitContext(subself)));
            addUnit(unit);
            final IFuture<IUnitResult<S, L, D, Q>> unitResult = self.async(unit)._start(rootScopes);
            unitResult.whenComplete((r, ex) -> finalizeUnit(unit, ex));
            return Tuple2.of(unitResult, unit);
        }

    }

    public static <S, L, D, R> IFuture<IUnitResult<S, L, D, R>> run(String id, ITypeChecker<S, L, D, R> unitChecker,
            IScopeImpl<S, D> scopeImpl, Iterable<L> edgeLabels, ICancel cancel) {
        return run(id, unitChecker, scopeImpl, edgeLabels, cancel, Runtime.getRuntime().availableProcessors());
    }

    public static <S, L, D, R> IFuture<IUnitResult<S, L, D, R>> run(String id, ITypeChecker<S, L, D, R> typeChecker,
            IScopeImpl<S, D> scopeImpl, Iterable<L> edgeLabels, ICancel cancel, int parallelism) {
        return new Broker<>(id, typeChecker, scopeImpl, edgeLabels, cancel, new WorkStealingScheduler(parallelism))
                .run();
    }

    public static <S, L, D, R> IFuture<IUnitResult<S, L, D, R>> debug(String id, ITypeChecker<S, L, D, R> typeChecker,
            IScopeImpl<S, D> scopeImpl, Iterable<L> edgeLabels, ICancel cancel, double preemptProbability,
            int scheduleDelayBoundMillis) {
        return debug(id, typeChecker, scopeImpl, edgeLabels, cancel, Runtime.getRuntime().availableProcessors(),
                preemptProbability, scheduleDelayBoundMillis);
    }

    public static <S, L, D, R> IFuture<IUnitResult<S, L, D, R>> debug(String id, ITypeChecker<S, L, D, R> typeChecker,
            IScopeImpl<S, D> scopeImpl, Iterable<L> edgeLabels, ICancel cancel, int parallelism,
            double preemptProbability, int scheduleDelayBoundMillis) {
        return new Broker<>(id, typeChecker, scopeImpl, edgeLabels, cancel,
                new WonkyScheduler(parallelism, preemptProbability, scheduleDelayBoundMillis)).run();
    }

}