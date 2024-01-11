package mb.p_raffrayi.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;

import org.metaborg.util.Ref;
import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.HashTrieRelation3;
import org.metaborg.util.collection.IRelation3;
import org.metaborg.util.collection.ImList;
import org.metaborg.util.collection.MultiSet;
import org.metaborg.util.collection.MultiSetMap;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.Function2;
import org.metaborg.util.future.AggregateFuture;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.ICompletable;
import org.metaborg.util.future.ICompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.tuple.Tuple2;
import org.metaborg.util.unit.Unit;

import io.usethesource.capsule.Set;
import mb.p_raffrayi.DeadlockException;
import mb.p_raffrayi.IRecordedQuery;
import mb.p_raffrayi.IUnitResult;
import mb.p_raffrayi.IUnitResult.TransitionTrace;
import mb.p_raffrayi.IUnitStats;
import mb.p_raffrayi.TypeCheckingFailedException;
import mb.p_raffrayi.actors.IActor;
import mb.p_raffrayi.actors.IActorMonitor;
import mb.p_raffrayi.actors.IActorRef;
import mb.p_raffrayi.actors.IActorStats;
import mb.p_raffrayi.actors.TypeTag;
import mb.p_raffrayi.actors.deadlock.ChandyMisraHaas;
import mb.p_raffrayi.actors.deadlock.ChandyMisraHaas.Host;
import mb.p_raffrayi.impl.DeadlockUtils.GraphBuilder;
import mb.p_raffrayi.impl.DeadlockUtils.IGraph;
import mb.p_raffrayi.impl.diff.IDifferContext;
import mb.p_raffrayi.impl.diff.IDifferOps;
import mb.p_raffrayi.impl.diff.IScopeGraphDiffer;
import mb.p_raffrayi.impl.tokens.CloseLabel;
import mb.p_raffrayi.impl.tokens.CloseScope;
import mb.p_raffrayi.impl.tokens.DifferResult;
import mb.p_raffrayi.impl.tokens.DifferState;
import mb.p_raffrayi.impl.tokens.IWaitFor;
import mb.p_raffrayi.impl.tokens.InitScope;
import mb.p_raffrayi.impl.tokens.Match;
import mb.p_raffrayi.impl.tokens.PQuery;
import mb.p_raffrayi.impl.tokens.Query;
import mb.p_raffrayi.impl.tokens.TypeCheckerResult;
import mb.p_raffrayi.impl.tokens.TypeCheckerState;
import mb.p_raffrayi.impl.tokens.UnitAdd;
import mb.p_raffrayi.nameresolution.DataLeq;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.p_raffrayi.nameresolution.IResolutionContext;
import mb.p_raffrayi.nameresolution.IQuery;
import mb.p_raffrayi.nameresolution.NameResolutionQuery;
import mb.p_raffrayi.nameresolution.StateMachineQuery;
import mb.p_raffrayi.nameresolution.tracing.AExtQuerySet;
import mb.p_raffrayi.nameresolution.tracing.ExtQuerySet;
import mb.p_raffrayi.nameresolution.tracing.ExtQuerySets;
import mb.scopegraph.ecoop21.LabelOrder;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.ScopeGraphUtil;
import org.metaborg.util.collection.BiMap;
import mb.scopegraph.oopsla20.diff.ScopeGraphDiff;
import mb.scopegraph.oopsla20.path.IResolutionPath;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.scopegraph.oopsla20.reference.Env;
import mb.scopegraph.oopsla20.reference.ScopeGraph;
import mb.scopegraph.oopsla20.terms.newPath.ResolutionPath;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;
import mb.scopegraph.patching.IPatchCollection;
import mb.scopegraph.resolution.StateMachine;

public abstract class AbstractUnit<S, L, D, R> implements IUnit<S, L, D, R>, IActorMonitor, Host<IProcess<S, L, D>> {

    private static final ILogger logger = LoggerUtils.logger(IUnit.class);

    protected final TypeTag<IUnit<S, L, D, ?>> TYPE = TypeTag.of(IUnit.class);

    protected final IActor<? extends IUnit<S, L, D, R>> self;
    protected final @Nullable IActorRef<? extends IUnit<S, L, D, ?>> parent;
    protected final IUnitContext<S, L, D> context;

    protected final ChandyMisraHaas<IProcess<S, L, D>> cmh;
    protected final UnitProcess<S, L, D> process;
    private final BrokerProcess<S, L, D> broker = BrokerProcess.of();

    private volatile boolean innerResult;
    private final Ref<R> analysis;
    protected final List<Throwable> failures;
    private final Map<String, IUnitResult<S, L, D, ?>> subUnitResults;
    private final ICompletableFuture<IUnitResult<S, L, D, R>> unitResult;

    protected final Ref<IScopeGraph.Immutable<S, L, D>> scopeGraph;
    protected final Set.Immutable<L> edgeLabels;
    protected final Set.Transient<S> scopes;
    protected final Set.Transient<S> sharedScopes;
    protected final List<S> rootScopes = new ArrayList<>();

    private final IRelation3.Transient<S, EdgeOrData<L>, Delay> delays;

    protected @Nullable IScopeGraphDiffer<S, L, D> differ;
    private final Ref<ScopeGraphDiff<S, L, D>> diffResult = new Ref<>();
    protected final ICompletableFuture<Unit> whenDifferActivated = new CompletableFuture<>();

    protected MultiSet.Transient<String> scopeNameCounters;
    protected Set.Transient<String> usedStableScopes;

    protected final java.util.Set<IRecordedQuery<S, L, D>> recordedQueries = new HashSet<>();

    protected TransitionTrace stateTransitionTrace = TransitionTrace.OTHER;
    protected final Stats stats;

