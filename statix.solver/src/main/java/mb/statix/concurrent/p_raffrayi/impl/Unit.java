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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import io.usethesource.capsule.Set;
import mb.nabl2.util.CapsuleUtil;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation3;
import mb.nabl2.util.collections.MultiSet;
import mb.nabl2.util.collections.MultiSetMap;
import mb.statix.concurrent.actors.IActor;
import mb.statix.concurrent.actors.IActorMonitor;
import mb.statix.concurrent.actors.IActorRef;
import mb.statix.concurrent.actors.IActorStats;
import mb.statix.concurrent.actors.TypeTag;
import mb.statix.concurrent.actors.deadlock.ChandyMisraHaas;
import mb.statix.concurrent.actors.deadlock.ChandyMisraHaas.Host;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.ICompletable;
import mb.statix.concurrent.actors.futures.ICompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.DeadlockException;
import mb.statix.concurrent.p_raffrayi.ITypeChecker;
import mb.statix.concurrent.p_raffrayi.IUnitResult;
import mb.statix.concurrent.p_raffrayi.IUnitStats;
import mb.statix.concurrent.p_raffrayi.TypeCheckingFailedException;
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
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelWf;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.path.IScopePath;
import mb.statix.scopegraph.reference.EdgeOrData;
import mb.statix.scopegraph.reference.Env;
import mb.statix.scopegraph.reference.ScopeGraph;
import mb.statix.scopegraph.terms.path.Paths;

class Unit<S, L, D, R> implements IUnit<S, L, D, R>, IActorMonitor, Host<IActorRef<? extends IUnit<S, L, D, ?>>> {

    private static final ILogger logger = LoggerUtils.logger(IUnit.class);

    private final TypeTag<IUnit<S, L, D, ?>> TYPE = TypeTag.of(IUnit.class);

    private final IActor<? extends IUnit<S, L, D, R>> self;
    private final @Nullable IActorRef<? extends IUnit<S, L, D, ?>> parent;
    private final IUnitContext<S, L, D> context;
    private final ITypeChecker<S, L, D, R> typeChecker;

    private volatile UnitState state;
    private final ChandyMisraHaas<IActorRef<? extends IUnit<S, L, D, ?>>> cmh;

    private final Ref<R> analysis;
    private final List<Throwable> failures;
    private final ICompletableFuture<IUnitResult<S, L, D, R>> unitResult;

    private final Ref<IScopeGraph.Immutable<S, L, D>> scopeGraph;
    private final Set.Immutable<L> edgeLabels;
    private final Set.Transient<S> scopes;
    private final IRelation3.Transient<S, EdgeOrData<L>, Delay> delays;

    private final MultiSet.Transient<String> scopeNameCounters;

    private final Stats stats;

    Unit(IActor<? extends IUnit<S, L, D, R>> self, @Nullable IActorRef<? extends IUnit<S, L, D, ?>> parent,
            IUnitContext<S, L, D> context, ITypeChecker<S, L, D, R> unitChecker, Iterable<L> edgeLabels) {
        this.self = self;
        this.parent = parent;
        this.context = context;
        this.typeChecker = unitChecker;

        this.state = UnitState.INIT;
        this.cmh = new ChandyMisraHaas<>(this, this::handleDeadlock);

        this.analysis = new Ref<>();
        this.failures = Lists.newArrayList();
        this.unitResult = new CompletableFuture<>();

        this.scopeGraph = new Ref<>(ScopeGraph.Immutable.of());
        this.edgeLabels = CapsuleUtil.toSet(edgeLabels);
        this.scopes = CapsuleUtil.transientSet();
        this.delays = HashTrieRelation3.Transient.of();

        this.scopeNameCounters = MultiSet.Transient.of();

        this.stats = new Stats(self.stats());
    }

    ///////////////////////////////////////////////////////////////////////////
    // IBroker2UnitProtocol interface, called by IBroker implementations
    ///////////////////////////////////////////////////////////////////////////

