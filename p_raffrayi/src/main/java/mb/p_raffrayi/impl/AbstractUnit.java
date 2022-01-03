package mb.p_raffrayi.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.metaborg.util.Ref;
import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.HashTrieRelation3;
import org.metaborg.util.collection.IRelation3;
import org.metaborg.util.collection.MultiSet;
import org.metaborg.util.collection.MultiSetMap;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.Function2;
import org.metaborg.util.future.AggregateFuture;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.ICompletable;
import org.metaborg.util.future.ICompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.tuple.Tuple2;
import org.metaborg.util.unit.Unit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;

import io.usethesource.capsule.Set;
import mb.p_raffrayi.DeadlockException;
import mb.p_raffrayi.IRecordedQuery;
import mb.p_raffrayi.IResult;
import mb.p_raffrayi.ITypeCheckerContext;
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
import mb.scopegraph.ecoop21.INameResolutionContext;
import mb.scopegraph.ecoop21.LabelOrder;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.ecoop21.NameResolution;
import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.ScopeGraphUtil;
import mb.scopegraph.oopsla20.diff.BiMap;
import mb.scopegraph.oopsla20.diff.ScopeGraphDiff;
import mb.scopegraph.oopsla20.path.IResolutionPath;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.scopegraph.oopsla20.reference.Env;
import mb.scopegraph.oopsla20.reference.ScopeGraph;
import mb.scopegraph.oopsla20.terms.newPath.ResolutionPath;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

