package mb.statix.concurrent.p_raffrayi.impl;

import static com.google.common.collect.Streams.stream;

import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.metaborg.util.Ref;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.SetMultimap;
import mb.nabl2.util.CapsuleUtil;
import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation3;
import mb.nabl2.util.collections.MultiSet;
import mb.statix.concurrent.actors.IActor;
import mb.statix.concurrent.actors.IActorMonitor;
import mb.statix.concurrent.actors.IActorRef;
import mb.statix.concurrent.actors.TypeTag;
import mb.statix.concurrent.actors.deadlock.Clock;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.ICompletable;
import mb.statix.concurrent.actors.futures.ICompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.DeadlockException;
import mb.statix.concurrent.p_raffrayi.ITypeChecker;
import mb.statix.concurrent.p_raffrayi.IUnitResult;
import mb.statix.concurrent.p_raffrayi.impl.tokens.CloseLabel;
import mb.statix.concurrent.p_raffrayi.impl.tokens.CloseScope;
import mb.statix.concurrent.p_raffrayi.impl.tokens.IWaitFor;
import mb.statix.concurrent.p_raffrayi.impl.tokens.InitScope;
import mb.statix.concurrent.p_raffrayi.impl.tokens.Query;
import mb.statix.concurrent.p_raffrayi.impl.tokens.TypeCheckerResult;
import mb.statix.concurrent.p_raffrayi.impl.tokens.TypeCheckerState;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataLeq;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataLeqInternal;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataWf;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataWfInternal;
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelOrder;
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelWF;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.path.IScopePath;
import mb.statix.scopegraph.reference.EdgeOrData;
import mb.statix.scopegraph.reference.Env;
import mb.statix.scopegraph.reference.ScopeGraph;
import mb.statix.scopegraph.terms.path.Paths;

class Unit<S, L, D, R> implements IUnit<S, L, D, R>, IActorMonitor {

    private static final ILogger logger = LoggerUtils.logger(IUnit.class);

    private final TypeTag<IUnit<S, L, D, R>> TYPE = TypeTag.of(IUnit.class);

    private final IActor<? extends IUnit<S, L, D, R>> self;
    private final @Nullable IActorRef<? extends IUnit<S, L, D, R>> parent;
    private final IUnitContext<S, L, D, R> context;
    private final ITypeChecker<S, L, D, R> typeChecker;

    private Clock<IActorRef<? extends IUnit<S, L, D, R>>> clock;
    private UnitState state;

    private final Ref<R> analysis;
    private final List<Throwable> failures;
    private final ICompletableFuture<IUnitResult<S, L, D, R>> unitResult;

    private final Ref<IScopeGraph.Immutable<S, L, D>> scopeGraph;
    private final Set.Transient<S> scopes;
    private final IRelation3.Transient<S, EdgeOrData<L>, Delay> delays;

    private final MultiSet.Transient<String> scopeNameCounters;

    Unit(IActor<? extends IUnit<S, L, D, R>> self, @Nullable IActorRef<? extends IUnit<S, L, D, R>> parent,
            IUnitContext<S, L, D, R> context, ITypeChecker<S, L, D, R> unitChecker, Iterable<L> edgeLabels) {
        this.self = self;
        this.parent = parent;
        this.context = context;
        this.typeChecker = unitChecker;

        this.clock = Clock.of();
        this.state = UnitState.INIT;

        this.analysis = new Ref<>();
        this.failures = Lists.newArrayList();
        this.unitResult = new CompletableFuture<>();

        this.scopeGraph = new Ref<>(ScopeGraph.Immutable.of(edgeLabels));
        this.scopes = Set.Transient.of();
        this.delays = HashTrieRelation3.Transient.of();

        this.scopeNameCounters = MultiSet.Transient.of();

        self.addMonitor(this);
    }

    ///////////////////////////////////////////////////////////////////////////
    // IBroker2UnitProtocol interface, called by IBroker implementations
    ///////////////////////////////////////////////////////////////////////////