    @Override public IFuture<IUnitResult<S, L, D, R>> _start(List<S> rootScopes) {
        assertInState(UnitState.INIT);
        resume();

        state = UnitState.ACTIVE;
        for(S rootScope : CapsuleUtil.toSet(rootScopes)) {
            scopes.__insert(rootScope);
            doAddLocalShare(self, rootScope);
        }

        // run() after inits are initialized before run, since unitChecker
        // can immediately call methods, that are executed synchronously

        final ICompletableFuture<R> typeCheckerResult = new CompletableFuture<>();
        final TypeCheckerResult<S, L, D> result = TypeCheckerResult.of(self, typeCheckerResult);
        waitFor(result, self);
        self.schedule(this.typeChecker.run(this, rootScopes)).whenComplete(typeCheckerResult::complete);
        typeCheckerResult.whenComplete((r, ex) -> {
            logger.debug("{} type checker finished", this);
            resume(); // FIXME necessary?
            if(ex != null) {
                failures.add(ex);
            } else {
                analysis.set(r);
            }
            granted(result, self);
            final MultiSet.Immutable<IWaitFor<S, L, D>> selfTokens = getTokens(self);
            if(!selfTokens.isEmpty()) {
                logger.debug("{} returned while waiting on {}", self, selfTokens);
            }
            // tryFinish();
        });

        return unitResult;
    }

    ///////////////////////////////////////////////////////////////////////////
    // IActorMonitor
    ///////////////////////////////////////////////////////////////////////////

    @Override public void resumed() {
        logger.debug("{} resumed", this);
    }

    private void resume() {
        if(cmh.exec()) {
        }
    }

    @Override public void suspended() {
        logger.debug("{} suspended", this);
        if(cmh.idle()) {
        }
        tryFinish();
    }

