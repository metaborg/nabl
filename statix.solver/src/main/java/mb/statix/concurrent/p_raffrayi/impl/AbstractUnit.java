package mb.statix.concurrent.p_raffrayi.impl;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.metaborg.util.Ref;
import org.metaborg.util.functions.Function2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.unit.Unit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

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
import mb.statix.concurrent.p_raffrayi.ITypeCheckerContext;
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
import mb.statix.concurrent.p_raffrayi.nameresolution.DataWf;
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelOrder;
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelWf;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.diff.BiMap;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.reference.Env;
import mb.statix.scopegraph.reference.ScopeGraph;
import mb.statix.scopegraph.terms.newPath.ScopePath;

public abstract class AbstractUnit<S, L, D, R>
        implements IUnit<S, L, D, R>, IActorMonitor, Host<IActorRef<? extends IUnit<S, L, D, ?>>> {

    private static final ILogger logger = LoggerUtils.logger(AbstractUnit.class);

    protected final TypeTag<IUnit<S, L, D, ?>> TYPE = TypeTag.of(IUnit.class);

    protected final IActor<? extends IUnit<S, L, D, R>> self;
    @Nullable protected final IActorRef<? extends IUnit<S, L, D, ?>> parent;
    protected final IUnitContext<S, L, D> context;

    private final ChandyMisraHaas<IActorRef<? extends IUnit<S, L, D, ?>>> cmh;

    private volatile boolean innerResult;
    private final Ref<R> analysis;
    private final List<Throwable> failures;
    private final Map<String, IUnitResult<S, L, D, ?>> subUnitResults;
    private final ICompletableFuture<IUnitResult<S, L, D, R>> unitResult;

    protected final Ref<IScopeGraph.Immutable<S, EdgeOrEps<L>, D>> scopeGraph;
    protected final Set.Immutable<L> edgeLabels;
    protected final Set.Transient<S> scopes;
    private final IRelation3.Transient<S, EdgeKind<L>, Delay> delays;

    private final BiMap.Transient<S> reps = BiMap.Transient.of();

    private final MultiSet.Transient<String> scopeNameCounters;

    protected final Stats stats;

    public AbstractUnit(IActor<? extends IUnit<S, L, D, R>> self,
            @Nullable IActorRef<? extends IUnit<S, L, D, ?>> parent, IUnitContext<S, L, D> context,
            Iterable<L> edgeLabels) {
        this.self = self;
        this.parent = parent;
        this.context = context;

        this.cmh = new ChandyMisraHaas<>(this, this::handleDeadlock);

        this.innerResult = false;
        this.analysis = new Ref<>();
        this.failures = new ArrayList<>();
        this.subUnitResults = new HashMap<>();
        this.unitResult = new CompletableFuture<>();

        this.scopeGraph = new Ref<>(ScopeGraph.Immutable.of());
        this.edgeLabels = CapsuleUtil.toSet(edgeLabels);
        this.scopes = CapsuleUtil.transientSet();
        this.delays = HashTrieRelation3.Transient.of();

        this.scopeNameCounters = MultiSet.Transient.of();

        this.stats = new Stats(self.stats());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Interface
    ///////////////////////////////////////////////////////////////////////////

    protected abstract IFuture<D> getExternalDatum(D datum);

    ///////////////////////////////////////////////////////////////////////////
    // IUnit2UnitProtocol interface, called by IUnit implementations
    ///////////////////////////////////////////////////////////////////////////

    @Override public void _initShare(S scope, S childRep) {
        doChildInit(self.sender(TYPE), scope, childRep);
    }

    @Override public IFuture<Env<S, L, D>> _query(S scope, ScopePath<S, L> path, LabelWf<L> labelWF,
            DataWf<S, L, D> dataWF, LabelOrder<L> labelOrder, DataLeq<S, L, D> dataEquiv) {
        // resume(); // FIXME necessary?
        stats.incomingQueries += 1;
        return doQuery(self.sender(TYPE), scope, path, labelWF, labelOrder, dataWF, dataEquiv, null, null);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implementations -- independent of message handling context
    ///////////////////////////////////////////////////////////////////////////

    protected final S makeScope(String baseName) {
        final String name = baseName.replace('-', '_');
        final int n = scopeNameCounters.add(name);
        final S scope = context.makeScope(name + "-" + n);
        return scope;
    }

    protected final void doStart(List<S> rootScopes) {
        for(S rootScope : CapsuleUtil.toSet(rootScopes)) {
            scopes.__insert(rootScope);
            doAddLocalShare(rootScope);
        }
    }

    protected final IFuture<IUnitResult<S, L, D, R>> doFinish(IFuture<R> result) {
        final ICompletableFuture<R> internalResult = new CompletableFuture<>();
        final TypeCheckerResult<S, L, D> token = TypeCheckerResult.of(self, internalResult);
        waitFor(token, self);
        result.whenComplete(internalResult::complete); // FIXME self.schedule(result)?
        internalResult.whenComplete((r, ex) -> {
            logger.debug("{} type checker finished", this);
            resume(); // FIXME necessary?
            if(ex != null) {
                failures.add(ex);
            } else {
                analysis.set(r);
            }
            granted(token, self);
            final MultiSet.Immutable<IWaitFor<S, L, D>> selfTokens = getTokens(self);
            if(!selfTokens.isEmpty()) {
                logger.debug("{} returned while waiting on {}", self, selfTokens);
            }
            innerResult = true;
        });
        return unitResult;
    }

    protected <Q> Tuple2<IActorRef<? extends IUnit<S, L, D, Q>>, IFuture<IUnitResult<S, L, D, Q>>> doAddSubUnit(
            String id, Function2<IActor<IUnit<S, L, D, Q>>, IUnitContext<S, L, D>, IUnit<S, L, D, Q>> unitProvider,
            List<S> rootScopes) {
        for(S rootScope : rootScopes) {
            assertOwnOrSharedScope(rootScope);
            assertScopeOpen(findRep(rootScope));
        }

        final Tuple2<IFuture<IUnitResult<S, L, D, Q>>, IActorRef<? extends IUnit<S, L, D, Q>>> result_subunit =
                context.add(id, unitProvider, rootScopes);
        final IActorRef<? extends IUnit<S, L, D, Q>> subunit = result_subunit._2();

        final ICompletableFuture<IUnitResult<S, L, D, Q>> internalResult = new CompletableFuture<>();
        final TypeCheckerResult<S, L, D> token = TypeCheckerResult.of(self, internalResult);
        waitFor(token, subunit);
        result_subunit._1().whenComplete(internalResult::complete); // must come after waitFor

        for(S rootScope : CapsuleUtil.toSet(rootScopes)) {
            waitFor(CloseLabel.of(self, findRep(rootScope), EdgeKind.eps()), subunit);
        }

        final IFuture<IUnitResult<S, L, D, Q>> ret = internalResult.whenComplete((r, ex) -> {
            logger.debug("{} subunit {} finished", this, subunit);
            resume();
            granted(token, subunit);
            if(ex != null) {
                failures.add(new Exception("No result for sub unit " + id));
            } else {
                subUnitResults.put(id, r);
            }
        });

        return Tuple2.of(subunit, ret);
    }

    protected final S doFreshScope(String baseName, Collection<L> edgeLabels, boolean data, boolean sharing) {
        final S scope = makeScope(baseName);

        scopes.__insert(scope);
        doAddLocalShare(scope);
        doInitShare(self, scope, edgeLabels, data, sharing);

        return scope;
    }

    protected final void doAddLocalShare(S scope) {
        assertOwnOrSharedScope(scope);
        waitFor(InitScope.of(self, scope), self);
    }

    protected final void doChildInit(IActorRef<? extends IUnit<S, L, D, ?>> sender, S root, S childRep) {
        final S localRep = findRep(root);
        if(!context.owner(localRep).equals(self)) {
            logger.error("Cannot set child representative for non-owned local rep {} (orig: {}).", localRep, root);
            throw new IllegalStateException("Cannot set child representative for " + localRep);
        }

        scopeGraph.set(scopeGraph.get().addEdge(localRep, EdgeOrEps.eps(), childRep));
        doCloseLabel(sender, localRep, EdgeKind.eps());
    }

    protected final void doInitShare(IActorRef<? extends IUnit<S, L, D, ?>> sender, S scope, Collection<L> labels,
            boolean data, boolean sharing) {
        assertOwnOrSharedScope(scope);

        S localRep;
        if(isOwner(scope)) {
            localRep = scope;
        } else {
            if(data) {
                throw new IllegalStateException("Cannot set data for shared scope " + scope);
            }
            localRep = makeScope(context.name(scope) + "-rep");
            scopes.__insert(localRep);

            // Set representative as datum.
            waitFor(CloseLabel.of(self, localRep, EdgeKind.data()), sender);
            doSetDatum(localRep, context.embed(scope));

            reps.put(scope, localRep);
            self.async(parent)._initShare(scope, localRep);
        }

        granted(InitScope.of(self, scope), sender);
        for(L label : labels) {
            waitFor(CloseLabel.of(self, localRep, EdgeKind.edge(label)), sender);
        }
        if(data) {
            waitFor(CloseLabel.of(self, localRep, EdgeKind.data()), sender);
        }
        if(sharing) {
            waitFor(CloseScope.of(self, localRep), sender);
        }
        if(isScopeInitialized(localRep)) {
            releaseDelays(localRep);
        }
    }

    protected final void doSetDatum(S scope, D datum) {
        final EdgeKind<L> edge = EdgeKind.data();
        assertLabelOpen(scope, edge);

        scopeGraph.set(scopeGraph.get().setDatum(scope, datum));
        doCloseLabel(self, scope, edge);
    }

    protected final void doCloseScope(IActorRef<? extends IUnit<S, L, D, ?>> sender, S scope) {
        assertOwnScope(scope);
        granted(CloseScope.of(self, scope), sender);

        if(isScopeInitialized(scope)) {
            releaseDelays(scope);
        }
    }

    protected final void doCloseLabel(IActorRef<? extends IUnit<S, L, D, ?>> sender, S scope, EdgeKind<L> edge) {
        granted(CloseLabel.of(self, scope, edge), sender);

        if(isEdgeClosed(scope, edge)) {
            releaseDelays(scope, edge);
        }
    }

    protected final void doAddEdge(@SuppressWarnings("unused") IActorRef<? extends IUnit<S, L, D, ?>> sender, S source,
            L label, S target) {
        assertLabelOpen(source, EdgeKind.edge(label));
        scopeGraph.set(scopeGraph.get().addEdge(source, EdgeOrEps.edge(label), target));
    }

    protected final IFuture<Env<S, L, D>> doQuery(IActorRef<? extends IUnit<S, L, D, ?>> sender, S scope,
            ScopePath<S, L> path, LabelWf<L> labelWF, LabelOrder<L> labelOrder, DataWf<S, L, D> dataWF,
            DataLeq<S, L, D> dataEquiv, DataWf<S, L, D> dataWfInternal, DataLeq<S, L, D> dataEquivInternal) {
        logger.debug("got _query from {}", sender);
        final boolean external = !sender.equals(self);

        final NameResolution<S, L, D> nr = new NameResolution<S, L, D>(edgeLabels, labelOrder) {

            @Override public Optional<IFuture<Env<S, L, D>>> externalEnv(S scope, ScopePath<S, L> path, LabelWf<L> re,
                    LabelOrder<L> labelOrder) {
                if(canAnswer(scope)) {
                    logger.debug("local env {}", scope);
                    return Optional.empty();
                } else {
                    final IActorRef<? extends IUnit<S, L, D, ?>> owner = context.owner(scope);
                    logger.debug("remote env {} at {}", scope, owner);
                    // this code mirrors query(...)
                    final IFuture<Env<S, L, D>> result =
                            self.async(owner)._query(scope, path, re, dataWF, labelOrder, dataEquiv);
                    final Query<S, L, D> wf = Query.of(sender, path, re, dataWF, labelOrder, dataEquiv, result);
                    waitFor(wf, owner);
                    if(external) {
                        stats.forwardedQueries += 1;
                    } else {
                        stats.outgoingQueries += 1;
                    }
                    return Optional.of(result.whenComplete((r, ex) -> {
                        logger.debug("got answer from {}", sender);
                        resume();
                        granted(wf, owner);
                    }));
                }
            }

            @Override protected IFuture<Optional<D>> getDatum(S scope) {
                return isComplete(scope, EdgeKind.data(), sender).thenCompose(__ -> {
                    final Optional<D> datum;
                    if(!(datum = scopeGraph.get().getData(scope)).isPresent()) {
                        return CompletableFuture.completedFuture(Optional.empty());
                    }
                    if(external) {
                        logger.debug("require external rep for {}", datum.get());
                        final IFuture<D> result = getExternalDatum(datum.get());
                        final IFuture<D> ret;
                        if(result.isDone()) {
                            ret = result;
                        } else {
                            final ICompletableFuture<D> internalResult = new CompletableFuture<>();
                            final IWaitFor<S, L, D> token =
                                    TypeCheckerState.of(sender, ImmutableList.of(datum.get()), internalResult);
                            waitFor(token, self);
                            result.whenComplete(internalResult::complete); // must come after waitFor
                            ret = internalResult.whenComplete((rep, ex) -> {
                                self.assertOnActorThread();
                                granted(token, self);
                            });
                        }
                        return ret.thenApply(rep -> {
                            logger.debug("got external rep {} for {}", rep, datum.get());
                            return Optional.of(rep);
                        });
                    } else {
                        return CompletableFuture.completedFuture(datum);
                    }
                });
            }

            @Override protected IFuture<Iterable<S>> getEdges(S scope, EdgeOrEps<L> label) {
                return isComplete(scope, EdgeKind.from(label), sender).thenApply(__ -> {
                    return scopeGraph.get().getEdges(scope, label);
                });
            }

            @Override protected IFuture<Boolean> dataWf(D d, ICancel cancel) throws InterruptedException {
                stats.dataWfChecks += 1;
                final IFuture<Boolean> result;
                if(external || dataWfInternal == null) {
                    result = dataWF.wf(d, queryContext, cancel);
                } else {
                    result = dataWfInternal.wf(d, queryContext, cancel);
                }
                if(result.isDone()) {
                    return result;
                } else {
                    final ICompletableFuture<Boolean> internalResult = new CompletableFuture<>();
                    final TypeCheckerState<S, L, D> token =
                            TypeCheckerState.of(sender, ImmutableList.of(d), internalResult);
                    waitFor(token, self);
                    result.whenComplete(internalResult::complete); // must come after waitFor
                    return internalResult.whenComplete((r, ex) -> {
                        self.assertOnActorThread();
                        granted(token, self);
                    });
                }
            }

            @Override protected IFuture<Boolean> dataLeq(D d1, D d2, ICancel cancel) throws InterruptedException {
                stats.dataLeqChecks += 1;
                final IFuture<Boolean> result;
                if(external || dataEquivInternal == null) {
                    result = dataEquiv.leq(d1, d2, queryContext, cancel);
                } else {
                    result = dataEquivInternal.leq(d1, d2, queryContext, cancel);
                }
                if(result.isDone()) {
                    return result;
                } else {
                    final ICompletableFuture<Boolean> internalResult = new CompletableFuture<>();
                    final TypeCheckerState<S, L, D> token =
                            TypeCheckerState.of(sender, ImmutableList.of(d1, d2), internalResult);
                    waitFor(token, self);
                    result.whenComplete(internalResult::complete); // must come after waitFor
                    return internalResult.whenComplete((r, ex) -> {
                        self.assertOnActorThread();
                        granted(token, self);
                    });
                }
            }

            public IFuture<Boolean> dataLeqAlwaysTrue(ICancel cancel) {
                return dataEquiv.alwaysTrue(queryContext, cancel);
            }

        };

        // Note that scope == path.getTarget() does not necessarily hold
        // when epsilon edges are traversed, because the epsilon edges are
        // not included in the path.
        // In general, local representatives of a shared scope should never be
        // exposed to a type checker, and hence never be included in a path.
        final IFuture<Env<S, L, D>> result = nr.env(scope, path, labelWF, context.cancel());
        result.whenComplete((env, ex) -> {
            logger.debug("have answer for {}", sender);
        });
        return result;
    }

    private final ITypeCheckerContext<S, L, D> queryContext = new ITypeCheckerContext<S, L, D>() {

        @Override public String id() {
            return self.id() + "#query";
        }

        @SuppressWarnings("unused") @Override public <Q> IFuture<IUnitResult<S, L, D, Q>> add(String id,
                ITypeChecker<S, L, D, Q> unitChecker, List<S> rootScopes) {
            throw new UnsupportedOperationException("Unsupported in query context.");
        }


        @SuppressWarnings("unused") @Override public IFuture<IUnitResult<S, L, D, Unit>> add(String id,
                List<S> givenRootScopes, java.util.Set<S> givenOwnScopes,
                IScopeGraph.Immutable<S, L, D> givenScopeGraph, List<S> rootScopes) {
            throw new UnsupportedOperationException("Unsupported in query context.");
        }

        @SuppressWarnings("unused") @Override public void initScope(S root, Collection<L> labels, boolean sharing) {
            throw new UnsupportedOperationException("Unsupported in query context.");
        }

        @SuppressWarnings("unused") @Override public S freshScope(String baseName, Collection<L> edgeLabels,
                boolean data, boolean sharing) {
            throw new UnsupportedOperationException("Unsupported in query context.");
        }

        @SuppressWarnings("unused") @Override public void shareLocal(S scope) {
            throw new UnsupportedOperationException("Unsupported in query context.");
        }

        @SuppressWarnings("unused") @Override public void setDatum(S scope, D datum) {
            throw new UnsupportedOperationException("Unsupported in query context.");
        }

        @SuppressWarnings("unused") @Override public void addEdge(S source, L label, S target) {
            throw new UnsupportedOperationException("Unsupported in query context.");
        }

        @SuppressWarnings("unused") @Override public void closeEdge(S source, L label) {
            throw new UnsupportedOperationException("Unsupported in query context.");
        }

        @SuppressWarnings("unused") @Override public void closeScope(S scope) {
            throw new UnsupportedOperationException("Unsupported in query context.");
        }

        @Override public IFuture<Set<IResolutionPath<S, L, D>>> query(S scope, LabelWf<L> labelWF,
                LabelOrder<L> labelOrder, DataWf<S, L, D> dataWF, DataLeq<S, L, D> dataEquiv,
                @Nullable DataWf<S, L, D> dataWfInternal, @Nullable DataLeq<S, L, D> dataEquivInternal) {
            // does not require the Unit to be ACTIVE

            final ScopePath<S, L> path = new ScopePath<>(scope);
            final IFuture<Env<S, L, D>> result = doQuery(self, scope, path, labelWF, labelOrder, dataWF, dataEquiv,
                    dataWfInternal, dataEquivInternal);
            final Query<S, L, D> wf = Query.of(self, path, labelWF, dataWF, labelOrder, dataEquiv, result);
            waitFor(wf, self);
            stats.localQueries += 1;
            return self.schedule(result).whenComplete((env, ex) -> {
                granted(wf, self);
            }).thenApply(CapsuleUtil::toSet);
        }

    };

    protected final boolean isOwner(S scope) {
        final IActorRef<? extends IUnit<S, L, D, ?>> owner = context.owner(scope);
        return owner.equals(self);
    }

    protected boolean canAnswer(S scope) {
        return isOwner(scope);
    }

    protected S findRep(S scope) {
        assertOwnOrSharedScope(scope);
        final S rep = reps.getValueOrDefault(scope, scope);
        assertOwnScope(rep);
        return rep;
    }

    protected S findCanon(S scope) {
        assertOwnScope(scope);
        final S rep = reps.getKeyOrDefault(scope, scope);
        assertOwnOrSharedScope(rep);
        return rep;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Wait fors & finalization
    ///////////////////////////////////////////////////////////////////////////

    private MultiSet.Immutable<IWaitFor<S, L, D>> waitFors = MultiSet.Immutable.of();
    private MultiSetMap.Immutable<IActorRef<? extends IUnit<S, L, D, ?>>, IWaitFor<S, L, D>> waitForsByActor =
            MultiSetMap.Immutable.of();

    protected boolean isWaiting() {
        return !waitFors.isEmpty();
    }

    protected boolean isWaitingFor(IWaitFor<S, L, D> token) {
        return waitFors.contains(token);
    }

    private MultiSet.Immutable<IWaitFor<S, L, D>> getTokens(IActorRef<? extends IUnit<S, L, D, ?>> unit) {
        return waitForsByActor.get(unit);
    }

    protected void waitFor(IWaitFor<S, L, D> token, IActorRef<? extends IUnit<S, L, D, ?>> actor) {
        logger.debug("{} wait for {}/{}", self, actor, token);
        waitFors = waitFors.add(token);
        waitForsByActor = waitForsByActor.put(actor, token);
    }

    protected void granted(IWaitFor<S, L, D> token, IActorRef<? extends IUnit<S, L, D, ?>> actor) {
        if(!waitForsByActor.contains(actor, token)) {
            logger.error("{} not waiting for granted {}/{}", self, actor, token);
            throw new IllegalStateException(self + " not waiting for granted " + actor + "/" + token);
        }
        waitFors = waitFors.remove(token);
        waitForsByActor = waitForsByActor.remove(actor, token);
        logger.debug("{} granted {}/{}", self, actor, token);
    }

    /**
     * Checks if the unit is finished, or still waiting on something. Must be called after all grants. Note that if a
     * wait-for is granted, and others are introduced, this method must be called after all have been processed, or it
     * may conclude prematurely that the unit is done.
     */
    protected void tryFinish() {
        logger.debug("{} tryFinish", this);
        if(innerResult && !unitResult.isDone() && !isWaiting()) {
            logger.debug("{} finish", this);

            // Flatten scope graph by discarding epsilon edges and setting source
            // scope to the canonical scope, instead of local representative.
            final IScopeGraph.Transient<S, L, D> _sg = ScopeGraph.Transient.of();
            scopeGraph.get().getData().forEach(_sg::setDatum);
            scopeGraph.get().getEdges().forEach((src_lbl, tgts) -> {
                final S src = findCanon(src_lbl.getKey());
                src_lbl.getValue().accept(() -> {}, l -> {
                    tgts.forEach(tgt -> _sg.addEdge(src, l, tgt));
                });
            });

            unitResult.complete(
                    UnitResult.of(self.id(), _sg.freeze(), analysis.get(), failures, subUnitResults, stats));
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Resolution & delays
    ///////////////////////////////////////////////////////////////////////////

    private void releaseDelays(S scope) {
        for(Map.Entry<EdgeKind<L>, Delay> entry : delays.get(scope)) {
            final EdgeKind<L> edge = entry.getKey();
            if(!isWaitingFor(CloseLabel.of(self, scope, edge))) {
                final Delay delay = entry.getValue();
                logger.debug("released {} on {}(/{})", delay, scope, edge);
                delays.remove(scope, edge, delay);
                self.complete(delay.future, org.metaborg.util.unit.Unit.unit, null);
            }
        }
    }

    private void releaseDelays(S scope, EdgeKind<L> edge) {
        for(Delay delay : delays.get(scope, edge)) {
            logger.debug("released {} on {}/{}", delay, scope, edge);
            delays.remove(scope, edge, delay);
            self.complete(delay.future, org.metaborg.util.unit.Unit.unit, null);
        }
    }

    private boolean isScopeInitialized(S scope) {
        return !isWaitingFor(InitScope.of(self, scope)) && !isWaitingFor(CloseScope.of(self, scope));
    }

    private boolean isEdgeClosed(S scope, EdgeKind<L> edge) {
        return isScopeInitialized(scope) && !isWaitingFor(CloseLabel.of(self, scope, edge));
    }

    private IFuture<org.metaborg.util.unit.Unit> isComplete(S scope, EdgeKind<L> edge,
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

    protected void suspend() {
        if(cmh.idle()) {
        }
    }

    protected void resume() {
        if(cmh.exec()) {
        }
    }

    @Override public void _deadlockQuery(IActorRef<? extends IUnit<S, L, D, ?>> i, int m) {
        final IActorRef<? extends IUnit<S, L, D, ?>> j = self.sender(TYPE);
        cmh.query(i, m, j);
    }

    @Override public void _deadlockReply(IActorRef<? extends IUnit<S, L, D, ?>> i, int m,
            java.util.Set<IActorRef<? extends IUnit<S, L, D, ?>>> R) {
        cmh.reply(i, m, R);
    }

    @Override public void _deadlocked(java.util.Set<IActorRef<? extends IUnit<S, L, D, ?>>> nodes) {
        if(failDelays(nodes)) {
            resume(); // resume to ensure further deadlock detection after these are handled
        }
    }

    private void handleDeadlock(java.util.Set<IActorRef<? extends IUnit<S, L, D, ?>>> nodes) {
        logger.debug("{} deadlocked with {}", this, nodes);
        if(!nodes.contains(self)) {
            throw new IllegalStateException("Deadlock unrelated to this unit.");
        }
        if(nodes.size() == 1) {
            logger.debug("{} self-deadlocked with {}", this, getTokens(self));
            if(failDelays(nodes)) {
                resume(); // resume to ensure further deadlock detection after these are handled
            } else {
                failAll();
            }
        } else {
            // nodes will include self
            for(IActorRef<? extends IUnit<S, L, D, ?>> node : nodes) {
                self.async(node)._deadlocked(nodes);
            }
        }
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
    private void failAll() {
        // Grants are processed immediately, while the result failure is scheduled.
        // This ensures that all labels are closed by the time the result failure is processed.
        for(IWaitFor<S, L, D> wf : waitFors) {
            // @formatter:off
            wf.visit(IWaitFor.cases(
                initScope -> {
                    failures.add(new DeadlockException(initScope.toString()));
                    granted(initScope, self);
                    if(!isOwner(initScope.scope())) {
                        S rep = doFreshScope(context.name(initScope.scope()), Arrays.asList(), false, false);
                        self.async(parent)._initShare(initScope.scope(), rep);
                    }
                    releaseDelays(initScope.scope());
                },
                closeScope -> {
                    failures.add(new DeadlockException(closeScope.toString()));
                    granted(closeScope, self);
                    releaseDelays(closeScope.scope());
                },
                closeLabel -> {
                    failures.add(new DeadlockException(closeLabel.toString()));
                    granted(closeLabel, self);
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
    // IActorMonitor
    ///////////////////////////////////////////////////////////////////////////

    private long wakeTimeNanos;

    @Override public void started() {
        logger.debug("{} started", this);
        wakeTimeNanos = System.nanoTime();
    }

    @Override public void resumed() {
        logger.debug("{} resumed", this);
        wakeTimeNanos = System.nanoTime();
    }

    @Override public void suspended() {
        logger.debug("{} suspended", this);
        final long suspendTimeNanos = System.nanoTime();
        final long activeTimeNanos = suspendTimeNanos - wakeTimeNanos;
        stats.runtimeNanos += activeTimeNanos;
        suspend();
        tryFinish();
    }

    @Override public void stopped(Throwable ex) {
        if(!unitResult.isDone()) {
            if(ex != null && ex instanceof InterruptedException) {
                unitResult.completeExceptionally(ex);
            } else {
                unitResult.completeExceptionally(new TypeCheckingFailedException(this + " stopped.", ex));
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Stats
    ///////////////////////////////////////////////////////////////////////////

    protected static class Stats implements IUnitStats {

        protected int localQueries;
        protected int incomingQueries;
        protected int outgoingQueries;
        protected int forwardedQueries;
        protected long runtimeNanos;
        protected int dataWfChecks;
        protected int dataLeqChecks;

        private IActorStats actorStats;

        private Stats(IActorStats actorStats) {
            this.actorStats = actorStats;
        }

        @Override public Iterable<String> csvHeaders() {
            // @formatter:off
            return Iterables.concat(ImmutableList.of(
                "runtimeMillis",
                "localQueries",
                "incomingQueries",
                "outgoingQueries",
                "forwardedQueries",
                "dataWfChecks",
                "dataLeqChecks"
            ), actorStats.csvHeaders());
            // @formatter:on
        }

        @Override public Iterable<String> csvRow() {
            // @formatter:off
            return Iterables.concat(ImmutableList.of(
                Long.toString(TimeUnit.MILLISECONDS.convert(runtimeNanos, TimeUnit.NANOSECONDS)),
                Integer.toString(localQueries),
                Integer.toString(incomingQueries),
                Integer.toString(outgoingQueries),
                Integer.toString(forwardedQueries),
                Integer.toString(dataWfChecks),
                Integer.toString(dataLeqChecks)
            ), actorStats.csvRow());
            // @formatter:on
        }

        @Override public String toString() {
            return "UnitStats{ownQueries=" + localQueries + ",foreignQueries=" + incomingQueries + ",forwardedQueries="
                    + outgoingQueries + "," + actorStats + "}";
        }

    }

    ///////////////////////////////////////////////////////////////////////////
    // Assertions
    ///////////////////////////////////////////////////////////////////////////

    protected void assertOwnOrSharedScope(S scope) {
        if(!scopes.contains(scope)) {
            logger.error("Scope {} is not owned or shared by {}", scope, this);
            throw new IllegalArgumentException("Scope " + scope + " is not owned or shared by " + this);
        }
    }

    protected void assertOwnScope(S scope) {
        if(!context.owner(scope).equals(self)) {
            logger.error("Scope {} is not owned by {}", scope, this);
            throw new IllegalArgumentException("Scope " + scope + " is not owned by " + this);
        }
    }

    protected void assertScopeOpen(S scope) {
        assertOwnScope(scope);
        if(isScopeInitialized(scope)) {
            logger.error("Scope {} is not open on {}.", scope, self);
            throw new IllegalArgumentException("Scope " + scope + " is not open on " + self + ".");
        }
    }

    protected void assertLabelOpen(S scope, EdgeKind<L> edge) {
        assertOwnScope(scope);
        if(isEdgeClosed(scope, edge)) {
            logger.error("Label {}/{} is not open on {}.", scope, edge, self);
            throw new IllegalArgumentException("Label " + scope + "/" + edge + " is not open on " + self + ".");
        }
    }

}