    @Override public IFuture<IUnitResult<S, L, D, R>> _start(@Nullable S root) {
        assertInState(UnitState.INIT);

        state = UnitState.ACTIVE;
        if(root != null) {
            doAddLocalShare(self, root); // FIXME If we doAddShare here, the system deadlocks but is not picked up by DLM
        }

        // run() after inits are initialized before run, since unitChecker
        // can immediately call methods, that are executed synchronously

        final ICompletableFuture<R> typeCheckerResult = new CompletableFuture<>();
        final TypeCheckerResult<S, L, D> result = TypeCheckerResult.of(typeCheckerResult);
        waitFor(result, self);
        self.schedule(this.typeChecker.run(this, root)).whenComplete(typeCheckerResult::complete);
        typeCheckerResult.whenComplete((r, ex) -> {
            if(ex != null) {
                failures.add(ex);
            } else {
                analysis.set(r);
            }
            granted(result, self);
            tryFinish();
        });

        return unitResult;
    }

    ///////////////////////////////////////////////////////////////////////////
    // ITypeCheckerContext interface, called by ITypeChecker implementations
    ///////////////////////////////////////////////////////////////////////////

    // NB. Invoke methods via `local` so that we have the same scheduling & ordering
    // guarantees as for remote calls.

    @Override public String id() {
        return self.id();
    }

    @Override public void add(String id, ITypeChecker<S, L, D, R> unitChecker, S root) {
        assertInState(UnitState.ACTIVE);
        assertOwnOrSharedScope(root);

        final IActorRef<? extends IUnit<S, L, D, R>> subunit = context.add(id, unitChecker, root);

        doAddShare(subunit, root);
    }

    @Override public void initRoot(S root, Iterable<L> labels, boolean sharing) {
        assertInState(UnitState.ACTIVE);

        final List<EdgeOrData<L>> edges = stream(labels).map(EdgeOrData::edge).collect(Collectors.toList());

        doInitShare(self, root, edges, sharing);
    }

    @Override public S freshScope(String baseName, Iterable<L> edgeLabels, boolean data, boolean sharing) {
        assertInState(UnitState.ACTIVE);

        final String name = baseName.replace("-", "_");
        final int n = scopeNameCounters.add(name);
        final S scope = context.makeScope(name + "-" + n);

        final List<EdgeOrData<L>> labels = Lists.newArrayList();
        for(L l : edgeLabels) {
            labels.add(EdgeOrData.edge(l));
        }
        if(data) {
            labels.add(EdgeOrData.data());
        }

        doAddLocalShare(self, scope);
        doInitShare(self, scope, labels, sharing);

        return scope;
    }

    @Override public void setDatum(S scope, D datum) {
        assertInState(UnitState.ACTIVE);

        final EdgeOrData<L> edge = EdgeOrData.data();
        assertLabelOpen(scope, edge);

        scopeGraph.set(scopeGraph.get().setDatum(scope, datum));
        doCloseLabel(self, scope, edge);
    }

    @Override public void addEdge(S source, L label, S target) {
        assertInState(UnitState.ACTIVE);

        doAddEdge(self, source, label, target);
    }

    @Override public void closeEdge(S source, L label) {
        assertInState(UnitState.ACTIVE);

        doCloseLabel(self, source, EdgeOrData.edge(label));
    }

    @Override public void closeScope(S scope) {
        assertInState(UnitState.ACTIVE);

        doCloseScope(self, scope);
    }

    @Override public IFuture<Set<IResolutionPath<S, L, D>>> query(S scope, LabelWF<L> labelWF, LabelOrder<L> labelOrder,
            DataWf<D> dataWF, DataLeq<D> dataEquiv, DataWfInternal<D> dataWfInternal,
            DataLeqInternal<D> dataEquivInternal) {
        assertInState(UnitState.ACTIVE);

        final IScopePath<S, L> path = Paths.empty(scope);
        final IFuture<Env<S, L, D>> result =
                doQuery(self, path, labelWF, labelOrder, dataWF, dataEquiv, dataWfInternal, dataEquivInternal);
        final Query<S, L, D> wf = Query.of(path, labelWF, dataWF, labelOrder, dataEquiv, result);
        waitFor(wf, self);
        return self.schedule(result).whenComplete((env, ex) -> {
            granted(wf, self);
            tryFinish();
        }).thenApply(CapsuleUtil::toSet);
    }