    @Override public void stopped(Throwable ex) {
        if(!state.equals(UnitState.DONE)) {
            if(ex != null && ex instanceof InterruptedException) {
                unitResult.completeExceptionally(ex);
            } else {
                unitResult.completeExceptionally(new TypeCheckingFailedException(this + " stopped.", ex));
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // ITypeCheckerContext interface, called by ITypeChecker implementations
    ///////////////////////////////////////////////////////////////////////////

    // NB. Invoke methods via `local` so that we have the same scheduling & ordering
    // guarantees as for remote calls.

    @Override public String id() {
        return self.id();
    }

    @Override public <Q> IFuture<IUnitResult<S, L, D, Q>> add(String id, ITypeChecker<S, L, D, Q> unitChecker,
            List<S> rootScopes) {
        assertInState(UnitState.ACTIVE);
        for(S rootScope : rootScopes) {
            assertOwnOrSharedScope(rootScope);
        }

        final Tuple2<IFuture<IUnitResult<S, L, D, Q>>, IActorRef<? extends IUnit<S, L, D, Q>>> result_subunit =
                context.add(id, unitChecker, rootScopes);
        final ICompletableFuture<IUnitResult<S, L, D, Q>> result = new CompletableFuture<>();
        final IActorRef<? extends IUnit<S, L, D, Q>> subunit = result_subunit._2();
        final TypeCheckerResult<S, L, D> token = TypeCheckerResult.of(self, result);
        waitFor(token, subunit);
        result_subunit._1().whenComplete((r, ex) -> {
            logger.debug("{} subunit {} finished", this, subunit);
            resume();
            granted(token, subunit);
            result.complete(r, ex);
        });

        for(S rootScope : CapsuleUtil.toSet(rootScopes)) {
            doAddShare(subunit, rootScope);
        }

        return result;
    }

    @Override public void initScope(S root, Iterable<L> labels, boolean sharing) {
        assertInState(UnitState.ACTIVE);

        final List<EdgeOrData<L>> edges = stream(labels).map(EdgeOrData::edge).collect(Collectors.toList());

        doInitShare(self, root, edges, sharing);
    }

    @Override public S freshScope(String baseName, Iterable<L> edgeLabels, boolean data, boolean sharing) {
        assertInState(UnitState.ACTIVE);

        final String name = baseName.replace('-', '_');
        final int n = scopeNameCounters.add(name);
        final S scope = context.makeScope(name + "-" + n);

        final List<EdgeOrData<L>> labels = Lists.newArrayList();
        for(L l : edgeLabels) {
            labels.add(EdgeOrData.edge(l));
        }
        if(data) {
            labels.add(EdgeOrData.data());
        }

        scopes.__insert(scope);
        doAddLocalShare(self, scope);
        doInitShare(self, scope, labels, sharing);

        return scope;
    }

    @Override public void shareLocal(S scope) {
        assertInState(UnitState.ACTIVE);

        doAddShare(self, scope);
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

    @Override public IFuture<Set<IResolutionPath<S, L, D>>> query(S scope, LabelWf<L> labelWF, LabelOrder<L> labelOrder,
            DataWf<D> dataWF, DataLeq<D> dataEquiv, DataWfInternal<D> dataWfInternal,
            DataLeqInternal<D> dataEquivInternal) {
        assertInState(UnitState.ACTIVE);

        final IScopePath<S, L> path = Paths.empty(scope);
        final IFuture<Env<S, L, D>> result =
                doQuery(self, path, labelWF, labelOrder, dataWF, dataEquiv, dataWfInternal, dataEquivInternal);
        final Query<S, L, D> wf = Query.of(self, path, labelWF, dataWF, labelOrder, dataEquiv, result);
        waitFor(wf, self);
        stats.ownQueries += 1;
        return self.schedule(result).whenComplete((env, ex) -> {
            granted(wf, self);
            // tryFinish();
        }).thenApply(CapsuleUtil::toSet);
    }

    ///////////////////////////////////////////////////////////////////////////
    // IUnit2UnitProtocol interface, called by IUnit implementations
    ///////////////////////////////////////////////////////////////////////////

    @Override public void _initShare(S scope, Iterable<EdgeOrData<L>> edges, boolean sharing) {
        resume();
        doInitShare(self.sender(TYPE), scope, edges, sharing);
    }

    @Override public void _addShare(S scope) {
        doAddShare(self.sender(TYPE), scope);
    }

    @Override public void _doneSharing(S scope) {
        resume();
        doCloseScope(self.sender(TYPE), scope);
    }

    @Override public final void _addEdge(S source, L label, S target) {
        doAddEdge(self.sender(TYPE), source, label, target);
    }

    @Override public void _closeEdge(S scope, EdgeOrData<L> edge) {
        resume();
        doCloseLabel(self.sender(TYPE), scope, edge);
    }

    @Override public final IFuture<Env<S, L, D>> _query(IScopePath<S, L> path, LabelWf<L> labelWF, DataWf<D> dataWF,
            LabelOrder<L> labelOrder, DataLeq<D> dataEquiv) {
        // resume(); // FIXME necessary?
        stats.foreignQueries += 1;
        return doQuery(self.sender(TYPE), path, labelWF, labelOrder, dataWF, dataEquiv, null, null);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implementations -- independent of message handling context
    ///////////////////////////////////////////////////////////////////////////

    private final void doAddLocalShare(IActorRef<? extends IUnit<S, L, D, ?>> sender, S scope) {
        assertOwnOrSharedScope(scope);

        waitFor(InitScope.of(self, scope), sender);
    }

    private final void doAddShare(IActorRef<? extends IUnit<S, L, D, ?>> sender, S scope) {
        doAddLocalShare(sender, scope);

        final IActorRef<? extends IUnit<S, L, D, ?>> owner = context.owner(scope);
        if(!owner.equals(self)) {
            self.async(parent)._addShare(scope);
        }
    }

    private final void doInitShare(IActorRef<? extends IUnit<S, L, D, ?>> sender, S scope,
            Iterable<EdgeOrData<L>> edges, boolean sharing) {
        assertOwnOrSharedScope(scope);

        granted(InitScope.of(self, scope), sender);
        for(EdgeOrData<L> edge : edges) {
            waitFor(CloseLabel.of(self, scope, edge), sender);
        }
        if(sharing) {
            waitFor(CloseScope.of(self, scope), sender);
        }
        // tryFinish();

        final IActorRef<? extends IUnit<S, L, D, ?>> owner = context.owner(scope);
        if(owner.equals(self)) {
            if(isScopeInitialized(scope)) {
                releaseDelays(scope);
            }
        } else {
            self.async(parent)._initShare(scope, edges, sharing);
        }
    }

    private final void doCloseScope(IActorRef<? extends IUnit<S, L, D, ?>> sender, S scope) {
        assertOwnOrSharedScope(scope);

        granted(CloseScope.of(self, scope), sender);
        // tryFinish();

        final IActorRef<? extends IUnit<S, L, D, ?>> owner = context.owner(scope);
        if(owner.equals(self)) {
            if(isScopeInitialized(scope)) {
                releaseDelays(scope);
            }
        } else {
            self.async(parent)._doneSharing(scope);
        }
    }

    private final void doCloseLabel(IActorRef<? extends IUnit<S, L, D, ?>> sender, S scope, EdgeOrData<L> edge) {
        assertOwnOrSharedScope(scope);

        granted(CloseLabel.of(self, scope, edge), sender);
        // tryFinish();

        final IActorRef<? extends IUnit<S, L, D, ?>> owner = context.owner(scope);
        if(owner.equals(self)) {
            if(isEdgeClosed(scope, edge)) {
                releaseDelays(scope, edge);
            }
        } else {
            self.async(parent)._closeEdge(scope, edge);
        }
    }

    private final void doAddEdge(@SuppressWarnings("unused") IActorRef<? extends IUnit<S, L, D, ?>> sender, S source,
            L label, S target) {
        assertOwnOrSharedScope(source);
        assertLabelOpen(source, EdgeOrData.edge(label));

        scopeGraph.set(scopeGraph.get().addEdge(source, label, target));

        final IActorRef<? extends IUnit<S, L, D, ?>> owner = context.owner(source);
        if(!owner.equals(self)) {
            self.async(parent)._addEdge(source, label, target);
        }
    }

    private final IFuture<Env<S, L, D>> doQuery(IActorRef<? extends IUnit<S, L, D, ?>> sender, IScopePath<S, L> path,
            LabelWf<L> labelWF, LabelOrder<L> labelOrder, DataWf<D> dataWF, DataLeq<D> dataEquiv,
            DataWfInternal<D> dataWfInternal, DataLeqInternal<D> dataEquivInternal) {
        logger.debug("got _query from {}", sender);
        final boolean external = !sender.equals(self);

        final NameResolution<S, L, D> nr = new NameResolution<S, L, D>(edgeLabels, labelOrder) {

            @Override public Optional<IFuture<Env<S, L, D>>> externalEnv(IScopePath<S, L> path, LabelWf<L> re,
                    LabelOrder<L> labelOrder) {
                final S scope = path.getTarget();
                final IActorRef<? extends IUnit<S, L, D, ?>> owner = context.owner(scope);
                if(owner.equals(self)) {
                    logger.debug("local env {}", scope);
                    return Optional.empty();
                } else {
                    logger.debug("remote env {} at {}", scope, owner);
                    // this code mirrors query(...)
                    final IFuture<Env<S, L, D>> result =
                            self.async(owner)._query(path, re, dataWF, labelOrder, dataEquiv);
                    final Query<S, L, D> wf = Query.of(sender, path, re, dataWF, labelOrder, dataEquiv, result);
                    waitFor(wf, owner);
                    stats.forwardedQueries += 1;
                    return Optional.of(result.whenComplete((r, ex) -> {
                        resume();
                        logger.debug("got answer from {}", sender);
                        granted(wf, owner);
                        // tryFinish();
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
                            // tryFinish();
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
                        // tryFinish();
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
                        // tryFinish();
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

    private MultiSet.Immutable<IWaitFor<S, L, D>> waitFors = MultiSet.Immutable.of();
    private MultiSetMap.Immutable<IActorRef<? extends IUnit<S, L, D, ?>>, IWaitFor<S, L, D>> waitForsByActor =
            MultiSetMap.Immutable.of();

    private boolean isWaiting() {
        return !waitFors.isEmpty();
    }

    private boolean isWaitingFor(IWaitFor<S, L, D> token) {
        return waitFors.contains(token);
    }

    private MultiSet.Immutable<IWaitFor<S, L, D>> getTokens(IActorRef<? extends IUnit<S, L, D, ?>> unit) {
        return waitForsByActor.get(unit);
    }

    private void waitFor(IWaitFor<S, L, D> token, IActorRef<? extends IUnit<S, L, D, ?>> actor) {
        logger.debug("{} wait for {}/{}", self, actor, token);
        waitFors = waitFors.add(token);
        waitForsByActor = waitForsByActor.put(actor, token);
    }

    private void granted(IWaitFor<S, L, D> token, IActorRef<? extends IUnit<S, L, D, ?>> actor) {
        if(!waitForsByActor.contains(actor, token)) {
            logger.error("{} not waiting for granted {}/{}", self, actor, token);
            throw new IllegalStateException(self + " not waiting for granted " + actor + "/" + token);
        }
        waitFors = waitFors.remove(token);
        waitForsByActor = waitForsByActor.remove(actor, token);
    }

    /**
     * Checks if the unit is finished, or still waiting on something. Must be called after all grants. Note that if a
     * wait-for is granted, and others are introduced, this method must be called after all have been processed, or it
     * may conclude prematurely that the unit is done.
     */
    private void tryFinish() {
        logger.debug("{} tryFinish", this);
        if(state.equals(UnitState.ACTIVE) && !isWaiting()) {
            logger.debug("{} finish", this);
            state = UnitState.DONE;
            unitResult.complete(UnitResult.of(id(), scopeGraph.get(), analysis.get(), failures, stats));
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Resolution & delays
    ///////////////////////////////////////////////////////////////////////////

    private void releaseDelays(S scope) {
        for(Entry<EdgeOrData<L>, Delay> entry : delays.get(scope)) {
            final EdgeOrData<L> edge = entry.getKey();
            if(!isWaitingFor(CloseLabel.of(self, scope, edge))) {
                final Delay delay = entry.getValue();
                logger.debug("released {} on {}(/{})", delay, scope, edge);
                delays.remove(scope, edge, delay);
                self.complete(delay.future, org.metaborg.util.unit.Unit.unit, null);
            }
        }
    }

    private void releaseDelays(S scope, EdgeOrData<L> edge) {
        for(Delay delay : delays.get(scope, edge)) {
            logger.debug("released {} on {}/{}", delay, scope, edge);
            delays.remove(scope, edge, delay);
            self.complete(delay.future, org.metaborg.util.unit.Unit.unit, null);
        }
    }

    private boolean isScopeInitialized(S scope) {
        return !isWaitingFor(InitScope.of(self, scope)) && !isWaitingFor(CloseScope.of(self, scope));
    }

    private boolean isEdgeClosed(S scope, EdgeOrData<L> edge) {
        return isScopeInitialized(scope) && !isWaitingFor(CloseLabel.of(self, scope, edge));
    }

    private IFuture<org.metaborg.util.unit.Unit> isComplete(S scope, EdgeOrData<L> edge,
            IActorRef<? extends IUnit<S, L, D, ?>> sender) {
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
        public final IActorRef<? extends IUnit<S, L, D, ?>> sender;

        Delay(ICompletableFuture<org.metaborg.util.unit.Unit> future, IActorRef<? extends IUnit<S, L, D, ?>> sender) {
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

    @Override public void _deadlockQuery(IActorRef<? extends IUnit<S, L, D, ?>> i, int m) {
        final IActorRef<? extends IUnit<S, L, D, ?>> j = self.sender(TYPE);
        cmh.query(i, m, j);
    }

    @Override public void _deadlockReply(IActorRef<? extends IUnit<S, L, D, ?>> i, int m,
            java.util.Set<IActorRef<? extends IUnit<S, L, D, ?>>> R) {
        cmh.reply(i, m, R);
    }

    private void handleDeadlock(java.util.Set<IActorRef<? extends IUnit<S, L, D, ?>>> nodes) {
        logger.debug("{} deadlocked with {}", this, nodes);
        if(!nodes.contains(self)) {
            throw new IllegalStateException("Deadlock unrelated to this unit.");
        }
        if(nodes.size() == 1) {
            logger.debug("{} self-deadlocked with {}", this, getTokens(self));
            if(!failDelays(nodes)) {
                failAll(nodes);
            }
        } else {
            for(IActorRef<? extends IUnit<S, L, D, ?>> node : nodes) {
                self.async(node)._deadlocked(nodes);
            }
        }
    }

    @Override public void _deadlocked(java.util.Set<IActorRef<? extends IUnit<S, L, D, ?>>> nodes) {
        // resume(); // FIXME not necessary?
        failDelays(nodes);
    }

    /**
     * Fail delays that are part of the deadlock. If any delays can be failed, computations should be able to continue
     * (or fail following the exception).
     * 
     * The set of open scopes and labels is unchanged, and it is safe for the type checker to continue.
     */
    private boolean failDelays(java.util.Set<IActorRef<? extends IUnit<S, L, D, ?>>> nodes) {
        final Set.Transient<ICompletable<?>> deadlocked = CapsuleUtil.transientSet();
        for(Delay delay : delays.inverse().keySet()) {
            if(nodes.contains(delay.sender)) {
                logger.debug("{} fail {}", self, delay);
                delays.inverse().remove(delay);
                deadlocked.__insert(delay.future);
            }
        }
        for(IActorRef<? extends IUnit<S, L, D, ?>> node : nodes) {
            for(IWaitFor<S, L, D> wf : getTokens(node)) {
                // @formatter:off
                wf.visit(IWaitFor.cases(
                    initScope -> {},
                    closeScope -> {},
                    closeLabel -> {},
                    query -> {},
                    result  -> {},
                    typeCheckerState -> {
                        if(nodes.contains(typeCheckerState.origin())) {
                            logger.debug("{} fail {}", self, typeCheckerState);
                            deadlocked.__insert(typeCheckerState.future());
                        }
                    }
                ));
                // @formatter:on
            }
        }
        for(ICompletable<?> future : deadlocked) {
            self.complete(future, null, new DeadlockException("Type checker deadlocked."));
        }
        return !deadlocked.isEmpty();
    }

    /**
     * If there are no delays to fail, the type checker has no way to make progress. In this case, everything is failed,
     * and the state cleaned such that all scopes and labels are closed and these closures properly reported to the
     * parent.
     * 
     * After this, it is not safe if the type checker is ever called again, as it may then try to close scopes and
     * labels that were cleaned up. Therefore, local delays are not failed, as this would trigger their completion and
     * trigger the type checker. Delays resulting from remote queries must still be cancelled, or the remote unit waits
     * indefinitely for the result from this now defunct unit.
     */
    private void failAll(java.util.Set<IActorRef<? extends IUnit<S, L, D, ?>>> nodes) {
        // Grants are processed immediately, while the result failure is scheduled.
        // This ensures that all labels are closed by the time the result failure is processed.
        for(IWaitFor<S, L, D> wf : waitFors) {
            // @formatter:off
            wf.visit(IWaitFor.cases(
                initScope -> {
                    failures.add(new DeadlockException(initScope.toString()));
                    granted(initScope, self);
                    if(!context.owner(initScope.scope()).equals(self)) {
                        self.async(parent)._initShare(initScope.scope(), CapsuleUtil.immutableSet(), false);
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
                    if(typeCheckerState.origin().equals(self)) {
                        logger.error("Unexpected remaining internal state: " + typeCheckerState);
                        throw new IllegalStateException("Unexpected remaining internal state: " + typeCheckerState);
                    }
                    self.complete(typeCheckerState.future(), null, new DeadlockException("Type checker deadlocked."));
                }
            ));
            // @formatter:on
        }
        // tryFinish();
    }

    ///////////////////////////////////////////////////////////////////////////
    // ChandryMisraHaas.Host
    ///////////////////////////////////////////////////////////////////////////

    @Override public IActorRef<? extends IUnit<S, L, D, ?>> process() {
        return self;
    }

    @Override public java.util.Set<IActorRef<? extends IUnit<S, L, D, ?>>> dependentSet() {
        return waitForsByActor.keySet();
    }

    @Override public void query(IActorRef<? extends IUnit<S, L, D, ?>> k, IActorRef<? extends IUnit<S, L, D, ?>> i,
            int m) {
        self.async(k)._deadlockQuery(i, m);
    }

    @Override public void reply(IActorRef<? extends IUnit<S, L, D, ?>> k, IActorRef<? extends IUnit<S, L, D, ?>> i,
            int m, java.util.Set<IActorRef<? extends IUnit<S, L, D, ?>>> R) {
        self.async(k)._deadlockReply(i, m, R);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Stats
    ///////////////////////////////////////////////////////////////////////////

    private static class Stats implements IUnitStats {

        private int ownQueries;
        private int foreignQueries;
        private int forwardedQueries;

        private IActorStats actorStats;

        private Stats(IActorStats actorStats) {
            this.actorStats = actorStats;
        }

        @Override public Iterable<String> csvHeaders() {
            return Iterables.concat(ImmutableList.of("ownQueries", "incomingQueries", "outgoingQueries"),
                    actorStats.csvHeaders());
        }

        @Override public Iterable<String> csvRow() {
            return Iterables.concat(ImmutableList.of(Integer.toString(ownQueries), Integer.toString(foreignQueries),
                    Integer.toString(forwardedQueries)), actorStats.csvRow());
        }

        @Override public String toString() {
            return "UnitStats{ownQueries=" + ownQueries + ",foreignQueries=" + foreignQueries + ",forwardedQueries="
                    + forwardedQueries + "," + actorStats + "}";
        }

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
            logger.error("Scope {} is not owned or shared by {}", scope, this);
            throw new IllegalArgumentException("Scope " + scope + " is not owned or shared by " + this);
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
    // toString
    ///////////////////////////////////////////////////////////////////////////

    @Override public String toString() {
        return "Unit{" + self.id() + "}";
    }

}