public abstract class AbstractUnit<S, L, D, R extends IResult<S, L, D>, T>
        implements IUnit<S, L, D, R, T>, IActorMonitor, Host<IProcess<S, L, D>> {

    private static final ILogger logger = LoggerUtils.logger(IUnit.class);

    protected final TypeTag<IUnit<S, L, D, ?, ?>> TYPE = TypeTag.of(IUnit.class);

    protected final IActor<? extends IUnit<S, L, D, R, T>> self;
    protected final @Nullable IActorRef<? extends IUnit<S, L, D, ?, ?>> parent;
    protected final IUnitContext<S, L, D> context;

    protected final ChandyMisraHaas<IProcess<S, L, D>> cmh;
    protected final UnitProcess<S, L, D> process;
    private final BrokerProcess<S, L, D> broker = BrokerProcess.of();

    private volatile boolean innerResult;
    private final Ref<R> analysis;
    protected final List<Throwable> failures;
    private final Map<String, IUnitResult<S, L, D, ?, ?>> subUnitResults;
    private final ICompletableFuture<IUnitResult<S, L, D, R, T>> unitResult;

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
    private final Ref<StateCapture<S, L, D, T>> localCapture = new Ref<>();

    protected TransitionTrace stateTransitionTrace = TransitionTrace.OTHER;
    protected final Stats stats;

    public AbstractUnit(IActor<? extends IUnit<S, L, D, R, T>> self,
            @Nullable IActorRef<? extends IUnit<S, L, D, ?, ?>> parent, IUnitContext<S, L, D> context,
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
        this.usedStableScopes = Set.Transient.of();

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

    @Override public IFuture<IQueryAnswer<S, L, D>> _query(IActorRef<? extends IUnit<S, L, D, ?, ?>> origin,
            ScopePath<S, L> path, LabelWf<L> labelWF, DataWf<S, L, D> dataWF, LabelOrder<L> labelOrder,
            DataLeq<S, L, D> dataEquiv) {
        // resume(); // FIXME necessary?
        stats.incomingQueries += 1;
        return doQuery(self.sender(TYPE), origin, false, path, labelWF, labelOrder, dataWF, dataEquiv, null, null);
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

    protected final IFuture<IUnitResult<S, L, D, R, T>> doFinish(IFuture<R> result) {
        final ICompletableFuture<R> internalResult = new CompletableFuture<>();
        final TypeCheckerResult<S, L, D> token = TypeCheckerResult.of(self, internalResult);
        waitFor(token, self);
        result.whenComplete(internalResult::complete); // FIXME self.schedule(result)?
        internalResult.whenComplete((r, ex) -> {
            final String id = self.id();
            logger.debug("{} type checker finished", id);
            resume(); // FIXME necessary?
            if(isDifferEnabled()) {
                whenDifferActivated.thenAccept(__ -> differ.typeCheckerFinished());
            }
            if(ex != null) {
                logger.error("type checker errored: {}", ex);
                failures.add(ex);
            } else {
                analysis.set(r);
            }
            granted(token, self);
            final MultiSet.Immutable<IWaitFor<S, L, D>> selfTokens = getTokens(process);
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
        startDiffer(currentRootScopes, previousRootScopes);
        self.complete(whenDifferActivated, Unit.unit, null);
    }

    private void startDiffer(List<S> currentRootScopes, List<S> previousRootScopes) {
        final ICompletableFuture<ScopeGraphDiff<S, L, D>> differResult = new CompletableFuture<>();

        // Handle diff output
        final DifferResult<S, L, D> result = DifferResult.of(self, differResult);
        waitFor(result, self);
        self.schedule(differ.diff(currentRootScopes, previousRootScopes)).whenComplete(differResult::complete);
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

    protected <Q extends IResult<S, L, D>, U>
            Tuple2<IActorRef<? extends IUnit<S, L, D, Q, U>>, IFuture<IUnitResult<S, L, D, Q, U>>>
            doAddSubUnit(String id,
                    Function2<IActor<IUnit<S, L, D, Q, U>>, IUnitContext<S, L, D>, IUnit<S, L, D, Q, U>> unitProvider,
                    List<S> rootScopes, boolean ignoreResult) {
        for(S rootScope : rootScopes) {
            assertOwnOrSharedScope(rootScope);
        }

        final Tuple2<IFuture<IUnitResult<S, L, D, Q, U>>, IActorRef<? extends IUnit<S, L, D, Q, U>>> result_subunit =
                context.add(id, unitProvider, rootScopes);
        final IActorRef<? extends IUnit<S, L, D, Q, U>> subunit = result_subunit._2();

        final ICompletableFuture<IUnitResult<S, L, D, Q, U>> internalResult = new CompletableFuture<>();
        final TypeCheckerResult<S, L, D> token = TypeCheckerResult.of(self, internalResult);
        waitFor(token, subunit);
        result_subunit._1().whenComplete(internalResult::complete); // must come after waitFor

        for(S rootScope : CapsuleUtil.toSet(rootScopes)) {
            doAddShare(subunit, rootScope);
        }

        final IFuture<IUnitResult<S, L, D, Q, U>> ret = internalResult.whenComplete((r, ex) -> {
            logger.debug("{} subunit {} finished", this, subunit);
            granted(token, subunit);
            resume();
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
        final List<EdgeOrData<L>> labels = Lists.newArrayList();
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
        final IActorRef<? extends IUnit<S, L, D, ?, ?>> sender = self.sender(TYPE);
        if(isDifferEnabled()) {
            return whenDifferActivated.thenCompose(__ -> {
                final ICompletableFuture<Optional<S>> future = new CompletableFuture<>();
                final DifferState<S, L, D> state = DifferState.ofMatch(sender, previousScope, future);
                waitFor(state, self);
                differ.match(previousScope).whenComplete(future::complete);
                future.whenComplete((r, ex) -> {
                    granted(state, self);
                    resume(); // FIXME needed?
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

    protected final void doAddLocalShare(IActorRef<? extends IUnit<S, L, D, ?, ?>> sender, S scope) {
        assertOwnOrSharedScope(scope);

        waitFor(InitScope.of(self, scope), sender);
    }

    protected final void doAddShare(IActorRef<? extends IUnit<S, L, D, ?, ?>> sender, S scope) {
        doAddLocalShare(sender, scope);

        if(!isOwner(scope)) {
            self.async(parent)._addShare(scope);
        }
    }

    protected final void doInitShare(IActorRef<? extends IUnit<S, L, D, ?, ?>> sender, S scope,
            Iterable<EdgeOrData<L>> edges, boolean sharing) {
        assertOwnOrSharedScope(scope);

        granted(InitScope.of(self, scope), sender);
        resume(); // FIXME necessary?

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

    protected final void doCloseScope(IActorRef<? extends IUnit<S, L, D, ?, ?>> sender, S scope) {
        assertOwnOrSharedScope(scope);

        granted(CloseScope.of(self, scope), sender);
        resume(); // FIXME necessary?

        if(isScopeInitialized(scope)) {
            releaseDelays(scope);
        }

        if(!isOwner(scope)) {
            self.async(parent)._doneSharing(scope);
        }
    }

    protected final void doCloseLabel(IActorRef<? extends IUnit<S, L, D, ?, ?>> sender, S scope, EdgeOrData<L> edge) {
        assertOwnOrSharedScope(scope);

        granted(CloseLabel.of(self, scope, edge), sender);
        resume(); // FIXME necessary?

        if(isEdgeClosed(scope, edge)) {
            releaseDelays(scope, edge);
        }

        if(!isOwner(scope)) {
            self.async(parent)._closeEdge(scope, edge);
        }
    }

    protected final void doAddEdge(@SuppressWarnings("unused") IActorRef<? extends IUnit<S, L, D, ?, ?>> sender,
            S source, L label, S target) {
        assertOwnOrSharedScope(source);
        assertLabelOpen(source, EdgeOrData.edge(label));

        scopeGraph.set(scopeGraph.get().addEdge(source, label, target));

        if(!isOwner(source)) {
            self.async(parent)._addEdge(source, label, target);
        }
    }

    protected final IFuture<IQueryAnswer<S, L, D>> doQuery(IActorRef<? extends IUnit<S, L, D, ?, ?>> sender,
            IActorRef<? extends IUnit<S, L, D, ?, ?>> origin, boolean record, ScopePath<S, L> path, LabelWf<L> labelWF,
            LabelOrder<L> labelOrder, DataWf<S, L, D> dataWF, DataLeq<S, L, D> dataEquiv,
            DataWf<S, L, D> dataWfInternal, DataLeq<S, L, D> dataEquivInternal) {
        final ILogger logger = LoggerUtils.logger(INameResolutionContext.class);
        logger.debug("got _query from {}", sender);

        final boolean external = !sender.equals(self);
        final ImmutableSet.Builder<IRecordedQuery<S, L, D>> transitiveQueries = ImmutableSet.builder();
        final ImmutableSet.Builder<IRecordedQuery<S, L, D>> predicateQueries = ImmutableSet.builder();
        final ITypeCheckerContext<S, L, D> queryContext = queryContext(predicateQueries, origin);

        final INameResolutionContext<S, L, D> nrc = new INameResolutionContext<S, L, D>() {

            @Override public IFuture<Env<S, L, D>> externalEnv(LocalEnv<S, L, D> context, ScopePath<S, L> path,
                    LabelWf<L> re, LabelOrder<L> labelOrder, ICancel cancel) {
                final S scope = path.getTarget();
                if(canAnswer(scope)) {
                    logger.debug("local env {}", scope);
                    return context.localEnv(path, re, cancel).thenApply(ans -> {
                        if(isQueryRecordingEnabled() && record && sharedScopes.contains(scope)) {
                            recordedQueries.add(RecordedQuery.of(path, datumScopes(ans), re, dataWF, ans));
                        }

                        return ans;
                    });
                } else {
                    return getOwner(scope).thenCompose(owner -> {
                        logger.debug("remote env {} at {}", scope, owner);
                        // this code mirrors query(...)
                        final IFuture<IQueryAnswer<S, L, D>> result =
                                self.async(owner)._query(origin, path, re, dataWF, labelOrder, dataEquiv);
                        final Query<S, L, D> wf = Query.of(sender, path, re, dataWF, labelOrder, dataEquiv, result);
                        waitFor(wf, owner);
                        if(external) {
                            stats.forwardedQueries += 1;
                        } else {
                            stats.outgoingQueries += 1;
                        }
                        return result.thenApply(ans -> {
                            if(isQueryRecordingEnabled()) {
                                if(external) {
                                    // For external queries, track this query as transitive.
                                    transitiveQueries
                                            .add(RecordedQuery.of(path, datumScopes(ans.env()), re, dataWF, ans.env()));
                                    transitiveQueries.addAll(ans.transitiveQueries());
                                    predicateQueries.addAll(ans.predicateQueries());
                                } else if(record) {
                                    // For local query, record it as such.
                                    recordedQueries.add(RecordedQuery.of(path, datumScopes(ans.env()), re, dataWF, ans.env(), false));
                                    recordedQueries.addAll(ans.transitiveQueries());
                                    recordedQueries.addAll(ans.predicateQueries());
                                }
                            }
                            return ans.env();
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
                                    TypeCheckerState.of(sender, ImmutableList.of(datum.get()), internalResult);
                            waitFor(token, self);
                            result.whenComplete(internalResult::complete); // must come after waitFor
                            ret = internalResult.whenComplete((rep, ex) -> {
                                self.assertOnActorThread();
                                granted(token, self);
                                resume();
                            });
                            resume();
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

            @Override public IFuture<Iterable<S>> getEdges(S scope, L label) {
                return isComplete(scope, EdgeOrData.edge(label), sender).thenApply(__ -> {
                    return scopeGraph.get().getEdges(scope, label);
                });
            }

            @Override public IFuture<Boolean> dataWf(D d, ICancel cancel) throws InterruptedException {
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

            @Override public IFuture<Boolean> dataLeq(D d1, D d2, ICancel cancel) throws InterruptedException {
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

            @Override public IFuture<Boolean> dataLeqAlwaysTrue(ICancel cancel) {
                return dataEquiv.alwaysTrue(queryContext, cancel);
            }

        };
        final NameResolution<S, L, D> nr = new NameResolution<S, L, D>(edgeLabels, labelOrder, nrc);

        final IFuture<Env<S, L, D>> result = nr.env(path, labelWF, context.cancel());
        result.whenComplete((env, ex) -> {
            logger.debug("have answer for {}", sender);
        });
        return result.thenApply(env -> QueryAnswer.of(env, transitiveQueries.build(), predicateQueries.build()));
    }

    private final ITypeCheckerContext<S, L, D> queryContext(ImmutableSet.Builder<IRecordedQuery<S, L, D>> queries,
            IActorRef<? extends IUnit<S, L, D, ?, ?>> origin) {
        return new AbstractQueryTypeCheckerContext<S, L, D, R>() {

            @Override public String id() {
                return self.id() + "#query";
            }

            @Override public IFuture<Set.Immutable<IResolutionPath<S, L, D>>> query(S scope, LabelWf<L> labelWF,
                    LabelOrder<L> labelOrder, DataWf<S, L, D> dataWF, DataLeq<S, L, D> dataEquiv,
                    @Nullable DataWf<S, L, D> dataWfInternal, @Nullable DataLeq<S, L, D> dataEquivInternal) {
                // does not require the Unit to be ACTIVE

                final ScopePath<S, L> path = new ScopePath<>(scope);
                // If record is true, a potentially hidden scope from the predicate may leak to the recordedQueries of the receiver.
                final IFuture<IQueryAnswer<S, L, D>> result = doQuery(self, origin, false, path, labelWF, labelOrder,
                        dataWF, dataEquiv, dataWfInternal, dataEquivInternal);
                final Query<S, L, D> wf = Query.of(self, path, labelWF, dataWF, labelOrder, dataEquiv, result);
                waitFor(wf, self);
                stats.localQueries += 1;
                return self.schedule(result).thenApply(ans -> {
                    if(isQueryRecordingEnabled()) {
                        // Type-checkers can embed scopes in their predicates that are not accessible from the outside.
                        // If such a query is confirmed, the scope graph differ will never produce a scope diff for it,
                        // leading to exceptions. However, since the query is local, it is not required to verify it anyway.
                        // Hence, we just ignore it.
                        if(!context.scopeId(path.getTarget()).equals(origin.id())) {
                            queries.add(ARecordedQuery.of(path, datumScopes(ans.env()), labelWF, dataWF, ans.env(), true));
                        }
                        ans.transitiveQueries().forEach(q -> queries.add(q.withIncludePatches(false)));
                        queries.addAll(ans.predicateQueries());
                    }
                    return CapsuleUtil.<IResolutionPath<S, L, D>>toSet(ans.env());
                }).whenComplete((ans, ex) -> {
                    granted(wf, self);
                    resume(); // FIXME needed?
                });
            }
        };
    }

    protected final IFuture<Env<S, L, D>> doQueryPrevious(IActorRef<? extends IUnit<S, L, D, ?, ?>> sender,
            IScopeGraph.Immutable<S, L, D> scopeGraph, ScopePath<S, L> path, LabelWf<L> labelWF, DataWf<S, L, D> dataWF,
            LabelOrder<L> labelOrder, DataLeq<S, L, D> dataEquiv) {
        logger.debug("rec pquery from {}", sender);
        // TODO: fix entanglement between StaticNameResolutionContext and StaticQueryContext
        final INameResolutionContext<S, L, D> nrc = new StaticNameResolutionContext(sender, scopeGraph,
                new StaticQueryContext(sender, scopeGraph), dataWF, dataEquiv);
        final NameResolution<S, L, D> nr = new NameResolution<>(edgeLabels, labelOrder, nrc);

        return nr.env(path, labelWF, context.cancel());
    }

    protected final boolean isOwner(S scope) {
        return context.scopeId(scope).equals(self.id());
    }

    protected final IFuture<IActorRef<? extends IUnit<S, L, D, ?, ?>>> getOwner(S scope) {
        final IFuture<IActorRef<? extends IUnit<S, L, D, ?, ?>>> future = context.owner(scope);
        if(future.isDone()) {
            return future;
        }
        final ICompletableFuture<IActorRef<? extends IUnit<S, L, D, ?, ?>>> result = new CompletableFuture<>();
        final UnitAdd<S, L, D> unitAdd = UnitAdd.of(self, context.scopeId(scope), result);
        waitFor(unitAdd, broker);
        // Due to the synchronous nature of the Broker, this future may be completed by
        // another unit's thread. Hence we wrap it in self.schedule, so we do not break
        // the actor abstraction.
        self.schedule(context.owner(scope)).whenComplete((r, ex) -> {
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
        return Streams.stream(env).map(ResolutionPath::getDatum).map(context::getScopes)
                .reduce(CapsuleUtil.immutableSet(), Set.Immutable::__insertAll);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Scopegraph Capture
    ///////////////////////////////////////////////////////////////////////////

    protected void localCapture(StateCapture<S, L, D, T> capture) {
        if(localCapture.get() != null) {
            logger.error("Cannot create multiple local captures.");
            throw new IllegalStateException("Cannot create multiple local captures.");
        }
        localCapture.set(capture);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Wait fors & finalization
    ///////////////////////////////////////////////////////////////////////////

    private MultiSet.Immutable<IWaitFor<S, L, D>> waitFors = MultiSet.Immutable.of();
    private MultiSetMap.Immutable<IProcess<S, L, D>, IWaitFor<S, L, D>> waitForsByProcess = MultiSetMap.Immutable.of();

    protected boolean isWaiting() {
        return !waitFors.isEmpty();
    }

    protected boolean isWaitingFor(IWaitFor<S, L, D> token) {
        return waitFors.contains(token);
    }

    protected boolean isWaitingFor(IWaitFor<S, L, D> token, IActor<? extends IUnit<S, L, D, R, T>> from) {
        return waitForsByProcess.get(new UnitProcess<>(from)).contains(token);
    }

    protected int countWaitingFor(IWaitFor<S, L, D> token, IActorRef<? extends IUnit<S, L, D, ?, ?>> from) {
        return waitForsByProcess.get(new UnitProcess<>(from)).count(token);
    }

    private MultiSet.Immutable<IWaitFor<S, L, D>> getTokens(IProcess<S, L, D> unit) {
        return waitForsByProcess.get(unit);
    }

    protected MultiSet.Immutable<IWaitFor<S, L, D>> ownTokens() {
        return getTokens(process);
    }

    protected void waitFor(IWaitFor<S, L, D> token, IActorRef<? extends IUnit<S, L, D, ?, ?>> actor) {
        waitFor(token, process(actor));
    }

    protected void waitFor(IWaitFor<S, L, D> token, IProcess<S, L, D> process) {
        resume(); // Always needed?
        logger.debug("{} wait for {}/{}", self, process, token);
        waitFors = waitFors.add(token);
        waitForsByProcess = waitForsByProcess.put(process, token);
    }

    protected void granted(IWaitFor<S, L, D> token, IActorRef<? extends IUnit<S, L, D, ?, ?>> actor) {
        granted(token, process(actor));
    }

    protected void granted(IWaitFor<S, L, D> token, IProcess<S, L, D> process) {
        // resume();
        if(!waitForsByProcess.contains(process, token)) {
            logger.error("{} not waiting for granted {}/{}", self, process, token);
            throw new IllegalStateException(self + " not waiting for granted " + process + "/" + token);
        }
        logger.debug("{} granted {} by {}", self, token, process);
        waitFors = waitFors.remove(token);
        waitForsByProcess = waitForsByProcess.remove(process, token);
    }

    private IProcess<S, L, D> process(IActorRef<? extends IUnit<S, L, D, ?, ?>> actor) {
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
            unitResult.complete(UnitResult.<S, L, D, R, T>builder()
                .id(self.id())
                .scopeGraph(scopeGraph.get())
                .localScopeGraph(localScopeGraph())
                .queries(recordedQueries)
                .rootScopes(rootScopes)
                .scopes(scopes.freeze())
                .analysis(analysis.get())
                .localState(localCapture.get())
                .failures(failures)
                .subUnitResults(subUnitResults)
                .stats(stats)
                .stateTransitionTrace(stateTransitionTrace)
                .diff(diffResult.get())
                .build()
            );
            // @formatter:on
        } else {
            logger.trace("Still waiting for {}{}", innerResult ? "inner result and " : "", waitForsByProcess);
        }
    }

    protected IScopeGraph.Immutable<S, L, D> localScopeGraph() {
        return scopeGraph.get();
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
            IActorRef<? extends IUnit<S, L, D, ?, ?>> sender) {
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
        public final IActorRef<? extends IUnit<S, L, D, ?, ?>> sender;

        Delay(ICompletableFuture<org.metaborg.util.unit.Unit> future,
                IActorRef<? extends IUnit<S, L, D, ?, ?>> sender) {
            this.future = future;
            this.sender = sender;
        }

        @Override public String toString() {
            return "Delay{future=" + future + ",sender=" + sender + "}";
        }

    }

    protected final class StaticNameResolutionContext implements INameResolutionContext<S, L, D> {

        private final IActorRef<? extends IUnit<S, L, D, ?, ?>> sender;
        private final DataLeq<S, L, D> dataLeq;
        private final ITypeCheckerContext<S, L, D> queryContext;
        private final IScopeGraph.Immutable<S, L, D> scopeGraph;
        private final DataWf<S, L, D> dataWf;

        protected StaticNameResolutionContext(IActorRef<? extends IUnit<S, L, D, ?, ?>> sender,
                IScopeGraph.Immutable<S, L, D> scopeGraph, ITypeCheckerContext<S, L, D> queryContext,
                DataWf<S, L, D> dataWf, DataLeq<S, L, D> dataLeq) {
            this.sender = sender;
            this.queryContext = queryContext;
            this.scopeGraph = scopeGraph;
            this.dataWf = dataWf;
            this.dataLeq = dataLeq;
        }

        @Override public IFuture<Env<S, L, D>> externalEnv(LocalEnv<S, L, D> context, ScopePath<S, L> path,
                LabelWf<L> re, LabelOrder<L> labelOrder, ICancel cancel) {
            final S scope = path.getTarget();
            if(canAnswer(scope)) {
                logger.debug("local p_env {}", scope);
                return context.localEnv(path, re, cancel);
            }
            return getOwner(scope).thenCompose(owner -> {
                logger.debug("remote p_env from {}", owner);
                final ICompletableFuture<Env<S, L, D>> future = new CompletableFuture<>();
                final PQuery<S, L, D> query = PQuery.of(sender, path, dataWf, future);
                waitFor(query, owner);
                self.async(owner)._queryPrevious(path, re, dataWf, labelOrder, dataLeq).whenComplete((env, ex) -> {
                    logger.debug("got p_env from {}", sender);
                    granted(query, owner);
                    future.complete(env, ex);
                    resume();
                });

                return future;
            });
        }

        @Override public IFuture<Optional<D>> getDatum(S scope) {
            return CompletableFuture
                    .completedFuture(scopeGraph.getData(scope).map(AbstractUnit.this::getPreviousDatum));
        }

        @Override public IFuture<Iterable<S>> getEdges(S scope, L label) {
            return CompletableFuture.completedFuture(scopeGraph.getEdges(scope, label));
        }

        @Override public IFuture<Boolean> dataWf(D datum, ICancel cancel) throws InterruptedException {
            return dataWf.wf(datum, queryContext, cancel);
        }

        @Override public IFuture<Boolean> dataLeq(D d1, D d2, ICancel cancel) throws InterruptedException {
            return dataLeq.leq(d1, d2, queryContext, cancel);
        }

        @Override public IFuture<Boolean> dataLeqAlwaysTrue(ICancel cancel) {
            return dataLeq.alwaysTrue(queryContext, cancel);
        }
    }

    protected final class StaticQueryContext extends AbstractQueryTypeCheckerContext<S, L, D, R> {

        private final IActorRef<? extends IUnit<S, L, D, ?, ?>> sender;
        private final IScopeGraph.Immutable<S, L, D> scopeGraph;

        public StaticQueryContext(IActorRef<? extends IUnit<S, L, D, ?, ?>> sender,
                IScopeGraph.Immutable<S, L, D> scopeGraph) {
            this.sender = sender;
            this.scopeGraph = scopeGraph;
        }

        @Override public String id() {
            return self.id() + "#prev-query";
        }

        @Override public IFuture<? extends java.util.Set<IResolutionPath<S, L, D>>> query(S scope, LabelWf<L> labelWF,
                LabelOrder<L> labelOrder, DataWf<S, L, D> dataWF, DataLeq<S, L, D> dataEquiv,
                DataWf<S, L, D> dataWfInternal, DataLeq<S, L, D> dataEquivInternal) {
            final INameResolutionContext<S, L, D> pContext =
                    new StaticNameResolutionContext(sender, scopeGraph, this, dataWF, dataEquiv);
            // @formatter:off
            return new NameResolution<>(edgeLabels, labelOrder, pContext)
                .env(new ScopePath<S, L>(scope), labelWF, context.cancel())
                .thenApply(CapsuleUtil::toSet);
            // @formatter:on
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
            handleDeadlock(nodes);
        } else if(failDelays(nodes)) {
            resume();
        }
    }

    protected void handleDeadlock(java.util.Set<IProcess<S, L, D>> nodes) {
        logger.debug("{} deadlocked with {}", this, nodes);

        if(!nodes.contains(process)) {
            throw new IllegalStateException("Deadlock unrelated to this unit.");
        }

        AggregateFuture.forAll(nodes, node -> node.from(self, context)._state()).whenComplete((states, ex) -> {
            if(ex != null) {
                failures.add(ex);
                return;
            }

            final GraphBuilder<IProcess<S, L, D>> invWFGBuilder = GraphBuilder.of();
            states.forEach(state -> {
                final IProcess<S, L, D> self = state.getSelf();
                invWFGBuilder.addVertex(self);
                state.getDependencies().forEach(dep -> {
                    invWFGBuilder.addEdge(dep, self);
                });
            });

            final IGraph<IProcess<S, L, D>> invWFG = invWFGBuilder.build();

            if(!DeadlockUtils.connectedToAll(process, invWFG)) {
                logger.debug("{} not part of wfg SCC, computing knots in graph.");
                final java.util.Set<java.util.Set<IProcess<S, L, D>>> sccs = DeadlockUtils.sccs(invWFG);

                if(sccs.isEmpty()) {
                    return;
                }
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
            } else {
                if(isIncrementalDeadlockEnabled()) {
                    handleDeadlockIncremental(nodes, states);
                } else {
                    handleDeadlockRegular(nodes);
                }
            }
        });
    }

    private void handleDeadlockIncremental(java.util.Set<IProcess<S, L, D>> nodes,
            Collection<StateSummary<S, L, D>> states) {
        logger.debug("Received patches: {}.", states);
        if(states.stream().noneMatch(this::isRestartable)) {
            logger.debug("No restartable units, doing regular deadlock handling.");
            handleDeadlockRegular(nodes);
            return;
        }
        final Map<Boolean, java.util.Set<StateSummary<S, L, D>>> units =
                states.stream().collect(Collectors.partitioningBy(this::isActive, Collectors.toSet()));
        if(units.get(true).isEmpty()) {
            // No restarted units in cluster, release all involved units.
            logger.debug("Releasing all involved units.");
            nodes.forEach(node -> node.from(self, context)._release());
        } else {
            // @formatter:off
            final java.util.Set<StateSummary<S, L, D>> activeProcesses = units.get(true).stream()
                .collect(Collectors.toSet());
            // @formatter:on
            final Map<Boolean, java.util.Set<IProcess<S, L, D>>> restarts = units.get(false).stream()
                    .collect(Collectors.partitioningBy(node -> shouldRestart(node, activeProcesses),
                            Collectors.mapping(StateSummary::getSelf, Collectors.toSet())));
            if(restarts.get(true).isEmpty()) {
                logger.warn("False deadlock detected. Active units have no incoming dependencies elegible for restart.");
            } else {
                logger.debug("Restarting {} (conservative).", restarts);
                restarts.get(true).forEach(node -> node.from(self, context)._restart());
                logger.debug("Restarted {} (conservative).", restarts);
            }
        }
    }

    private boolean isRestartable(StateSummary<S, L, D> state) {
        return state.getState().equals(StateSummary.State.UNKNOWN);
    }

    private boolean isActive(StateSummary<S, L, D> state) {
        return state.getState().equals(StateSummary.State.ACTIVE);
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
            } else {
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
                    activate -> {},
                    unitAdd -> {}
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
                activate -> {
                    logger.error("Unit neither activated nor released.");
                    self.complete(activate.future(), null, new DeadlockException("Type checker deadlocked."));
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
        return context.settings().isIncremental();
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
        return waitForsByProcess.keySet();
    }

    @Override public void query(IProcess<S, L, D> k, IProcess<S, L, D> i, int m) {
        k.from(self, context)._deadlockQuery(i, m, process);
    }

    @Override public void reply(IProcess<S, L, D> k, IProcess<S, L, D> i, int m, java.util.Set<IProcess<S, L, D>> R) {
        k.from(self, context)._deadlockReply(i, m, R);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Scope graph diffing
    ///////////////////////////////////////////////////////////////////////////

    protected IDifferContext<S, L, D> differContext(Function1<D, D> instantiateData) {
        return new IDifferContext<S, L, D>() {

            @Override public IFuture<Iterable<S>> getEdges(S scope, L label) {
                return isComplete(scope, EdgeOrData.edge(label), self).thenApply(__ -> {
                    return scopeGraph.get().getEdges(scope, label);
                });
            }

            @Override public IFuture<Set.Immutable<L>> labels(S scope) {
                assertOwnOrSharedScope(scope);
                // TODO make more precise with labels for which scope was initialized.
                return CompletableFuture.completedFuture(edgeLabels);
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
                        resume(); // FIXME necessary?
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
                        resume();
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

        @Override public Iterable<String> csvHeaders() {
            // @formatter:off
            return Iterables.concat(ImmutableList.of(
                "runtimeMillis",
                "localQueries",
                "incomingQueries",
                "outgoingQueries",
                "forwardedQueries",
                "incomingConfirmations",
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
                Integer.toString(incomingConfirmations),
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