    public AbstractUnit(IActor<? extends IUnit<S, L, D, R>> self,
            @Nullable IActorRef<? extends IUnit<S, L, D, ?>> parent, IUnitContext<S, L, D> context,
            Iterable<L> edgeLabels) {
        this.self = self;
        this.parent = parent;
        this.context = context;

        this.cmh = new ChandyMisraHaas<>(this, this::handleDeadlock);
        this.process = new UnitProcess<>(self);

        this.innerResult = false;
        this.analysis = new Ref<>();
        this.failures = new ArrayList<>();
        this.subUnitResults = new HashMap<>();
        this.unitResult = new CompletableFuture<>();

        this.scopeGraph = new Ref<>(ScopeGraph.Immutable.of());
        this.edgeLabels = CapsuleUtil.toSet(edgeLabels);
        this.scopes = CapsuleUtil.transientSet();
        this.sharedScopes = CapsuleUtil.transientSet();
        this.delays = HashTrieRelation3.Transient.of();

        this.scopeNameCounters = MultiSet.Transient.of();
        this.usedStableScopes = CapsuleUtil.transientSet();

        this.stats = new Stats(self.stats());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Interface
    ///////////////////////////////////////////////////////////////////////////

    protected abstract IFuture<D> getExternalDatum(D datum);

    protected abstract D getPreviousDatum(D datum);

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

    @Override public void _addEdge(S source, L label, S target) {
        doAddEdge(self.sender(TYPE), source, label, target);
    }

    @Override public void _closeEdge(S scope, EdgeOrData<L> edge) {
        resume();
        doCloseLabel(self.sender(TYPE), scope, edge);
    }

    @Override public IFuture<IQueryAnswer<S, L, D>> _query(IActorRef<? extends IUnit<S, L, D, ?>> origin,
            ScopePath<S, L> path, IQuery<S, L, D> query, DataWf<S, L, D> dataWF, DataLeq<S, L, D> dataEquiv) {
        // resume(); // FIXME necessary?
        stats.incomingQueries += 1;
        return doQuery(self.sender(TYPE), origin, false, path, query, dataWF, dataEquiv, null, null);
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

    protected final S makeStableScope(String name) {
        return context.makeScope(name + "!");
    }

    protected final void doStart(List<S> currentRootScopes) {
        this.rootScopes.addAll(currentRootScopes);
        for(S rootScope : CapsuleUtil.toSet(currentRootScopes)) {
            scopes.__insert(rootScope);
            doAddLocalShare(self, rootScope);
        }
    }

    protected final IFuture<IUnitResult<S, L, D, R>> doFinish(IFuture<R> result) {
        final ICompletableFuture<R> internalResult = new CompletableFuture<>();
        final TypeCheckerResult<S, L, D> token = TypeCheckerResult.of(self, internalResult);
        waitFor(token, self);
        result.whenComplete(internalResult::complete); // FIXME self.schedule(result)?
        internalResult.whenComplete((r, ex) -> {
            final String id = self.id();
            logger.debug("{} type checker finished", id);
            resume(); // FIXME necessary?
            if(isDifferEnabled()) {
                self.assertOnActorThread();
                whenDifferActivated.thenAccept(__ -> differ.typeCheckerFinished());
            }
            if(ex != null) {
                logger.error("type checker errored: {}", ex);
                failures.add(ex);
            } else {
                analysis.set(r);
            }
            granted(token, self);
            final MultiSet<IWaitFor<S, L, D>> selfTokens = getTokens(process);
            if(!selfTokens.isEmpty()) {
                logger.debug("{} returned while waiting on {}", self, selfTokens);
            }
            innerResult = true;
        });
        return unitResult;
    }

    protected boolean isDifferEnabled() {
        return context.settings().scopeGraphDiff();
    }

    protected void initDiffer(IScopeGraphDiffer<S, L, D> differ, List<S> currentRootScopes,
            List<S> previousRootScopes) {
        assertDifferEnabled();
        logger.debug("Initializing differ: {} with scopes: {} ~ {}.", differ, currentRootScopes, previousRootScopes);
        this.differ = differ;
        doFinishDiffer(differ.diff(currentRootScopes, previousRootScopes));
        whenDifferActivated.complete(Unit.unit);
    }

    protected void initDiffer(IScopeGraphDiffer<S, L, D> differ, IScopeGraph.Immutable<S, L, D> scopeGraph,
            Collection<S> scopes, Collection<S> sharedScopes, IPatchCollection.Immutable<S> patches, Collection<S> openScopes,
            MultiSetMap.Immutable<S, EdgeOrData<L>> openEdges) {
        assertDifferEnabled();
        logger.debug("Initializing differ: {} with initial scope graph: {}.", differ, scopeGraph);
        this.differ = differ;
        doFinishDiffer(differ.diff(scopeGraph, scopes, sharedScopes, patches, openScopes, openEdges));
        whenDifferActivated.complete(Unit.unit);
    }

    private void doFinishDiffer(IFuture<ScopeGraphDiff<S, L, D>> future) {
        final ICompletableFuture<ScopeGraphDiff<S, L, D>> differResult = new CompletableFuture<>();

        // Handle diff output
        final DifferResult<S, L, D> result = DifferResult.of(self, differResult);
        waitFor(result, self);
        self.schedule(future).whenComplete(differResult::complete);
        differResult.whenComplete((r, ex) -> {
            logger.debug("{} scope graph differ finished", this);
            if(ex != null) {
                logger.error("scope graph differ errored.", ex);
                failures.add(ex);
            } else {
                diffResult.set(r);
            }
            granted(result, self);
            resume();
            tryFinish();
        });
    }

    ///////////////////////////////////////////////////////////////////////////
    // ITypeCheckerContext interface, called by ITypeChecker implementations
    ///////////////////////////////////////////////////////////////////////////

    // NB. Invoke methods via `local` so that we have the same scheduling & ordering
    // guarantees as for remote calls.

    protected <U> Tuple2<IActorRef<? extends IUnit<S, L, D, U>>, IFuture<IUnitResult<S, L, D, U>>> doAddSubUnit(
            String id, Function2<IActor<IUnit<S, L, D, U>>, IUnitContext<S, L, D>, IUnit<S, L, D, U>> unitProvider,
            List<S> rootScopes, boolean ignoreResult) {
        for(S rootScope : rootScopes) {
            assertOwnOrSharedScope(rootScope);
        }

        final Tuple2<IFuture<IUnitResult<S, L, D, U>>, IActorRef<? extends IUnit<S, L, D, U>>> result_subunit =
                context.add(id, unitProvider, rootScopes);
        final IActorRef<? extends IUnit<S, L, D, U>> subunit = result_subunit._2();

        final ICompletableFuture<IUnitResult<S, L, D, U>> internalResult = new CompletableFuture<>();
        final TypeCheckerResult<S, L, D> token = TypeCheckerResult.of(self, internalResult);
        waitFor(token, subunit);
        result_subunit._1().whenComplete(internalResult::complete); // must come after waitFor

        for(S rootScope : CapsuleUtil.toSet(rootScopes)) {
            doAddShare(subunit, rootScope);
        }

        final IFuture<IUnitResult<S, L, D, U>> ret = internalResult.whenComplete((r, ex) -> {
            logger.debug("{} subunit {} finished", this, subunit);
            granted(token, subunit);
            // resume(); // FIXME needed?
            if(ex != null) {
                failures.add(new Exception("No result for sub unit " + id));
            } else if(!ignoreResult) {
                subUnitResults.put(id, r);
            }
        });

        return Tuple2.of(subunit, ret);
    }

    protected final S doFreshScope(String baseName, Iterable<L> edgeLabels, boolean data, boolean sharing) {
        final S scope = makeScope(baseName);

        return doPrepareScope(scope, edgeLabels, data, sharing);
    }

    protected final S doStableFreshScope(String name, Iterable<L> edgeLabels, boolean data) {
        if(!usedStableScopes.__insert(name)) {
            throw new IllegalStateException("Stable scope identity " + name + " already used.");
        }
        final S scope = makeStableScope(name);

        return doPrepareScope(scope, edgeLabels, data, true);
    }

    private S doPrepareScope(final S scope, Iterable<L> edgeLabels, boolean data, boolean sharing) {
        final List<EdgeOrData<L>> labels = new ArrayList<>();
        for(L l : edgeLabels) {
            labels.add(EdgeOrData.edge(l));
            if(!this.edgeLabels.contains(l)) {
                logger.error("Initializing scope {} with unregistered edge label {}.", scope, l);
                throw new IllegalStateException(
                        "Initializing scope " + scope + " with unregistered edge label " + l + ".");
            }
        }
        if(data) {
            labels.add(EdgeOrData.data());
        }

        scopes.__insert(scope);
        if(sharing) {
            sharedScopes.__insert(scope);
        }

        doAddLocalShare(self, scope);
        doInitShare(self, scope, labels, sharing);

        return scope;
    }

    @Override public IFuture<Optional<S>> _match(S previousScope) {
        assertOwnScope(previousScope);
        final IActorRef<? extends IUnit<S, L, D, ?>> sender = self.sender(TYPE);
        if(isDifferEnabled()) {
            return whenDifferActivated.thenCompose(__ -> {
                final ICompletableFuture<Optional<S>> future = new CompletableFuture<>();
                final DifferState<S, L, D> state = DifferState.ofMatch(sender, previousScope, future);
                waitFor(state, self);
                differ.match(previousScope).whenComplete(future::complete);
                future.whenComplete((r, ex) -> {
                    granted(state, self);
                });
                return future;
            });
        } else {
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implementations -- independent of message handling context
    ///////////////////////////////////////////////////////////////////////////

    protected final void doAddLocalShare(IActorRef<? extends IUnit<S, L, D, ?>> sender, S scope) {
        assertOwnOrSharedScope(scope);

        waitFor(InitScope.of(self, scope), sender);
    }

    protected final void doAddShare(IActorRef<? extends IUnit<S, L, D, ?>> sender, S scope) {
        doAddLocalShare(sender, scope);

        if(!isOwner(scope)) {
            self.async(parent)._addShare(scope);
        }
    }

    protected final void doInitShare(IActorRef<? extends IUnit<S, L, D, ?>> sender, S scope,
            Iterable<EdgeOrData<L>> edges, boolean sharing) {
        assertOwnOrSharedScope(scope);

        granted(InitScope.of(self, scope), sender);
        // resume(); // seems unnecessary (tested with unit tests + CSV-IO)

        for(EdgeOrData<L> edge : edges) {
            waitFor(CloseLabel.of(self, scope, edge), sender);
        }
        if(sharing) {
            waitFor(CloseScope.of(self, scope), sender);
        }

        if(isScopeInitialized(scope)) {
            releaseDelays(scope);
        }

        if(!isOwner(scope)) {
            self.async(parent)._initShare(scope, edges, sharing);
        }
    }

    protected final void doSetDatum(S scope, D datum) {
        final EdgeOrData<L> edge = EdgeOrData.data();
        assertLabelOpen(scope, edge);

        scopeGraph.set(scopeGraph.get().setDatum(scope, datum));
        doCloseLabel(self, scope, edge);
    }

    protected final void doCloseScope(IActorRef<? extends IUnit<S, L, D, ?>> sender, S scope) {
        assertOwnOrSharedScope(scope);

        granted(CloseScope.of(self, scope), sender);
        // resume(); // seems unnecessary (tested with unit tests + CSV-IO)

        if(isScopeInitialized(scope)) {
            releaseDelays(scope);
        }

        if(!isOwner(scope)) {
            self.async(parent)._doneSharing(scope);
        }
    }

    protected final void doCloseLabel(IActorRef<? extends IUnit<S, L, D, ?>> sender, S scope, EdgeOrData<L> edge) {
        assertOwnOrSharedScope(scope);

        granted(CloseLabel.of(self, scope, edge), sender);
        // resume(); // seems unnecessary (tested with unit tests + CSV-IO)

        if(isEdgeClosed(scope, edge)) {
            releaseDelays(scope, edge);
        }

        if(!isOwner(scope)) {
            self.async(parent)._closeEdge(scope, edge);
        }
    }

    protected final void doAddEdge(@SuppressWarnings("unused") IActorRef<? extends IUnit<S, L, D, ?>> sender, S source,
            L label, S target) {
        assertOwnOrSharedScope(source);
        assertLabelOpen(source, EdgeOrData.edge(label));

        scopeGraph.set(scopeGraph.get().addEdge(source, label, target));

        if(!isOwner(source)) {
            self.async(parent)._addEdge(source, label, target);
        }
    }

    protected final IFuture<IQueryAnswer<S, L, D>> doQuery(IActorRef<? extends IUnit<S, L, D, ?>> sender,
            IActorRef<? extends IUnit<S, L, D, ?>> origin, boolean record, ScopePath<S, L> path, IQuery<S, L, D> query,
            DataWf<S, L, D> dataWF, DataLeq<S, L, D> dataEquiv, DataWf<S, L, D> dataWfInternal,
            DataLeq<S, L, D> dataEquivInternal) {
        final ILogger logger = LoggerUtils.logger(IResolutionContext.class);
        logger.debug("got _query from {}", sender);

        final boolean external = !sender.equals(self);

        final Ref<IResolutionContext<S, L, D, ExtQuerySet<S, L, D>>> context = new Ref<>();
        context.set(new IResolutionContext<S, L, D, ExtQuerySet<S, L, D>>() {

            @Override public IFuture<Tuple2<Env<S, L, D>, ExtQuerySet<S, L, D>>> externalEnv(ScopePath<S, L> path,
                    IQuery<S, L, D> query, ICancel cancel) {
                final S scope = path.getTarget();
                if(canAnswer(scope)) {
                    logger.debug("local env {}", scope);
                    return query.resolve(context.get(), path, cancel).thenApply(ans -> {
                        if(isQueryRecordingEnabled() && record && sharedScopes.contains(scope)) {
                            recordedQueries.add(
                                    RecordedQuery.of(path, datumScopes(ans._1()), query.labelWf(), dataWF, ans._1()));
                        }

                        return ans;
                    });
                } else {
                    return getOwner(scope).thenCompose(owner -> {
                        logger.debug("remote env {} at {}", scope, owner);
                        // this code mirrors query(...)
                        final IFuture<IQueryAnswer<S, L, D>> result =
                                self.async(owner)._query(origin, path, query, dataWF, dataEquiv);
                        final Query<S, L, D> wf = Query.of(sender, path, query, dataWF, dataEquiv, result);
                        waitFor(wf, owner);
                        if(external) {
                            stats.forwardedQueries += 1;
                        } else {
                            stats.outgoingQueries += 1;
                        }
                        return result.thenApply(ans -> {
                            AExtQuerySet.Builder<S, L, D> extQueriesBuilder =
                                    AExtQuerySet.<S, L, D>builder().from(ExtQuerySets.empty());
                            if(isQueryRecordingEnabled()) {
                                extQueriesBuilder.addAllTransitiveQueries(ans.transitiveQueries());
                                extQueriesBuilder.addAllPredicateQueries(ans.predicateQueries());
                                extQueriesBuilder.addTransitiveQueries(RecordedQuery.of(path, datumScopes(ans.env()),
                                        query.labelWf(), dataWF, ans.env()));
                            }
                            return Tuple2.of(ans.env(), extQueriesBuilder.build());
                        }).whenComplete((env, ex) -> {
                            logger.debug("got answer from {}", sender);
                            granted(wf, owner);
                            resume();
                        });
                    });
                }
            }

            @Override public IFuture<Optional<D>> getDatum(S scope) {
                return isComplete(scope, EdgeOrData.data(), sender).thenCompose(__ -> {
                    final Optional<D> datum;
                    if(!(datum = scopeGraph.get().getData(scope)).isPresent()) {
                        return CompletableFuture.completedFuture(Optional.empty());
                    }
                    if(external || dataWfInternal == null) {
                        logger.debug("require external rep for {}", datum.get());
                        final IFuture<D> result = getExternalDatum(datum.get());
                        final IFuture<D> ret;
                        if(result.isDone()) {
                            ret = result;
                        } else {
                            final ICompletableFuture<D> internalResult = new CompletableFuture<>();
                            final IWaitFor<S, L, D> token =
                                    TypeCheckerState.of(sender, ImList.Immutable.of(datum.get()), internalResult);
                            waitFor(token, self);
                            result.whenComplete(internalResult::complete); // must come after waitFor
                            ret = internalResult.whenComplete((rep, ex) -> {
                                self.assertOnActorThread();
                                granted(token, self);
                                // resume();
                            });
                            // resume();
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

            @Override public IFuture<Collection<S>> getEdges(S scope, L label) {
                return isComplete(scope, EdgeOrData.edge(label), sender).thenApply(__ -> {
                    return scopeGraph.get().getEdges(scope, label);
                });
            }

            @Override public IFuture<Tuple2<Boolean, ExtQuerySet<S, L, D>>> dataWf(D d, ICancel cancel)
                    throws InterruptedException {
                stats.dataWfChecks += 1;
                final IFuture<Boolean> result;
                final ICompletableFuture<Boolean> future = new CompletableFuture<>();
                final RecordingTypeCheckerContext queryContext = new RecordingTypeCheckerContext(origin);

                if(external || dataWfInternal == null) {
                    result = dataWF.wf(d, queryContext, cancel);
                } else {
                    result = dataWfInternal.wf(d, queryContext, cancel);
                }
                if(result.isDone()) {
                    result.whenComplete(future::complete);
                } else {
                    final ICompletableFuture<Boolean> internalResult = new CompletableFuture<>();
                    final TypeCheckerState<S, L, D> token =
                            TypeCheckerState.of(sender, ImList.Immutable.of(d), internalResult);
                    waitFor(token, self);
                    result.whenComplete(internalResult::complete); // must come after waitFor
                    internalResult.whenComplete((r, ex) -> {
                        self.assertOnActorThread();
                        granted(token, self);
                        future.complete(r, ex);
                    });
                }
                return future.thenApply(wf -> {
                    return Tuple2.of(wf,
                            AExtQuerySet.<S, L, D>builder().addAllPredicateQueries(queryContext.dispose()).build());
                });
            }

            @Override public IFuture<Tuple2<Boolean, ExtQuerySet<S, L, D>>> dataEquiv(D d1, D d2, ICancel cancel)
                    throws InterruptedException {
                stats.dataLeqChecks += 1;
                final IFuture<Boolean> result;
                final ICompletableFuture<Boolean> future = new CompletableFuture<>();
                final RecordingTypeCheckerContext queryContext = new RecordingTypeCheckerContext(origin);

                if(external || dataEquivInternal == null) {
                    result = dataEquiv.leq(d1, d2, queryContext, cancel);
                } else {
                    result = dataEquivInternal.leq(d1, d2, queryContext, cancel);
                }
                if(result.isDone()) {
                    result.whenComplete(future::complete);
                } else {
                    final ICompletableFuture<Boolean> internalResult = new CompletableFuture<>();
                    final TypeCheckerState<S, L, D> token =
                            TypeCheckerState.of(sender, ImList.Immutable.of(d1, d2), internalResult);
                    waitFor(token, self);
                    result.whenComplete(internalResult::complete); // must come after waitFor
                    internalResult.whenComplete((r, ex) -> {
                        self.assertOnActorThread();
                        granted(token, self);
                        future.complete(r, ex);
                    });
                }

                return future.thenApply(wf -> {
                    return Tuple2.of(wf,
                            AExtQuerySet.<S, L, D>builder().addAllPredicateQueries(queryContext.dispose()).build());
                });
            }

            @Override public IFuture<Boolean> dataEquivAlwaysTrue(ICancel cancel) {
                final RecordingTypeCheckerContext queryContext = new RecordingTypeCheckerContext(origin);
                return dataEquiv.alwaysTrue(queryContext, cancel).thenApply(at -> {
                    if(!queryContext.dispose().isEmpty()) {
                        throw new IllegalStateException("alwaysTrue() check should not perform queries.");
                    }
                    return at;
                });
            }

            @Override public ExtQuerySet<S, L, D> unitMetadata() {
                return ExtQuerySets.empty();
            }

            @Override public ExtQuerySet<S, L, D> compose(ExtQuerySet<S, L, D> queries1, ExtQuerySet<S, L, D> queries2) {
                return queries1.addQueries(queries2);
            }

        });

        final IFuture<Tuple2<Env<S, L, D>, ExtQuerySet<S, L, D>>> result =
                context.get().externalEnv(path, query, this.context.cancel());

        result.whenComplete((env, ex) -> {
            logger.debug("have answer for {}", sender);
            if(record && isQueryRecordingEnabled()) {
                recordedQueries.addAll(env._2().transitiveQueries());
                recordedQueries.addAll(env._2().predicateQueries());
            }
        });
        return result
                .thenApply(env -> QueryAnswer.of(env._1(), env._2().transitiveQueries(), env._2().predicateQueries()));
    }

    private class RecordingTypeCheckerContext extends AbstractQueryTypeCheckerContext<S, L, D, R> {

        private final Set.Transient<IRecordedQuery<S, L, D>> queries = CapsuleUtil.transientSet();
        private boolean disposed = false;

        private final IActorRef<? extends IUnit<S, L, D, ?>> origin;

        public RecordingTypeCheckerContext(IActorRef<? extends IUnit<S, L, D, ?>> origin) {
            this.origin = origin;
        }

        @Override public String id() {
            return self.id() + "#query";
        }

        @Override public IFuture<? extends java.util.Set<IResolutionPath<S, L, D>>> query(S scope,
                IQuery<S, L, D> query, DataWf<S, L, D> dataWF, DataLeq<S, L, D> dataEquiv,
                DataWf<S, L, D> dataWfInternal, DataLeq<S, L, D> dataEquivInternal) {
            // does not require the Unit to be ACTIVE

            final ScopePath<S, L> path = new ScopePath<>(scope);
            // If record is true, a potentially hidden scope from the predicate may leak to the recordedQueries of the receiver.
            final IFuture<IQueryAnswer<S, L, D>> result =
                    doQuery(self, origin, false, path, query, dataWF, dataEquiv, dataWfInternal, dataEquivInternal);
            final Query<S, L, D> wf = Query.of(self, path, query, dataWF, dataEquiv, result);
            waitFor(wf, self);
            stats.localQueries += 1;
            return self.schedule(result).thenApply(ans -> {
                if(isQueryRecordingEnabled()) {
                    // Type-checkers can embed scopes in their predicates that are not accessible from the outside.
                    // If such a query is confirmed, the scope graph differ will never produce a scope diff for it,
                    // leading to exceptions. However, since the query is local, it is not required to verify it anyway.
                    // Hence, we just ignore it.
                    if(!context.scopeId(path.getTarget()).equals(origin.id())) {
                        recordQuery(ARecordedQuery.of(path, datumScopes(ans.env()), query.labelWf(), dataWF, ans.env(),
                                false));
                    }

                    // Patches for transitive queries in nested(/predicate) query do not have to
                    // be applied to type-checker result
                    ans.transitiveQueries().forEach(q -> recordQuery(q.withIncludePatches(false)));
                    recordQueries(ans.predicateQueries());
                }
                return CapsuleUtil.<IResolutionPath<S, L, D>>toSet(ans.env());
            }).whenComplete((ans, ex) -> {
                granted(wf, self);
                // resume(); // seems unnecessary (unit tests + CSV IO)
            });
        }

        @Override public IFuture<? extends java.util.Set<IResolutionPath<S, L, D>>> query(S scope, LabelWf<L> labelWF,
                LabelOrder<L> labelOrder, DataWf<S, L, D> dataWF, DataLeq<S, L, D> dataEquiv,
                @Nullable DataWf<S, L, D> dataWfInternal, @Nullable DataLeq<S, L, D> dataEquivInternal) {
            return this.query(scope, new NameResolutionQuery<>(labelWF, labelOrder, edgeLabels), dataWF, dataEquiv,
                    dataWfInternal, dataEquivInternal);
        }

        @Override public IFuture<? extends java.util.Set<IResolutionPath<S, L, D>>> query(S scope,
                StateMachine<L> stateMachine, DataWf<S, L, D> dataWF, DataLeq<S, L, D> dataEquiv,
                DataWf<S, L, D> dataWfInternal, DataLeq<S, L, D> dataEquivInternal) {
            return this.query(scope, new StateMachineQuery<>(stateMachine), dataWF, dataEquiv, dataWfInternal,
                    dataEquivInternal);
        }

        private void assertUndisposed() {
            if(disposed) {
                throw new IllegalStateException("Recording query after context disposed.");
            }
        }

        private void recordQuery(IRecordedQuery<S, L, D> query) {
            assertUndisposed();
            this.queries.__insert(query);
        }

        private void recordQueries(java.util.Set<IRecordedQuery<S, L, D>> queries) {
            assertUndisposed();
            this.queries.__insertAll(queries);
        }

        public java.util.Set<IRecordedQuery<S, L, D>> dispose() {
            assertUndisposed();
            disposed = true;
            return queries.freeze();
        }

    }

    protected final IFuture<IQueryAnswer<S, L, D>> doQueryPrevious(IActorRef<? extends IUnit<S, L, D, ?>> sender,
            IScopeGraph.Immutable<S, L, D> scopeGraph, ScopePath<S, L> path, IQuery<S, L, D> query,
            DataWf<S, L, D> dataWF, DataLeq<S, L, D> dataEquiv) {
        logger.debug("rec pquery from {}", sender);
        final IResolutionContext<S, L, D, ExtQuerySet<S, L, D>> context =
                new StaticNameResolutionContext(sender, scopeGraph, dataWF, dataEquiv);

        return query.resolve(context, path, this.context.cancel()).thenApply(env -> {
            return QueryAnswer.of(env._1(), env._2().transitiveQueries(), env._2().predicateQueries());
        });
    }

    protected final boolean isOwner(S scope) {
        return context.scopeId(scope).equals(self.id());
    }

    protected final IFuture<IActorRef<? extends IUnit<S, L, D, ?>>> getOwner(S scope) {
        final IFuture<IActorRef<? extends IUnit<S, L, D, ?>>> future = context.owner(scope);
        if(future.isDone()) {
            return future;
        }
        final ICompletableFuture<IActorRef<? extends IUnit<S, L, D, ?>>> result = new CompletableFuture<>();
        final UnitAdd<S, L, D> unitAdd = UnitAdd.of(self, context.scopeId(scope), result);
        waitFor(unitAdd, broker);
        // Due to the synchronous nature of the Broker, this future may be completed by
        // another unit's thread. Hence we wrap it in self.schedule, so we do not break
        // the actor abstraction.
        self.schedule(future).whenComplete((r, ex) -> {
            self.assertOnActorThread();
            granted(unitAdd, broker);
            result.complete(r, ex);
            resume(); // FIXME needed?
        });
        return result;
    }

    protected boolean canAnswer(S scope) {
        return isOwner(scope);
    }

    private Set<S> datumScopes(Env<S, L, D> env) {
        return Iterables2.stream(env).map(ResolutionPath::getDatum).map(context::getScopes)
                .reduce(CapsuleUtil.immutableSet(), Set.Immutable::__insertAll);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Wait fors & finalization
    ///////////////////////////////////////////////////////////////////////////

    private WaitForGraph<IProcess<S, L, D>, IWaitFor<S, L, D>> wfg = new WaitForGraph<>();

    protected boolean isWaiting() {
        return wfg.isWaiting();
    }

    protected boolean isWaitingFor(IWaitFor<S, L, D> token) {
        return wfg.isWaitingFor(token);
    }

    protected boolean isWaitingFor(IWaitFor<S, L, D> token, IActor<? extends IUnit<S, L, D, ?>> from) {
        return wfg.isWaitingFor(new UnitProcess<>(from), token);
    }

    protected int countWaitingFor(IWaitFor<S, L, D> token, IActorRef<? extends IUnit<S, L, D, ?>> from) {
        return wfg.countWaitingFor(new UnitProcess<>(from), token);
    }

    private MultiSet<IWaitFor<S, L, D>> getTokens(IProcess<S, L, D> unit) {
        return wfg.getTokens(unit);
    }

    protected MultiSet<IWaitFor<S, L, D>> ownTokens() {
        return getTokens(process);
    }

    protected void waitFor(IWaitFor<S, L, D> token, IActorRef<? extends IUnit<S, L, D, ?>> actor) {
        waitFor(token, process(actor));
    }

    protected void waitFor(IWaitFor<S, L, D> token, IProcess<S, L, D> process) {
        if(wfg.waitFor(process, token)) {
            resume();
        }
    }

    protected void waitFor(IActorRef<? extends IUnit<S, L, D, ?>> actor) {
        waitFor(process(actor));
    }

    protected void waitFor(IProcess<S, L, D> process) {
        if(wfg.waitFor(process)) {
            resume();
        }
    }

    protected void granted(IWaitFor<S, L, D> token, IActorRef<? extends IUnit<S, L, D, ?>> actor) {
        granted(token, process(actor));
    }

    protected void granted(IWaitFor<S, L, D> token, IProcess<S, L, D> process) {
        if(wfg.granted(process, token)) {
            resume();
        }
    }

    protected void granted(IActorRef<? extends IUnit<S, L, D, ?>> actor) {
        granted(process(actor));
    }

    protected void granted(IProcess<S, L, D> process) {
        if(wfg.granted(process)) {
            resume();
        }
    }

    private IProcess<S, L, D> process(IActorRef<? extends IUnit<S, L, D, ?>> actor) {
        return actor == self ? process : new UnitProcess<>(actor);
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
            // @formatter:off
            final UnitResult<S, L, D, R> result = UnitResult.<S, L, D, R>builder()
                .id(self.id())
                .scopeGraph(scopeGraph.get())
                .queries(recordedQueries)
                .rootScopes(rootScopes)
                .scopes(scopes.freeze())
                .result(analysis.get())
                .failures(failures)
                .subUnitResults(subUnitResults)
                .stats(stats)
                .stateTransitionTrace(stateTransitionTrace)
                .diff(diffResult.get())
                .build();
            // @formatter:on
            self.complete(unitResult, result, null);
        } else {
            logger.trace("Still waiting for {}{}", !innerResult ? "inner result and " : "", wfg);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Resolution & delays
    ///////////////////////////////////////////////////////////////////////////

    private void releaseDelays(S scope) {
        for(Map.Entry<EdgeOrData<L>, Delay> entry : delays.get(scope)) {
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
        assertOwnOrSharedScope(scope);
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

    protected final class StaticNameResolutionContext implements IResolutionContext<S, L, D, ExtQuerySet<S, L, D>> {

        private final IActorRef<? extends IUnit<S, L, D, ?>> sender;
        private final DataLeq<S, L, D> dataLeq;
        private final IScopeGraph.Immutable<S, L, D> scopeGraph;
        private final DataWf<S, L, D> dataWf;

        protected StaticNameResolutionContext(IActorRef<? extends IUnit<S, L, D, ?>> sender,
                IScopeGraph.Immutable<S, L, D> scopeGraph, DataWf<S, L, D> dataWf, DataLeq<S, L, D> dataLeq) {
            this.sender = sender;
            this.scopeGraph = scopeGraph;
            this.dataWf = dataWf;
            this.dataLeq = dataLeq;
        }

        @Override public IFuture<Tuple2<Env<S, L, D>, ExtQuerySet<S, L, D>>> externalEnv(ScopePath<S, L> path,
                IQuery<S, L, D> query, ICancel cancel) {
            final S scope = path.getTarget();
            if(canAnswer(scope)) {
                logger.debug("local p_env {}", scope);
                return query.resolve(this, path, cancel);
            }

            return getOwner(scope).thenCompose(owner -> {
                logger.debug("remote p_env from {}", owner);
                final ICompletableFuture<Tuple2<Env<S, L, D>, ExtQuerySet<S, L, D>>> future = new CompletableFuture<>();
                final PQuery<S, L, D> pQuery = PQuery.of(sender, path, dataWf, future);
                waitFor(pQuery, owner);
                self.async(owner)._queryPrevious(path, query, dataWf, dataLeq).whenComplete((env, ex) -> {
                    logger.debug("got p_env from {}", sender);
                    granted(pQuery, owner);
                    future.complete(
                            Tuple2.of(env.env(), ExtQuerySet.of(env.transitiveQueries(), env.predicateQueries())), ex);
                    // resume(); // FIXME needed?
                });

                return future;
            });
        }

        @Override public IFuture<Optional<D>> getDatum(S scope) {
            return CompletableFuture
                    .completedFuture(scopeGraph.getData(scope).map(AbstractUnit.this::getPreviousDatum));
        }

        @Override public IFuture<Collection<S>> getEdges(S scope, L label) {
            return CompletableFuture.completedFuture(scopeGraph.getEdges(scope, label));
        }

        @Override public IFuture<Tuple2<Boolean, ExtQuerySet<S, L, D>>> dataWf(D datum, ICancel cancel)
                throws InterruptedException {
            final StaticQueryContext queryContext = new StaticQueryContext(sender, scopeGraph);
            return dataWf.wf(datum, queryContext, cancel).thenApply(wf -> {
                return Tuple2.of(wf, AExtQuerySet.<S, L, D>builder().addAllPredicateQueries(queryContext.dispose()).build());
            });
        }

        @Override public IFuture<Tuple2<Boolean, ExtQuerySet<S, L, D>>> dataEquiv(D d1, D d2, ICancel cancel)
                throws InterruptedException {
            final StaticQueryContext queryContext = new StaticQueryContext(sender, scopeGraph);
            return dataLeq.leq(d1, d2, queryContext, cancel).thenApply(wf -> {
                return Tuple2.of(wf, AExtQuerySet.<S, L, D>builder().addAllPredicateQueries(queryContext.dispose()).build());
            });
        }

        @Override public IFuture<Boolean> dataEquivAlwaysTrue(ICancel cancel) {
            final StaticQueryContext queryContext = new StaticQueryContext(sender, scopeGraph);
            return dataLeq.alwaysTrue(queryContext, cancel).thenApply(wf -> {
                if(!queryContext.dispose().isEmpty()) {
                    throw new IllegalStateException("alwaysTrue() evaluation should not perform queries.");
                }
                return wf;
            });
        }

        @Override public ExtQuerySet<S, L, D> unitMetadata() {
            return ExtQuerySets.empty();
        }

        @Override public ExtQuerySet<S, L, D> compose(ExtQuerySet<S, L, D> queries1, ExtQuerySet<S, L, D> queries2) {
            return queries1.addQueries(queries2);
        }

    }

    protected final class StaticQueryContext extends AbstractQueryTypeCheckerContext<S, L, D, R> {

        private final IActorRef<? extends IUnit<S, L, D, ?>> sender;
        private final IScopeGraph.Immutable<S, L, D> scopeGraph;

        private final Set.Transient<IRecordedQuery<S, L, D>> queries = CapsuleUtil.transientSet();
        private boolean disposed = false;

        public StaticQueryContext(IActorRef<? extends IUnit<S, L, D, ?>> sender,
                IScopeGraph.Immutable<S, L, D> scopeGraph) {
            this.sender = sender;
            this.scopeGraph = scopeGraph;
        }

        @Override public String id() {
            return self.id() + "#prev-query";
        }

        @Override public IFuture<? extends java.util.Set<IResolutionPath<S, L, D>>> query(S scope,
                IQuery<S, L, D> query, DataWf<S, L, D> dataWF, DataLeq<S, L, D> dataEquiv,
                DataWf<S, L, D> dataWfInternal, DataLeq<S, L, D> dataEquivInternal) {
            final IResolutionContext<S, L, D, ExtQuerySet<S, L, D>> pContext =
                    new StaticNameResolutionContext(sender, scopeGraph, dataWF, dataEquiv);

            return query.resolve(pContext, new ScopePath<>(scope), context.cancel()).thenApply(ans -> {
                // TODO: should this be conditional?
                recordQuery(ARecordedQuery.of(new ScopePath<S, L>(scope), datumScopes(ans._1()), query.labelWf(),
                        dataWF, ans._1(), true));

                recordQueries(ans._2().transitiveQueries());
                recordQueries(ans._2().predicateQueries());
                return CapsuleUtil.toSet(ans._1());
            });
        }

        @Override public IFuture<? extends java.util.Set<IResolutionPath<S, L, D>>> query(S scope, LabelWf<L> labelWF,
                LabelOrder<L> labelOrder, DataWf<S, L, D> dataWF, DataLeq<S, L, D> dataEquiv,
                DataWf<S, L, D> dataWfInternal, DataLeq<S, L, D> dataEquivInternal) {
            return query(scope, new NameResolutionQuery<>(labelWF, labelOrder, edgeLabels), dataWF, dataEquiv,
                    dataWfInternal, dataEquivInternal);
        }

        @Override public IFuture<? extends java.util.Set<IResolutionPath<S, L, D>>> query(S scope,
                StateMachine<L> stateMachine, DataWf<S, L, D> dataWF, DataLeq<S, L, D> dataEquiv,
                DataWf<S, L, D> dataWfInternal, DataLeq<S, L, D> dataEquivInternal) {
            return query(scope, new StateMachineQuery<>(stateMachine), dataWF, dataEquiv, dataWfInternal,
                    dataEquivInternal);
        }

        // Disposing management

        private void assertUnDisposed() {
            if(disposed) {
                throw new IllegalStateException("Cannot record query after disposing static context.");
            }
        }

        private void recordQueries(java.util.Set<IRecordedQuery<S, L, D>> queries) {
            assertUnDisposed();
            this.queries.__insertAll(queries);
        }

        private void recordQuery(IRecordedQuery<S, L, D> query) {
            assertUnDisposed();
            this.queries.__insert(query);
        }

        public java.util.Set<IRecordedQuery<S, L, D>> dispose() {
            assertUnDisposed();
            disposed = true;
            return queries.freeze();
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

    @Override public void _deadlockQuery(IProcess<S, L, D> i, int m, IProcess<S, L, D> k) {
        cmh.query(i, m, k);
    }

    @Override public void _deadlockReply(IProcess<S, L, D> i, int m, java.util.Set<IProcess<S, L, D>> R) {
        cmh.reply(i, m, R);
    }

    @Override public void _deadlocked(java.util.Set<IProcess<S, L, D>> nodes) {
        self.assertOnActorThread();
        if(!nodes.contains(process)) {
            throw new IllegalStateException("Deadlock unrelated to this unit.");
        }

        if(nodes.size() == 1) {
            if(!failDelays(nodes)) {
                resume();
            }
        } else if(failDelays(nodes)) {
            resume();
        }
    }

    protected void handleDeadlock(java.util.Set<IProcess<S, L, D>> nodes) {
        logger.debug("{} deadlocked with {}", this, nodes);

        if(!nodes.contains(process)) {
            throw new IllegalStateException("Deadlock unrelated to this unit.");
        }

        if(nodes.size() == 1) {
            if(!failDelays(nodes)) {
                failAll();
            } else {
                resume();
            }
            return;
        }

        AggregateFuture.forAll(nodes, node -> node.from(self, context)._state()).whenComplete((states, ex) -> {
            if(ex != null) {
                failures.add(ex);
                return;
            }

            final GraphBuilder<IProcess<S, L, D>> wfgBuilder = GraphBuilder.of();
            states.forEach(state -> {
                final IProcess<S, L, D> self = state.getSelf();
                wfgBuilder.addVertex(self);
                state.getDependencies().forEach(dep -> {
                    wfgBuilder.addEdge(self, dep);
                });
            });

            final IGraph<IProcess<S, L, D>> wfg = wfgBuilder.build();

            logger.debug("{} computing SCCs in graph.");

            final java.util.Set<java.util.Set<IProcess<S, L, D>>> sccs = DeadlockUtils.sccs(wfg);

            for(java.util.Set<IProcess<S, L, D>> scc : sccs) {
                if(isIncrementalDeadlockEnabled()) {
                    // @formatter:off
                    java.util.Set<StateSummary<S, L,D>> subStates = states.stream()
                            .filter(ss -> scc.contains(ss.getSelf()))
                            .collect(Collectors.toSet());
                    // @formatter:on
                    handleDeadlockIncremental(scc, subStates);
                } else {
                    handleDeadlockRegular(scc);
                }
            }
        });
    }

    private void handleDeadlockIncremental(java.util.Set<IProcess<S, L, D>> nodes,
            Collection<StateSummary<S, L, D>> states) {
        logger.debug("Handling incremental deadlock: {}.", states);
        final Map<StateSummary.State, java.util.Set<StateSummary<S, L, D>>> groupedStates =
                states.stream().collect(Collectors.groupingBy(StateSummary::getState, Collectors.toSet()));

        if(!groupedStates.containsKey(StateSummary.State.UNKNOWN)) {
            logger.debug("No restartable units, doing regular deadlock handling.");
            handleDeadlockRegular(nodes);
        } else if(!groupedStates.containsKey(StateSummary.State.ACTIVE)) {
            // No active units in cluster, release all involved units.
            logger.debug("Releasing all involved units.");
            groupedStates.get(StateSummary.State.UNKNOWN)
                    .forEach(node -> node.getSelf().from(self, context)._release());
        } else {
            final java.util.Set<StateSummary<S, L, D>> activeProcesses = groupedStates.get(StateSummary.State.ACTIVE);
            // @formatter:off
            final java.util.Set<IProcess<S, L, D>> restarts = groupedStates.get(StateSummary.State.UNKNOWN).stream()
                    .filter(node -> shouldRestart(node, activeProcesses))
                    .map(StateSummary::getSelf)
                    .collect(Collectors.toSet());
            // @formatter:on
            if(restarts.isEmpty()) {
                logger.warn(
                        "Conservative deadlock detection failed: Active units have no incoming dependencies elegible for restart. Restarting all unknown units");
                groupedStates.get(StateSummary.State.UNKNOWN)
                        .forEach(node -> node.getSelf().from(self, context)._restart());
            } else {
                logger.debug("Restarting {}.", restarts);
                restarts.forEach(node -> node.from(self, context)._restart());
                logger.debug("Restarted {}.", restarts);
            }
        }
    }

    private boolean shouldRestart(StateSummary<S, L, D> node, Collection<StateSummary<S, L, D>> activeProcesses) {
        // @formatter:off
        java.util.Set<IProcess<S, L, D>> activeNodes = activeProcesses.stream()
            .map(StateSummary::getSelf)
            .collect(Collectors.toSet());
        // @formatter:on
        return node.getDependencies().stream().anyMatch(activeNodes::contains);
    }

    private void handleDeadlockRegular(java.util.Set<IProcess<S, L, D>> nodes) {
        // All units are already active, proceed with regular deadlock handling
        if(nodes.size() == 1 && nodes.contains(process)) {
            logger.debug("{} self-deadlocked with {}", this, getTokens(process));
            if(failDelays(nodes)) {
                resume(); // resume to ensure further deadlock detection after these are handled
            } else if (dependentSet().equals(Collections.singleton(process))) {
                failAll();
            }
        } else {
            for(IProcess<S, L, D> node : nodes) {
                node.from(self, context)._deadlocked(nodes);
            }
        }
    }

    /**
     * Fail delays that are part of the deadlock. If any delays can be failed, computations should be able to continue
     * (or fail following the exception).
     *
     * The set of open scopes and labels is unchanged, and it is safe for the type checker to continue.
     */
    private boolean failDelays(Collection<IProcess<S, L, D>> nodes) {
        final Set.Transient<ICompletable<?>> deadlocked = CapsuleUtil.transientSet();
        for(Delay delay : delays.inverse().keySet()) {
            if(nodes.contains(process(delay.sender))) {
                logger.debug("{} fail {}", self, delay);
                delays.inverse().remove(delay);
                deadlocked.__insert(delay.future);
            }
        }
        for(IProcess<S, L, D> node : nodes) {
            for(IWaitFor<S, L, D> wf : getTokens(node)) {
                // @formatter:off
                wf.visit(IWaitFor.cases(
                    initScope -> {},
                    closeScope -> {},
                    closeLabel -> {},
                    query -> {},
                    pQuery -> {},
                    confirm -> {},
                    match -> {},
                    result  -> {},
                    typeCheckerState -> {
                        if(nodes.contains(process(typeCheckerState.origin()))) {
                            logger.debug("{} fail {}", self, typeCheckerState);
                            deadlocked.__insert(typeCheckerState.future());
                        }
                    },
                    differResult -> {},
                    differState -> {
                        /* if(nodes.contains(process(differState.origin()))) {
                            logger.debug("{} fail {}", self, differState);
                            deadlocked.__insert(differState.future());
                        } */
                    },
                    envDifferState -> {
                        /* if(nodes.contains(process(envDifferState.origin()))) {
                            logger.debug("{} fail {}", self, envDifferState);
                            deadlocked.__insert(envDifferState.future());
                        } */
                    },
                    unitAdd -> {}
                ));
                // @formatter:on
            }
        }
        for(ICompletable<?> future : deadlocked) {
            self.complete(future, null, new DeadlockException("Type checker " + self.id() + " deadlocked."));
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
        for(IWaitFor<S, L, D> wf : CapsuleUtil.toSet(wfg.getTokens())) {
            // @formatter:off
            wf.visit(IWaitFor.cases(
                initScope -> {
                    failures.add(new DeadlockException(initScope.toString()));
                    granted(initScope, self);
                    if(!isOwner(initScope.scope())) {
                        self.async(parent)._initShare(initScope.scope(), CapsuleUtil.immutableSet(), false);
                    }
                    releaseDelays(initScope.scope());
                },
                closeScope -> {
                    failures.add(new DeadlockException(closeScope.toString()));
                    granted(closeScope, self);
                    if(!isOwner(closeScope.scope())) {
                        self.async(parent)._doneSharing(closeScope.scope());
                    }
                    releaseDelays(closeScope.scope());
                },
                closeLabel -> {
                    failures.add(new DeadlockException(closeLabel.toString()));
                    granted(closeLabel, self);
                    if(!isOwner(closeLabel.scope())) {
                        self.async(parent)._closeEdge(closeLabel.scope(), closeLabel.label());
                    }
                    releaseDelays(closeLabel.scope(), closeLabel.label());
                },
                query -> {
                    logger.error("Unexpected remaining query: " + query);
                    throw new IllegalStateException("Unexpected remaining query: " + query);
                },
                pQuery -> {
                    logger.error("Unexpected remaining query: " + pQuery);
                    throw new IllegalStateException("Unexpected remaining query: " + pQuery);
                },
                confirm -> {
                    logger.error("Unexpected remaining confirmation request: " + confirm);
                    throw new IllegalStateException("Unexpected remaining confirmation request: " + confirm);
                },
                match -> {
                    logger.error("Unexpected remaining match query: " + match);
                    throw new IllegalStateException("Unexpected remaining match query: " + match);
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
                },
                differResult -> {
                    logger.error("Differ could not complete.");
                    self.complete(differResult.future(), null, new DeadlockException("Type checker deadlocked."));
                },
                differState -> {
                    logger.error("Unexpected remaining differ state.");
                    self.complete(differState.future(), null, new DeadlockException("Type checker deadlocked."));
                },
                envDifferState -> {
                    logger.error("Unexpected remaining environment differ state.");
                    self.complete(envDifferState.future(), null, new DeadlockException("Type checker deadlocked."));
                },
                unitAdd -> {
                    logger.error("Requested Unit {} was never added.", unitAdd.unitId());
                    self.complete(unitAdd.future(), null, new DeadlockException("Requested Unit " + unitAdd.unitId() + " was never added."));
                }
            ));
            // @formatter:on
        }


    }

    protected boolean isIncrementalEnabled() {
        return context.settings().isIncremental();
    }

    protected boolean isQueryRecordingEnabled() {
        return context.settings().recording();
    }

    protected boolean isIncrementalDeadlockEnabled() {
        return context.settings().incrementalDeadlock();
    }

    ///////////////////////////////////////////////////////////////////////////
    // ChandryMisraHaas.Host
    ///////////////////////////////////////////////////////////////////////////

    @Override public IProcess<S, L, D> process() {
        return process;
    }

    @Override public java.util.Set<IProcess<S, L, D>> dependentSet() {
        return wfg.dependencies();
    }

    @Override public void query(IProcess<S, L, D> k, IProcess<S, L, D> i, int m) {
        k.from(self, context)._deadlockQuery(i, m, process);
    }

    @Override public void reply(IProcess<S, L, D> k, IProcess<S, L, D> i, int m, java.util.Set<IProcess<S, L, D>> R) {
        k.from(self, context)._deadlockReply(i, m, R);
    }

    @Override public void assertOnActorThread() {
        self.assertOnActorThread();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Scope graph diffing
    ///////////////////////////////////////////////////////////////////////////

    protected IDifferContext<S, L, D> differContext(Function1<D, D> instantiateData) {
        return new IDifferContext<S, L, D>() {

            @Override public IFuture<Collection<S>> getEdges(S scope, L label) {
                final IFuture<Unit> complete = isComplete(scope, EdgeOrData.edge(label), self);
                if(complete.isDone()) {
                    return CompletableFuture.completedFuture(scopeGraph.get().getEdges(scope, label));
                }

                final ICompletableFuture<Collection<S>> result = new CompletableFuture<>();
                complete.whenComplete((__, ex) -> {
                    if(ex != null) {
                        if(ex instanceof DeadlockException) {
                            getEdges(scope, label).whenComplete(result::complete);
                        } else {
                            result.completeExceptionally(ex);
                        }
                    } else {
                        result.complete(scopeGraph.get().getEdges(scope, label));
                    }
                });
                return result;
            }

            @Override public IFuture<Optional<D>> datum(S scope) {
                assertOwnScope(scope);
                return isComplete(scope, EdgeOrData.data(), self).thenCompose(__ -> {
                    final Optional<D> datum = scopeGraph.get().getData(scope);
                    if(!datum.isPresent()) {
                        return CompletableFuture.completedFuture(datum);
                    }

                    final ICompletableFuture<D> future = new CompletableFuture<>();
                    final TypeCheckerState<S, L, D> state =
                            TypeCheckerState.of(self, Arrays.asList(datum.get()), future);
                    waitFor(state, self);
                    getExternalDatum(datum.get()).whenComplete(future::complete);
                    return future.whenComplete((r, ex) -> {
                        granted(state, self);
                    }).thenApply(Optional::of);
                });
            }

            @Override public Optional<D> rawDatum(S scope) {
                return scopeGraph.get().getData(scope).map(instantiateData::apply);
            }

            @Override public boolean available(S scope) {
                return scopes.contains(scope);
            }

            @Override public String toString() {
                return "DifferContext(" + self.id() + "):\n"
                        + ScopeGraphUtil.toString(scopeGraph.get(), instantiateData);
            }
        };
    }

    protected IDifferOps<S, L, D> differOps() {
        return new IDifferOps<S, L, D>() {
            @Override public boolean isMatchAllowed(S currentScope, S previousScope) {
                return context.scopeId(previousScope).equals(context.scopeId(currentScope));
            }

            @Override public Optional<BiMap.Immutable<S>> matchDatums(D currentDatum, D previousDatum) {
                return context.matchDatums(currentDatum, previousDatum);
            }

            @Override public Collection<S> getScopes(D d) {
                return context.getScopes(d);
            }

            @Override public D embed(S scope) {
                return context.embed(scope);
            }

            @Override public boolean ownScope(S scope) {
                return context.scopeId(scope).equals(self.id());
            }

            @Override public boolean ownOrSharedScope(S currentScope) {
                return scopes.contains(currentScope);
            }

            @Override public IFuture<Optional<S>> externalMatch(S previousScope) {
                return getOwner(previousScope).thenCompose(owner -> {
                    final Match<S, L, D> match = Match.of(self, previousScope);
                    waitFor(match, owner);
                    return self.async(owner)._match(previousScope).whenComplete((__, ___) -> {
                        granted(match, owner);
                    });
                });
            }
        };
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

    protected static class Stats implements IUnitStats, Serializable {

        private static final long serialVersionUID = 42L;

        protected int localQueries;
        protected int incomingQueries;
        protected int outgoingQueries;
        protected int forwardedQueries;
        protected int incomingConfirmations;
        protected long runtimeNanos;
        protected int dataWfChecks;
        protected int dataLeqChecks;

        private IActorStats actorStats;

        private Stats(IActorStats actorStats) {
            this.actorStats = actorStats;
        }

        @Override public Collection<String> csvHeaders() {
            final ImList.Mutable<String> builder = ImList.Mutable.of(
                "runtimeMillis",
                "localQueries",
                "incomingQueries",
                "outgoingQueries",
                "forwardedQueries",
                "incomingConfirmations",
                "dataWfChecks",
                "dataLeqChecks"
            );
            builder.addAll(actorStats.csvHeaders());
            return builder.freeze();
        }

        @Override public Collection<String> csvRow() {
            final ImList.Mutable<String> builder = ImList.Mutable.of(
                Long.toString(TimeUnit.MILLISECONDS.convert(runtimeNanos, TimeUnit.NANOSECONDS)),
                Integer.toString(localQueries),
                Integer.toString(incomingQueries),
                Integer.toString(outgoingQueries),
                Integer.toString(forwardedQueries),
                Integer.toString(incomingConfirmations),
                Integer.toString(dataWfChecks),
                Integer.toString(dataLeqChecks)
            );
            builder.addAll(actorStats.csvRow());
            return builder.freeze();
        }

        @Override public String toString() {
            return "UnitStats{ownQueries=" + localQueries + ",foreignQueries=" + incomingQueries + ",forwardedQueries="
                    + outgoingQueries + "," + actorStats + "}";
        }

    }

    ///////////////////////////////////////////////////////////////////////////
    // Assertions
    ///////////////////////////////////////////////////////////////////////////

    protected void assertOwnScope(S scope) {
        if(!context.scopeId(scope).equals(self.id())) {
            logger.error("Scope {} is not owned by {}", scope, this);
            throw new IllegalArgumentException("Scope " + scope + " is not owned by " + this);
        }
    }

    protected void assertOwnOrSharedScope(S scope) {
        if(!scopes.contains(scope)) {
            logger.error("Scope {} is not owned or shared by {}", scope, this);
            throw new IllegalArgumentException("Scope " + scope + " is not owned or shared by " + this);
        }
    }

    protected void assertLabelOpen(S scope, EdgeOrData<L> edge) {
        assertOwnOrSharedScope(scope);
        if(isEdgeClosed(scope, edge)) {
            logger.error("Label {}/{} is not open on {}.", scope, edge, self);
            throw new IllegalArgumentException("Label " + scope + "/" + edge + " is not open on " + self + ".");
        }
    }

    protected void assertDifferEnabled() {
        if(!isDifferEnabled()) {
            logger.error("Scope graph differ is not enabled.");
            throw new IllegalStateException("Scope graph differ is not enabled.");
        }
    }

    protected void assertIncrementalDeadlockEnabled() {
        if(!isIncrementalDeadlockEnabled()) {
            logger.error("Deadlock resolution for incremental analysis is not enabled.");
            throw new IllegalStateException("Deadlock resolution for incremental analysis is not enabled.");
        }
    }
}
