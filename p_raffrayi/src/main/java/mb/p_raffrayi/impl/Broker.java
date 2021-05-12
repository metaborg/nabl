package mb.p_raffrayi.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.metaborg.util.functions.Function2;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.ICompletable;
import org.metaborg.util.future.ICompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.tuple.Tuple2;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import mb.p_raffrayi.IScopeImpl;
import mb.p_raffrayi.ITypeChecker;
import mb.p_raffrayi.IUnitResult;
import mb.p_raffrayi.actors.IActor;
import mb.p_raffrayi.actors.IActorRef;
import mb.p_raffrayi.actors.TypeTag;
import mb.p_raffrayi.actors.impl.ActorSystem;
import mb.p_raffrayi.actors.impl.IActorScheduler;
import mb.p_raffrayi.actors.impl.WonkyScheduler;
import mb.p_raffrayi.actors.impl.WorkStealingScheduler;
import mb.p_raffrayi.impl.diff.IScopeGraphDifferOps;

public class Broker<S, L, D, R> {

    private static final ILogger logger = LoggerUtils.logger(Broker.class);

    private static final int INACTIVE_TIMEOUT = 5;

    private final String id;
    private final ITypeChecker<S, L, D, R> typeChecker;
    private final IInitialState<S, L, D, R> initialState;
    private final IScopeImpl<S, D> scopeImpl;
    private final Set<L> edgeLabels;
    private final IScopeGraphDifferOps<S, D> differOps;
    private final ICancel cancel;

    private final IActorScheduler scheduler;
    private final ActorSystem system;

    private final Map<String, IActorRef<? extends IUnit<S, L, D, ?>>> units;
    private final AtomicInteger unfinishedUnits;
    private final AtomicInteger totalUnits;

    private final Map<String, Set<ICompletable<IActorRef<? extends IUnit<S, L, D, ?>>>>> delays;
    private final Object lock = new Object(); // Used to synchronize updates/queries of `units` and `delays`

    private Broker(String id, ITypeChecker<S, L, D, R> typeChecker, IScopeImpl<S, D> scopeImpl, Iterable<L> edgeLabels,
            IInitialState<S, L, D, R> initialState, IScopeGraphDifferOps<S, D> differOps, ICancel cancel,
            IActorScheduler scheduler) {
        this.id = id;
        this.typeChecker = typeChecker;
        this.initialState = initialState;
        this.scopeImpl = scopeImpl;
        this.edgeLabels = ImmutableSet.copyOf(edgeLabels);
        this.differOps = differOps;
        this.cancel = cancel;

        this.scheduler = scheduler;
        this.system = new ActorSystem(scheduler);

        this.units = new ConcurrentHashMap<>();
        this.unfinishedUnits = new AtomicInteger();
        this.totalUnits = new AtomicInteger();

        this.delays = new ConcurrentHashMap<>();
    }

    private IFuture<IUnitResult<S, L, D, R>> run() {
        final IActor<IUnit<S, L, D, R>> unit =
                system.add(id, TypeTag.of(IUnit.class), self -> new TypeCheckerUnit<>(self, null, new UnitContext(self),
                        typeChecker, edgeLabels, initialState, scopeImpl, differOps));
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
        synchronized(lock) {
            units.put(unit.id(), unit);
            delays.computeIfPresent(unit.id(), (id, futures) -> {
                futures.forEach(f -> f.complete(unit));
                return null; // remove mapping
            });
        }
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

        @Override public String scopeId(S scope) {
            return scopeImpl.id(scope);
        }

        @Override public D substituteScopes(D datum, Map<S, S> substitution) {
            return scopeImpl.substituteScopes(datum, substitution);
        }

        // TODO: this type of asynchrony is very prone to errors
        // because there is no deadlock detection on it.
        // Hence a better way to wait for actors starting should be invented.
        @Override public IFuture<IActorRef<? extends IUnit<S, L, D, ?>>> owner(S scope) {
            final String id = scopeImpl.id(scope);
            synchronized(lock) {
                if(units.containsKey(id)) {
                    return CompletableFuture.completedFuture(units.get(id));
                } else {
                    final ICompletableFuture<IActorRef<? extends IUnit<S, L, D, ?>>> future = new CompletableFuture<>();
                    delays.computeIfAbsent(id, key -> Sets.newConcurrentHashSet()).add(future);
                    return future;
                }
            }
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

        @Override public int parallelism() {
            return scheduler.parallelism();
        }

    }

    public static <S, L, D, R> IFuture<IUnitResult<S, L, D, R>> run(String id, ITypeChecker<S, L, D, R> unitChecker,
            IScopeImpl<S, D> scopeImpl, Iterable<L> edgeLabels, IInitialState<S, L, D, R> initialState,
            IScopeGraphDifferOps<S, D> differOps, ICancel cancel) {
        return run(id, unitChecker, scopeImpl, edgeLabels, initialState, differOps, cancel,
                Runtime.getRuntime().availableProcessors());
    }

    public static <S, L, D, R> IFuture<IUnitResult<S, L, D, R>> run(String id, ITypeChecker<S, L, D, R> typeChecker,
            IScopeImpl<S, D> scopeImpl, Iterable<L> edgeLabels, IInitialState<S, L, D, R> initialState,
            IScopeGraphDifferOps<S, D> differOps, ICancel cancel, int parallelism) {
        return new Broker<>(id, typeChecker, scopeImpl, edgeLabels, initialState, differOps, cancel,
                new WorkStealingScheduler(parallelism)).run();
    }

    public static <S, L, D, R> IFuture<IUnitResult<S, L, D, R>> debug(String id, ITypeChecker<S, L, D, R> typeChecker,
            IScopeImpl<S, D> scopeImpl, Iterable<L> edgeLabels, IScopeGraphDifferOps<S, D> differOps, ICancel cancel,
            double preemptProbability, int scheduleDelayBoundMillis) {
        return debug(id, typeChecker, scopeImpl, edgeLabels, AInitialState.added(), differOps, cancel,
                Runtime.getRuntime().availableProcessors(), preemptProbability, scheduleDelayBoundMillis);
    }

    public static <S, L, D, R> IFuture<IUnitResult<S, L, D, R>> debug(String id, ITypeChecker<S, L, D, R> typeChecker,
            IScopeImpl<S, D> scopeImpl, Iterable<L> edgeLabels, IInitialState<S, L, D, R> initialState,
            IScopeGraphDifferOps<S, D> differOps, ICancel cancel, double preemptProbability,
            int scheduleDelayBoundMillis) {
        return debug(id, typeChecker, scopeImpl, edgeLabels, initialState, differOps, cancel,
                Runtime.getRuntime().availableProcessors(), preemptProbability, scheduleDelayBoundMillis);
    }

    public static <S, L, D, R> IFuture<IUnitResult<S, L, D, R>> debug(String id, ITypeChecker<S, L, D, R> typeChecker,
            IScopeImpl<S, D> scopeImpl, Iterable<L> edgeLabels, IInitialState<S, L, D, R> initialState,
            IScopeGraphDifferOps<S, D> differOps, ICancel cancel, int parallelism, double preemptProbability,
            int scheduleDelayBoundMillis) {
        return new Broker<>(id, typeChecker, scopeImpl, edgeLabels, initialState, differOps, cancel,
                new WonkyScheduler(parallelism, preemptProbability, scheduleDelayBoundMillis)).run();
    }

}