    ///////////////////////////////////////////////////////////////////////////
    // IUnit2UnitProtocol interface, called by IUnit implementations
    ///////////////////////////////////////////////////////////////////////////

    @Override public void _initShare(S scope, Iterable<EdgeOrData<L>> edges, boolean sharing) {
        doInitShare(self.sender(TYPE), scope, edges, sharing);
    }

    @Override public void _addShare(S scope) {
        doAddShare(self.sender(TYPE), scope);
    }

    @Override public void _doneSharing(S scope) {
        doCloseScope(self.sender(TYPE), scope);
    }

    @Override public final void _addEdge(S source, L label, S target) {
        doAddEdge(self.sender(TYPE), source, label, target);
    }

    @Override public void _closeEdge(S scope, EdgeOrData<L> edge) {
        doCloseLabel(self.sender(TYPE), scope, edge);
    }

    @Override public final IFuture<Env<S, L, D>> _query(IScopePath<S, L> path, LabelWF<L> labelWF, DataWf<D> dataWF,
            LabelOrder<L> labelOrder, DataLeq<D> dataEquiv) {
        return doQuery(self.sender(TYPE), path, labelWF, labelOrder, dataWF, dataEquiv, null, null);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implementations -- independent of message handling context
    ///////////////////////////////////////////////////////////////////////////

    private final void doAddLocalShare(IActorRef<? extends IUnit<S, L, D, R>> sender, S scope) {
        scopes.__insert(scope);
        waitFor(InitScope.of(scope), sender);
    }

    private final void doAddShare(IActorRef<? extends IUnit<S, L, D, R>> sender, S scope) {
        doAddLocalShare(sender, scope);

        final IActorRef<? extends IUnit<S, L, D, R>> owner = context.owner(scope);
        if(!owner.equals(self)) {
            self.async(parent)._addShare(scope);
        }
    }

    private final void doInitShare(IActorRef<? extends IUnit<S, L, D, R>> sender, S scope,
            Iterable<EdgeOrData<L>> edges, boolean sharing) {
        assertOwnOrSharedScope(scope);

        granted(InitScope.of(scope), sender);
        for(EdgeOrData<L> edge : edges) {
            waitFor(CloseLabel.of(scope, edge), sender);
        }
        if(sharing) {
            waitFor(CloseScope.of(scope), sender);
        }
        tryFinish();

        final IActorRef<? extends IUnit<S, L, D, R>> owner = context.owner(scope);
        if(owner.equals(self)) {
            if(isScopeInitialized(scope)) {
                releaseDelays(scope);
            }
        } else {
            self.async(parent)._initShare(scope, edges, sharing);
        }
    }

    private final void doCloseScope(IActorRef<? extends IUnit<S, L, D, R>> sender, S scope) {
        assertOwnOrSharedScope(scope);

        granted(CloseScope.of(scope), sender);
        tryFinish();

        final IActorRef<? extends IUnit<S, L, D, R>> owner = context.owner(scope);
        if(owner.equals(self)) {
            if(isScopeInitialized(scope)) {
                releaseDelays(scope);
            }
        } else {
            self.async(parent)._doneSharing(scope);
        }
    }

    private final void doCloseLabel(IActorRef<? extends IUnit<S, L, D, R>> sender, S scope, EdgeOrData<L> edge) {
        assertOwnOrSharedScope(scope);

        granted(CloseLabel.of(scope, edge), sender);
        tryFinish();

        final IActorRef<? extends IUnit<S, L, D, R>> owner = context.owner(scope);
        if(owner.equals(self)) {
            if(isEdgeClosed(scope, edge)) {
                releaseDelays(scope, edge);
            }
        } else {
            self.async(parent)._closeEdge(scope, edge);
        }
    }

    private final void doAddEdge(IActorRef<? extends IUnit<S, L, D, R>> sender, S source, L label, S target) {
        assertOwnOrSharedScope(source);
        assertLabelOpen(source, EdgeOrData.edge(label));

        scopeGraph.set(scopeGraph.get().addEdge(source, label, target));

        final IActorRef<? extends IUnit<S, L, D, R>> owner = context.owner(source);
        if(!owner.equals(self)) {
            self.async(parent)._addEdge(source, label, target);
        }
    }

    private final IFuture<Env<S, L, D>> doQuery(IActorRef<? extends IUnit<S, L, D, R>> sender, IScopePath<S, L> path,
            LabelWF<L> labelWF, LabelOrder<L> labelOrder, DataWf<D> dataWF, DataLeq<D> dataEquiv,
            DataWfInternal<D> dataWfInternal, DataLeqInternal<D> dataEquivInternal) {
        logger.debug("got _query from {}", sender);
        final boolean external = !sender.equals(self);

        final NameResolution<S, L, D> nr = new NameResolution<S, L, D>(scopeGraph.get().getEdgeLabels(), labelOrder) {

            // FIXME Eliminate tryFinish in resolution predicates. To eliminate tryFinish here,
            //       there needs to be a difference between local waitFors (originating from this
            //       unit's type checker), and waitFors that are the result of queries (but won't influence the
            //       local state). Method isWaiting should then only consider the local wait-fors when checking
            //       if the unit completed. Then these checks are not necessary anymore.

            @Override public Optional<IFuture<Env<S, L, D>>> externalEnv(IScopePath<S, L> path, LabelWF<L> re,
                    LabelOrder<L> labelOrder) {
                final S scope = path.getTarget();
                final IActorRef<? extends IUnit<S, L, D, R>> owner = context.owner(scope);
                if(owner.equals(self)) {
                    logger.debug("local env {}", scope);
                    return Optional.empty();
                } else {
                    logger.debug("remote env {} at {}", scope, owner);
                    // this code mirrors query(...)
                    final IFuture<Env<S, L, D>> result =
                            self.async(owner)._query(path, re, dataWF, labelOrder, dataEquiv);
                    final Query<S, L, D> wf = Query.of(path, re, dataWF, labelOrder, dataEquiv, result);
                    waitFor(wf, owner);
                    return Optional.of(result.whenComplete((r, ex) -> {
                        logger.debug("got answer from {}", sender);
                        granted(wf, owner);
                        tryFinish();
                    }));
                }
            }

            @Override protected IFuture<Optional<D>> getDatum(S scope) {
                return isComplete(scope, EdgeOrData.data(), sender).thenCompose(__ -> {
                    final Optional<D> datum;
                    if(!(datum = scopeGraph.get().getData(scope)).isPresent()) {
                        return CompletableFuture.completedFuture(Optional.empty());
                    }
                    if(external) {
                        logger.debug("require external rep for {}", datum.get());
                        final ICompletableFuture<D> externalRep = new CompletableFuture<>();
                        typeChecker.getExternalDatum(datum.get()).whenComplete(externalRep::complete);
                        final IWaitFor<S, L, D> token =
                                TypeCheckerState.of(sender, ImmutableList.of(datum.get()), externalRep);
                        waitFor(token, self);
                        return externalRep.whenComplete((rep, ex) -> {
                            self.assertOnActorThread();
                            granted(token, self);
                            tryFinish();
                        }).thenApply(rep -> {
                            logger.debug("got external rep {} for {}", rep, datum.get());
                            return Optional.of(rep);
                        });
                    } else {
                        return CompletableFuture.completedFuture(datum);
                    }
                });
            }

            @Override protected IFuture<Iterable<S>> getEdges(S scope, L label) {
                return isComplete(scope, EdgeOrData.edge(label), sender).thenApply(__ -> {
                    return scopeGraph.get().getEdges(scope, label);
                });
            }

            @Override protected IFuture<Boolean> dataWf(D d, ICancel cancel) throws InterruptedException {
                if(external || dataWfInternal == null) {
                    return CompletableFuture.completedFuture(dataWF.wf(d, cancel));
                } else {
                    final ICompletableFuture<Boolean> result = new CompletableFuture<>();
                    dataWfInternal.wf(d, cancel).whenComplete(result::complete);
                    final TypeCheckerState<S, L, D> token = TypeCheckerState.of(sender, ImmutableList.of(d), result);
                    waitFor(token, self);
                    return result.whenComplete((r, ex) -> {
                        self.assertOnActorThread();
                        granted(token, self);
                        tryFinish();
                    });
                }
            }

            @Override protected IFuture<Boolean> dataLeq(D d1, D d2, ICancel cancel) throws InterruptedException {
                if(external || dataEquivInternal == null) {
                    return CompletableFuture.completedFuture(dataEquiv.leq(d1, d2, cancel));
                } else {
                    final ICompletableFuture<Boolean> result = new CompletableFuture<>();
                    dataEquivInternal.leq(d1, d2, cancel).whenComplete(result::complete);
                    final TypeCheckerState<S, L, D> token =
                            TypeCheckerState.of(sender, ImmutableList.of(d1, d2), result);
                    waitFor(token, self);
                    return result.whenComplete((r, ex) -> {
                        self.assertOnActorThread();
                        granted(token, self);
                        tryFinish();
                    });
                }
            }

        };

        final IFuture<Env<S, L, D>> result = nr.env(path, labelWF, context.cancel());
        result.whenComplete((env, ex) -> {
            logger.debug("have answer for {}", sender);
        });
        return result;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Wait fors & finalization
    ///////////////////////////////////////////////////////////////////////////

    private void waitFor(IWaitFor<S, L, D> token, IActorRef<? extends IUnit<S, L, D, R>> unit) {
        context.waitFor(token, unit);
    }

    private void granted(IWaitFor<S, L, D> token, IActorRef<? extends IUnit<S, L, D, R>> unit) {
        self.assertOnActorThread();
        context.granted(token, unit);
    }

    /**
     * Checks if the unit is finished, or still waiting on something. Must be called after all grants. Note that if a
     * wait-for is granted, and others are introduced, this method must be called after all have been processed, or it
     * may conclude prematurely that the unit is done.
     */
    private void tryFinish() {
        boolean notDone = state.equals(UnitState.INIT) || state.equals(UnitState.ACTIVE);
        if(notDone && !context.isWaiting()) {
            state = UnitState.DONE;
            unitResult.complete(UnitResult.of(scopeGraph.get(), analysis.get(), failures));
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Resolution & delays
    ///////////////////////////////////////////////////////////////////////////

    private void releaseDelays(S scope) {
        for(Entry<EdgeOrData<L>, Delay> entry : delays.get(scope)) {
            final EdgeOrData<L> edge = entry.getKey();
            if(!context.isWaitingFor(CloseLabel.of(scope, edge))) {
                final Delay delay = entry.getValue();
                logger.debug("released {} on {}(/{})", delay.future, scope, edge);
                delays.remove(scope, edge, delay);
                self.complete(delay.future, org.metaborg.util.unit.Unit.unit, null);
            }
        }
    }

    private void releaseDelays(S scope, EdgeOrData<L> edge) {
        for(Delay delay : delays.get(scope, edge)) {
            logger.debug("released {} on {}/{}", delay.future, scope, edge);
            delays.remove(scope, edge, delay);
            self.complete(delay.future, org.metaborg.util.unit.Unit.unit, null);
        }
    }

    private boolean isScopeInitialized(S scope) {
        return !context.isWaitingFor(InitScope.of(scope)) && !context.isWaitingFor(CloseScope.of(scope));
    }

    private boolean isEdgeClosed(S scope, EdgeOrData<L> edge) {
        return isScopeInitialized(scope) && !context.isWaitingFor(CloseLabel.of(scope, edge));
    }

    private IFuture<org.metaborg.util.unit.Unit> isComplete(S scope, EdgeOrData<L> edge,
            IActorRef<? extends IUnit<S, L, D, R>> sender) {
        if(isEdgeClosed(scope, edge)) {
            return CompletableFuture.completedFuture(org.metaborg.util.unit.Unit.unit);
        } else {
            final CompletableFuture<org.metaborg.util.unit.Unit> result = new CompletableFuture<>();
            delays.put(scope, edge, new Delay(result, sender));
            return result;
        }
    }

    private class Delay {

        public final ICompletableFuture<org.metaborg.util.unit.Unit> future;
        public final IActorRef<? extends IUnit<S, L, D, R>> sender;

        Delay(ICompletableFuture<org.metaborg.util.unit.Unit> future, IActorRef<? extends IUnit<S, L, D, R>> sender) {
            this.future = future;
            this.sender = sender;
        }

        @Override public String toString() {
            return "Delay{future=" + future + ",sender=" + sender + "}";
        }

    }

    ///////////////////////////////////////////////////////////////////////////
    // Deadlock handling
    ///////////////////////////////////////////////////////////////////////////

    @Override public void _deadlocked(Clock<IActorRef<? extends IUnit<S, L, D, R>>> clock,
            java.util.Set<IActorRef<? extends IUnit<S, L, D, R>>> nodes,
            SetMultimap.Immutable<IActorRef<? extends IUnit<S, L, D, R>>, IWaitFor<S, L, D>> waitFors) {
        self.assertOnActorThread();

        if(!this.clock.equals(clock)) {
            // Deadlock is detected even when not all units are suspended---only the component is enough.
            // This means the unit could receive messages between suspend and the deadlock. However, they
            // should not influence the deadlocked state, as received queries from other units (even when
            // forwarded, thus creating a wait-for), do not cause progress on this unit. Eventually, we deadlock
            // would be there again.
            logger.debug("stale deadlock");
            return;
        }
        this.clock = this.clock.sent(self).delivered(self); // increase local clock to ensure deadlock detection after this
        if(nodes.size() == 1 && nodes.contains(self)) {
            if(!failDelays(nodes, waitFors)) {
                failAll(nodes, waitFors);
            }
        } else {
            failDelays(nodes, waitFors);
        }
    }

    /**
     * Fail delays that are part of the deadlock.
     */
    private boolean failDelays(java.util.Set<IActorRef<? extends IUnit<S, L, D, R>>> nodes,
            SetMultimap.Immutable<IActorRef<? extends IUnit<S, L, D, R>>, IWaitFor<S, L, D>> waitFors) {
        final Set.Transient<ICompletable<?>> deadlocked = Set.Transient.of();
        for(Delay delay : delays.inverse().keySet()) {
            if(nodes.contains(delay.sender)) {
                logger.info("{} fail {}", self, delay);
                delays.inverse().remove(delay);
                deadlocked.__insert(delay.future);
            }
        }
        for(IWaitFor<S, L, D> wf : waitFors.values()) {
            // @formatter:off
            wf.visit(IWaitFor.cases(
                initScope -> {},
                closeScope -> {},
                closeLabel -> {},
                query -> {},
                result  -> {},
                typeCheckerState -> {
                    if(nodes.contains(typeCheckerState.origin())) {
                        logger.info("{} fail {}", self, typeCheckerState);
                        deadlocked.__insert(typeCheckerState.future());
                    }
                }
            ));
            // @formatter:on
        }
        for(ICompletable<?> future : deadlocked) {
            self.complete(future, null, new DeadlockException("Type checker deadlocked."));
        }
        return !deadlocked.isEmpty();
    }

    private void failAll(java.util.Set<IActorRef<? extends IUnit<S, L, D, R>>> nodes,
            SetMultimap.Immutable<IActorRef<? extends IUnit<S, L, D, R>>, IWaitFor<S, L, D>> waitFors) {
        // Grants are processed immediately, while the result failure is scheduled.
        // This ensures that all labels are closed by the time the result failure is processed.
        for(IWaitFor<S, L, D> wf : waitFors.values()) {
            // @formatter:off
            wf.visit(IWaitFor.cases(
                initScope -> {
                    failures.add(new DeadlockException(initScope.toString()));
                    granted(initScope, self);
                    if(!context.owner(initScope.scope()).equals(self)) {
                        self.async(parent)._initShare(initScope.scope(), Set.Immutable.of(), false);
                    }
                    releaseDelays(initScope.scope());
                },
                closeScope -> {
                    failures.add(new DeadlockException(closeScope.toString()));
                    granted(closeScope, self);
                    if(!context.owner(closeScope.scope()).equals(self)) {
                        self.async(parent)._doneSharing(closeScope.scope());
                    }
                    releaseDelays(closeScope.scope());
                },
                closeLabel -> {
                    failures.add(new DeadlockException(closeLabel.toString()));
                    granted(closeLabel, self);
                    if(!context.owner(closeLabel.scope()).equals(self)) {
                        self.async(parent)._closeEdge(closeLabel.scope(), closeLabel.label());
                    }
                    releaseDelays(closeLabel.scope(), closeLabel.label());
                },
                query -> {
                    logger.error("Unexpected remaining query: " + query);
                    throw new IllegalStateException("Unexpected remaining query: " + query);
                },
                result  -> {
                    self.complete(result.future(), null, new DeadlockException("Type checker did not return a result."));
                },
                typeCheckerState -> {
                    if(nodes.contains(typeCheckerState.origin())) {
                        logger.error("Unexpected remaining internal state: " + typeCheckerState);
                        throw new IllegalStateException("Unexpected remaining internal state: " + typeCheckerState);
                    }
                    self.complete(typeCheckerState.future(), null, new DeadlockException("Type checker deadlocked."));
                }
            ));
            // @formatter:on
            tryFinish();
        }
    }

    @SuppressWarnings("unchecked") @Override public void sent(IActor<?> self, IActorRef<?> target,
            java.util.Set<String> tags) {
        if(tags.contains("stuckness")) {
            clock = clock.sent((IActorRef<? extends IUnit<S, L, D, R>>) target);
            logger.debug("updated clock: {}", clock);
        }
    }

    @SuppressWarnings("unchecked") @Override public void delivered(IActor<?> self, IActorRef<?> source,
            java.util.Set<String> tags) {
        if(tags.contains("stuckness")) {
            clock = clock.delivered((IActorRef<? extends IUnit<S, L, D, R>>) source);
            logger.debug("updated clock: {}", clock);
        }
    }

    @Override public void suspended(IActor<?> self) {
        if(state.equals(UnitState.INIT)) {
            // Ignore suspends before the start message is processed. This is important
            // because otherwise deadlock detection may fail. The suspend after the start
            // message is then ignored if no messages were exchanged. However, even without
            // messages the unit may need to initialize its root scope, and failing to do so
            // should be detected.
            return;
        }
        context.suspended(clock);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Assertions
    ///////////////////////////////////////////////////////////////////////////

    private void assertInState(UnitState s) {
        if(!state.equals(s)) {
            logger.error("Expected state {}, was {}", s, state);
            throw new IllegalStateException("Expected state " + s + ", was " + state);
        }
    }

    private void assertOwnOrSharedScope(S scope) {
        if(!scopes.contains(scope)) {
            logger.error("Scope {} is not owned or shared.", scope);
            throw new IllegalArgumentException("Scope " + scope + " is not owned or shared.");
        }
    }

    private void assertLabelOpen(S scope, EdgeOrData<L> edge) {
        assertOwnOrSharedScope(scope);
        if(isEdgeClosed(scope, edge)) {
            logger.error("Label {}/{} is not open on {}.", scope, edge, self);
            throw new IllegalArgumentException("Label " + scope + "/" + edge + " is not open on " + self + ".");
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // hashCode
    ///////////////////////////////////////////////////////////////////////////

    private volatile int hashCode;

    @Override public int hashCode() {
        int result = hashCode;
        if(result == 0) {
            result = System.identityHashCode(this);
            hashCode = result;
        }
        return result;
    }

    ///////////////////////////////////////////////////////////////////////////
    // toString
    ///////////////////////////////////////////////////////////////////////////

    @Override public String toString() {
        return "Unit{" + self.id() + "}";
    }

}