package mb.p_raffrayi.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.annotation.Nullable;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.MultiSet;
import org.metaborg.util.functions.Action0;
import org.metaborg.util.functions.Function0;
import org.metaborg.util.functions.Function2;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.ICompletable;
import org.metaborg.util.future.ICompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;
import org.metaborg.util.task.NullProgress;
import org.metaborg.util.tuple.Tuple2;

import io.usethesource.capsule.Set.Immutable;
import mb.p_raffrayi.DeadlockException;
import mb.p_raffrayi.IScopeImpl;
import mb.p_raffrayi.ITypeChecker;
import mb.p_raffrayi.ITypeChecker.IOutput;
import mb.p_raffrayi.ITypeChecker.IState;
import mb.p_raffrayi.IUnitResult;
import mb.p_raffrayi.PRaffrayiSettings;
import mb.p_raffrayi.actors.IActor;
import mb.p_raffrayi.actors.IActorRef;
import mb.p_raffrayi.actors.TypeTag;
import mb.p_raffrayi.actors.deadlock.ChandyMisraHaas;
import mb.p_raffrayi.actors.impl.ActorSystem;
import mb.p_raffrayi.actors.impl.IActorScheduler;
import mb.p_raffrayi.actors.impl.WonkyScheduler;
import mb.p_raffrayi.actors.impl.WorkStealingScheduler;
import org.metaborg.util.collection.BiMap;

public class Broker<S, L, D, R extends IOutput<S, L, D>, T extends IState<S, L, D>>
        implements ChandyMisraHaas.Host<IProcess<S, L, D>>, IDeadlockProtocol<S, L, D> {

    private static final ILogger logger = LoggerUtils.logger(Broker.class);

    private static final int INACTIVE_TIMEOUT = 5;

    private final String id;
    private final PRaffrayiSettings settings;
    private final ITypeChecker<S, L, D, R, T> typeChecker;
    private final boolean rootChanged;
    private final @Nullable IUnitResult<S, L, D, Result<S, L, D, R, T>> previousResult;
    private final IScopeImpl<S, D> scopeImpl;
    private final Set<L> edgeLabels;
    private final ICancel cancel;
    private final IProgress progress;

    private final IActorScheduler scheduler;
    private final ActorSystem system;

    private final Map<String, IActorRef<? extends IUnit<S, L, D, ?>>> units;
    private final AtomicInteger unfinishedUnits;
    private final AtomicInteger totalUnits;

    private final Map<String, ICompletableFuture<IActorRef<? extends IUnit<S, L, D, ?>>>> delays;
    private final ReentrantLock lock = new ReentrantLock(); // Used to synchronize updates/queries of `units` and `delays`

    private final BrokerProcess<S, L, D> process;
    private ChandyMisraHaas<IProcess<S, L, D>> cmh;
    private AtomicReference<MultiSet.Immutable<IProcess<S, L, D>>> dependentSet =
            new AtomicReference<>(MultiSet.Immutable.of());

    // https://regex101.com/r/sGeGLs/1
    private static final Pattern RE_ID_SEG = Pattern.compile("\\/(?:\\\\\\\\|\\\\\\/|[^\\\\\\/])+");

    private Broker(String id, PRaffrayiSettings settings, ITypeChecker<S, L, D, R, T> typeChecker,
            IScopeImpl<S, D> scopeImpl, Iterable<L> edgeLabels, boolean rootChanged,
            @Nullable IUnitResult<S, L, D, Result<S, L, D, R, T>> previousResult, ICancel cancel, IProgress progress,
            IActorScheduler scheduler) {
        this.id = id;
        this.settings = settings;
        this.typeChecker = typeChecker;
        this.rootChanged = rootChanged;
        // If initial state has failures, discard it.
        if(previousResult != null) {
            if(!previousResult.allFailures().isEmpty()) {
                logger.warn("Initial state contains failures, discarding it.");
                for(Throwable ex : previousResult.allFailures()) {
                    logger.debug("* ", ex);
                }
                this.previousResult = null;
            } else {
                this.previousResult = previousResult;
            }
        } else {
            this.previousResult = null;
        }
        this.scopeImpl = scopeImpl;
        this.edgeLabels = CapsuleUtil.toSet(edgeLabels);
        this.cancel = cancel;
        this.progress = progress;

        this.scheduler = scheduler;
        this.system = new ActorSystem(scheduler);

        this.units = new HashMap<>();
        this.unfinishedUnits = new AtomicInteger();
        this.totalUnits = new AtomicInteger();

        this.delays = new HashMap<>();
        this.process = BrokerProcess.of();
        this.cmh = new ChandyMisraHaas<>(this, this::_deadlocked);
    }

    private IFuture<IUnitResult<S, L, D, Result<S, L, D, R, T>>> run() {
        final IActor<IUnit<S, L, D, Result<S, L, D, R, T>>> unit = system.add(id, TypeTag.of(IUnit.class),
                self -> new TypeCheckerUnit<S, L, D, R, T>(self, null, new UnitContext(self), typeChecker, edgeLabels,
                        previousResult == null || rootChanged, previousResult));
        addUnit(unit);

        final IFuture<IUnitResult<S, L, D, Result<S, L, D, R, T>>> unitResult =
                system.async(unit)._start(Collections.emptyList());

        final IFuture<IUnitResult<S, L, D, Result<S, L, D, R, T>>> runResult = unitResult.compose((r, ex) -> {
            finalizeUnit(unit, ex);
            return system.stop().compose((r2, ex2) -> CompletableFuture.completed(r, ex));
        });

        startWatcherThread();

        return runResult;
    }

    private void addUnit(IActorRef<? extends IUnit<S, L, D, ?>> unit) {
        unfinishedUnits.incrementAndGet();
        totalUnits.incrementAndGet();
        final ICompletable<IActorRef<? extends IUnit<S, L, D, ?>>> future = executeCritial(() -> {
            units.put(unit.id(), unit);
            return delays.remove(unit.id());
        });

        if(future != null) {
            future.complete(unit);
        }
    }

    private void finalizeUnit(IActorRef<? extends IUnit<S, L, D, ?>> unit, Throwable ex) {
        progress.work(1);
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

    ///////////////////////////////////////////////////////////////////////////
    // Context
    ///////////////////////////////////////////////////////////////////////////

    private class UnitContext implements IUnitContext<S, L, D> {

        private final IActor<? extends IUnit<S, L, D, ?>> self;

        public UnitContext(IActor<? extends IUnit<S, L, D, ?>> self) {
            this.self = self;
        }

        @Override public ICancel cancel() {
            return cancel;
        }

        @Override public PRaffrayiSettings settings() {
            return settings;
        }

        @Override public S makeScope(String name) {
            return scopeImpl.make(self.id(), name);
        }

        @Override public String scopeId(S scope) {
            return scopeImpl.id(scope);
        }

        @Override public D substituteScopes(D datum, BiMap.Immutable<S> substitution) {
            return scopeImpl.substituteScopes(datum, substitution.asMap());
        }

        @Override public Immutable<S> getScopes(D datum) {
            return scopeImpl.getScopes(datum);
        }

        @Override public D embed(S scope) {
            return scopeImpl.embed(scope);
        }

        @Override public Optional<BiMap.Immutable<S>> matchDatums(D currentDatum, D previousDatum) {
            return scopeImpl.matchDatums(currentDatum, previousDatum);
        }

        @Override public IFuture<IActorRef<? extends IUnit<S, L, D, ?>>> owner(S scope) {
            final String id = scopeImpl.id(scope);
            // No synchronization and deadlock handling when unit can be found.
            final IActorRef<? extends IUnit<S, L, D, ?>> unit;
            if((unit = units.get(id)) != null) {
                return CompletableFuture.completedFuture(unit);
            }
            return executeCritial(() -> {
                cmh.exec();
                final IFuture<IActorRef<? extends IUnit<S, L, D, ?>>> result = getActorRef(id);
                cmh.idle();
                return result;
            });
        }

        @Override public <U> Tuple2<IFuture<IUnitResult<S, L, D, U>>, IActorRef<? extends IUnit<S, L, D, U>>> add(
                String id, Function2<IActor<IUnit<S, L, D, U>>, IUnitContext<S, L, D>, IUnit<S, L, D, U>> unitProvider,
                List<S> rootScopes) {
            return executeCritial(() -> {
                cmh.exec();
                final IActorRef<IUnit<S, L, D, U>> unit = self.add(id, TypeTag.of(IUnit.class),
                        (subself) -> unitProvider.apply(subself, new UnitContext(subself)));
                addUnit(unit);
                final IFuture<IUnitResult<S, L, D, U>> unitResult = self.async(unit)._start(rootScopes);
                unitResult.whenComplete((r, ex) -> finalizeUnit(unit, ex));
                cmh.idle();
                return Tuple2.of(unitResult, unit);
            });
        }

        @Override public int parallelism() {
            return scheduler.parallelism();
        }

        @Override public IDeadlockProtocol<S, L, D> deadlock() {
            return Broker.this;
        }

    }

    ///////////////////////////////////////////////////////////////////////////
    // Parent Actors
    ///////////////////////////////////////////////////////////////////////////

    private IFuture<IActorRef<? extends IUnit<S, L, D, ?>>> getActorRef(String unitId) {
        final Matcher idMatcher = RE_ID_SEG.matcher(unitId);
        final List<String> segments = new ArrayList<>();
        while(idMatcher.find()) {
            segments.add(idMatcher.group());
        }
        return executeCritial(() -> getActorRef(segments));
    }

    private IFuture<IActorRef<? extends IUnit<S, L, D, ?>>> getActorRef(final List<String> segments) {
        final String unitId = String.join("", segments);
        if(segments.isEmpty()) {
            throw new IllegalStateException("Invalid unit id.");
        }

        // Should be synchronized by `parent(String)` already.

        final IActorRef<? extends IUnit<S, L, D, ?>> unit;
        if((unit = units.get(unitId)) != null) {
            return CompletableFuture.completedFuture(unit);
        } else {
            segments.remove(segments.size() - 1);
            return getActorRef(segments).thenCompose(parent -> {
                return executeCritial(() -> {
                    final UnitProcess<S, L, D> origin = new UnitProcess<>(parent);
                    dependentSet.getAndUpdate(ds -> ds.add(origin, 1));
                    final ICompletableFuture<IActorRef<? extends IUnit<S, L, D, ?>>> future =
                            delays.computeIfAbsent(unitId, key -> new CompletableFuture<>());
                    return future.whenComplete((ref, ex) -> {
                        executeCritial(() -> dependentSet.getAndUpdate(ds -> ds.remove(origin, 1)));
                    });
                });
            });
        }

    }

    ///////////////////////////////////////////////////////////////////////////
    // IDeadlockProtocol
    ///////////////////////////////////////////////////////////////////////////

    @Override public void _deadlocked(Set<IProcess<S, L, D>> nodes) {
        final Map<String, ICompletableFuture<IActorRef<? extends IUnit<S, L, D, ?>>>> delays = new HashMap<>();
        executeCritial(() -> {
            delays.putAll(this.delays);
            dependentSet.set(MultiSet.Immutable.of());
            cmh.exec();
        });
        delays.forEach((unit, future) -> future.completeExceptionally(
                new DeadlockException("Deadlocked while waiting for unit " + unit + " to be added.")));
    }

    @Override public void _deadlockQuery(IProcess<S, L, D> i, int m, IProcess<S, L, D> k) {
        executeCritial(() -> cmh.query(i, m, k));
    }

    @Override public void _deadlockReply(IProcess<S, L, D> i, int m, Set<IProcess<S, L, D>> R) {
        executeCritial(() -> cmh.reply(i, m, R));
    }

    @Override public IFuture<StateSummary<S, L, D>> _state() {
        // When broker is involved in a deadlock, there is a unit waiting for
        // another unit to be added. Just releasing such a unit is not safe.
        return CompletableFuture.completedFuture(StateSummary.restart(process, dependentSet()));
    }

    @Override public void _release() {
        // Since we always force a restart, this method should never be called.
        logger.error("Trying to release broker.");
        throw new IllegalStateException("Cannot release broker.");
    }

    @Override public void _restart() {
        // Ignore
    }

    ///////////////////////////////////////////////////////////////////////////
    // ChandyMisrahaas.Host
    ///////////////////////////////////////////////////////////////////////////

    @Override public IProcess<S, L, D> process() {
        return process;
    }

    @Override public Set<IProcess<S, L, D>> dependentSet() {
        return executeCritial(() -> dependentSet.get().elementSet());
    }

    @Override public void query(IProcess<S, L, D> k, IProcess<S, L, D> i, int m) {
        k.from(this)._deadlockQuery(i, m, process);
    }

    @Override public void reply(IProcess<S, L, D> k, IProcess<S, L, D> i, int m, Set<IProcess<S, L, D>> R) {
        k.from(this)._deadlockReply(i, m, R);
    }

    @Override public void assertOnActorThread() {
        if(!lock.isHeldByCurrentThread()) {
            throw new IllegalStateException("Broker lock is not held by current Thread.");
        }
    }

    public IDeadlockProtocol<S, L, D> deadlock(IActorRef<? extends IDeadlockProtocol<S, L, D>> unit) {
        return system.async(unit);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Helper methods
    ///////////////////////////////////////////////////////////////////////////

    private void executeCritial(Action0 action) {
        lock.lock();
        try {
            action.apply();
        } finally {
            lock.unlock();
        }
    }

    private <Q> Q executeCritial(Function0<Q> action) {
        lock.lock();
        try {
            return action.apply();
        } finally {
            lock.unlock();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Utilities
    ///////////////////////////////////////////////////////////////////////////

    public static <S, L, D, R extends IOutput<S, L, D>, T extends IState<S, L, D>>
            IFuture<IUnitResult<S, L, D, Result<S, L, D, R, T>>> run(String id, PRaffrayiSettings settings,
                    ITypeChecker<S, L, D, R, T> unitChecker, IScopeImpl<S, D> scopeImpl, Iterable<L> edgeLabels,
                    ICancel cancel, IProgress progress) {
        return run(id, settings, unitChecker, scopeImpl, edgeLabels, true, null, cancel, progress,
                Runtime.getRuntime().availableProcessors());
    }

    public static <S, L, D, R extends IOutput<S, L, D>, T extends IState<S, L, D>>
            IFuture<IUnitResult<S, L, D, Result<S, L, D, R, T>>>
            run(String id, PRaffrayiSettings settings, ITypeChecker<S, L, D, R, T> unitChecker,
                    IScopeImpl<S, D> scopeImpl, Iterable<L> edgeLabels, boolean changed,
                    IUnitResult<S, L, D, Result<S, L, D, R, T>> previousResult, ICancel cancel, IProgress progress) {
        return run(id, settings, unitChecker, scopeImpl, edgeLabels, changed, previousResult, cancel, progress,
                Runtime.getRuntime().availableProcessors());
    }

    public static <S, L, D, R extends IOutput<S, L, D>, T extends IState<S, L, D>>
            IFuture<IUnitResult<S, L, D, Result<S, L, D, R, T>>> run(String id, PRaffrayiSettings settings,
                    ITypeChecker<S, L, D, R, T> typeChecker, IScopeImpl<S, D> scopeImpl, Iterable<L> edgeLabels,
                    boolean changed, IUnitResult<S, L, D, Result<S, L, D, R, T>> previousResult, ICancel cancel,
                    IProgress progress, int parallelism) {
        return new Broker<>(id, settings, typeChecker, scopeImpl, edgeLabels, changed, previousResult, cancel, progress,
                new WorkStealingScheduler(parallelism)).run();
    }

    public static <S, L, D, R extends IOutput<S, L, D>, T extends IState<S, L, D>>
            IFuture<IUnitResult<S, L, D, Result<S, L, D, R, T>>> debug(String id, PRaffrayiSettings settings,
                    ITypeChecker<S, L, D, R, T> typeChecker, IScopeImpl<S, D> scopeImpl, Iterable<L> edgeLabels,
                    ICancel cancel, double preemptProbability, int scheduleDelayBoundMillis) {
        return debug(id, settings, typeChecker, scopeImpl, edgeLabels, true, null, cancel,
                Runtime.getRuntime().availableProcessors(), preemptProbability, scheduleDelayBoundMillis);
    }

    public static <S, L, D, R extends IOutput<S, L, D>, T extends IState<S, L, D>>
            IFuture<IUnitResult<S, L, D, Result<S, L, D, R, T>>> debug(String id, PRaffrayiSettings settings,
                    ITypeChecker<S, L, D, R, T> typeChecker, IScopeImpl<S, D> scopeImpl, Iterable<L> edgeLabels,
                    boolean changed, IUnitResult<S, L, D, Result<S, L, D, R, T>> previousResult, ICancel cancel,
                    double preemptProbability, int scheduleDelayBoundMillis) {
        return debug(id, settings, typeChecker, scopeImpl, edgeLabels, changed, previousResult, cancel,
                Runtime.getRuntime().availableProcessors(), preemptProbability, scheduleDelayBoundMillis);
    }

    public static <S, L, D, R extends IOutput<S, L, D>, T extends IState<S, L, D>>
            IFuture<IUnitResult<S, L, D, Result<S, L, D, R, T>>> debug(String id, PRaffrayiSettings settings,
                    ITypeChecker<S, L, D, R, T> typeChecker, IScopeImpl<S, D> scopeImpl, Iterable<L> edgeLabels,
                    boolean changed, IUnitResult<S, L, D, Result<S, L, D, R, T>> previousResult, ICancel cancel,
                    int parallelism, double preemptProbability, int scheduleDelayBoundMillis) {
        return new Broker<>(id, settings, typeChecker, scopeImpl, edgeLabels, changed, previousResult, cancel,
                new NullProgress(), new WonkyScheduler(parallelism, preemptProbability, scheduleDelayBoundMillis))
                        .run();
    }

}
