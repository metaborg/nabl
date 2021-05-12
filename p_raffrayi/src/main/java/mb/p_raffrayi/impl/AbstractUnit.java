package mb.p_raffrayi.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.metaborg.util.Ref;
import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.HashTrieRelation3;
import org.metaborg.util.collection.IRelation3;
import org.metaborg.util.collection.MultiSet;
import org.metaborg.util.collection.MultiSetMap;
import org.metaborg.util.functions.Function2;
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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import io.usethesource.capsule.Set;
import mb.p_raffrayi.DeadlockException;
import mb.p_raffrayi.IRecordedQuery;
import mb.p_raffrayi.IScopeGraphLibrary;
import mb.p_raffrayi.ITypeChecker;
import mb.p_raffrayi.ITypeCheckerContext;
import mb.p_raffrayi.IUnitResult;
import mb.p_raffrayi.IUnitResult.Transitions;
import mb.p_raffrayi.IUnitStats;
import mb.p_raffrayi.TypeCheckingFailedException;
import mb.p_raffrayi.actors.IActor;
import mb.p_raffrayi.actors.IActorMonitor;
import mb.p_raffrayi.actors.IActorRef;
import mb.p_raffrayi.actors.IActorStats;
import mb.p_raffrayi.actors.TypeTag;
import mb.p_raffrayi.actors.deadlock.ChandyMisraHaas;
import mb.p_raffrayi.actors.deadlock.ChandyMisraHaas.Host;
import mb.p_raffrayi.impl.confirmation.DenyingConfirmation;
import mb.p_raffrayi.impl.confirmation.IQueryConfirmation;
import mb.p_raffrayi.impl.diff.AddingDiffer;
import mb.p_raffrayi.impl.diff.IScopeGraphDiffer;
import mb.p_raffrayi.impl.diff.IScopeGraphDifferContext;
import mb.p_raffrayi.impl.diff.IScopeGraphDifferOps;
import mb.p_raffrayi.impl.diff.ScopeGraphDiffer;
import mb.p_raffrayi.impl.tokens.CloseLabel;
import mb.p_raffrayi.impl.tokens.CloseScope;
import mb.p_raffrayi.impl.tokens.Complete;
import mb.p_raffrayi.impl.tokens.Datum;
import mb.p_raffrayi.impl.tokens.DifferResult;
import mb.p_raffrayi.impl.tokens.IWaitFor;
import mb.p_raffrayi.impl.tokens.InitScope;
import mb.p_raffrayi.impl.tokens.Match;
import mb.p_raffrayi.impl.tokens.Query;
import mb.p_raffrayi.impl.tokens.TypeCheckerResult;
import mb.p_raffrayi.impl.tokens.TypeCheckerState;
import mb.p_raffrayi.nameresolution.DataLeq;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelOrder;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.ecoop21.NameResolution;
import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.diff.ScopeGraphDiff;
import mb.scopegraph.oopsla20.path.IResolutionPath;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.scopegraph.oopsla20.reference.Env;
import mb.scopegraph.oopsla20.reference.ScopeGraph;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

public abstract class AbstractUnit<S, L, D, R>
        implements IUnit<S, L, D, R>, IActorMonitor, Host<IActorRef<? extends IUnit<S, L, D, ?>>> {

    private static final ILogger logger = LoggerUtils.logger(IUnit.class);

    protected final TypeTag<IUnit<S, L, D, ?>> TYPE = TypeTag.of(IUnit.class);

    protected final IActor<? extends IUnit<S, L, D, R>> self;
    protected final @Nullable IActorRef<? extends IUnit<S, L, D, ?>> parent;
    protected final IUnitContext<S, L, D> context;

    private final ChandyMisraHaas<IActorRef<? extends IUnit<S, L, D, ?>>> cmh;

    private volatile boolean innerResult;
    protected final Ref<R> analysis;
    protected final List<Throwable> failures;
    private final Map<String, IUnitResult<S, L, D, ?>> subUnitResults;
    private final ICompletableFuture<IUnitResult<S, L, D, R>> unitResult;

    protected final Ref<IScopeGraph.Immutable<S, L, D>> scopeGraph;
    protected final Set.Immutable<L> edgeLabels;
    protected final Set.Transient<S> scopes;
    private final List<S> rootScopes = new ArrayList<>();

    private final IRelation3.Transient<S, EdgeOrData<L>, Delay> delays;

    // TODO unwrap old scope graph(?)
    protected final IInitialState<S, L, D, R> initialState; // TODO: move to typecheckerunit
    protected final IQueryConfirmation<S, L, D> confirmation = new DenyingConfirmation<>();
    protected final IScopeGraphDiffer<S, L, D> differ;
    private final Ref<ScopeGraphDiff<S, L, D>> diffResult = new Ref<>();

    private final MultiSet.Transient<String> scopeNameCounters;

    protected final java.util.Set<IRecordedQuery<S, L, D>> recordedQueries = new HashSet<>();

    protected Transitions transitions = Transitions.OTHER;
    protected final Stats stats;

    public AbstractUnit(IActor<? extends IUnit<S, L, D, R>> self,
            @Nullable IActorRef<? extends IUnit<S, L, D, ?>> parent, IUnitContext<S, L, D> context,
            Iterable<L> edgeLabels, IInitialState<S, L, D, R> initialState, IScopeGraphDifferOps<S, D> differOps) {
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

        this.initialState = initialState;
        final IScopeGraph.Immutable<S, L, D> previousScopeGraph =
                initialState.previousResult().map(IUnitResult::scopeGraph).orElse(ScopeGraph.Immutable.of());
        this.differ = initialState.previousResult().isPresent()
                ? new ScopeGraphDiffer<>(new DifferContext(previousScopeGraph, differOps))
                : new AddingDiffer<>(new DifferContext(previousScopeGraph, differOps));

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

    @Override public IFuture<Env<S, L, D>> _query(ScopePath<S, L> path, LabelWf<L> labelWF, DataWf<S, L, D> dataWF,
            LabelOrder<L> labelOrder, DataLeq<S, L, D> dataEquiv) {
        // resume(); // FIXME necessary?
        stats.incomingQueries += 1;
        return doQuery(self.sender(TYPE), path, labelWF, labelOrder, dataWF, dataEquiv, null, null);
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
        this.rootScopes.addAll(rootScopes);
        for(S rootScope : CapsuleUtil.toSet(rootScopes)) {
            scopes.__insert(rootScope);
            doAddLocalShare(self, rootScope);
        }
        startDiffer(rootScopes);
    }

    protected final IFuture<IUnitResult<S, L, D, R>> doFinish(IFuture<R> result) {
        final ICompletableFuture<R> internalResult = new CompletableFuture<>();
        final TypeCheckerResult<S, L, D> token = TypeCheckerResult.of(self, internalResult);
        waitFor(token, self);
        result.whenComplete(internalResult::complete); // FIXME self.schedule(result)?
        internalResult.whenComplete((r, ex) -> {
            logger.debug("{} type checker finished", this);
            resume(); // FIXME necessary?
            differ.typeCheckerFinished();
            if(ex != null) {
                logger.error("type checker errored: {}", ex);
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

    private void startDiffer(List<S> rootScopes) {
        final ICompletableFuture<ScopeGraphDiff<S, L, D>> differResult = new CompletableFuture<>();

        // Handle diff output
        final DifferResult<S, L, D> result = DifferResult.of(self, differResult);
        waitFor(result, self);
        self.schedule(differ.diff(rootScopes,
                initialState.previousResult().map(IUnitResult::rootScopes).orElse(Collections.emptyList())))
                .whenComplete(differResult::complete);
        differResult.whenComplete((r, ex) -> {
            logger.debug("{} scope graph differ finished", this);
            resume(); // FIXME necessary
            if(ex != null) {
                logger.error("scope graph differ errored.", ex);
                failures.add(ex);
            } else {
                diffResult.set(r);
            }
            granted(result, self);
        });
    }

    ///////////////////////////////////////////////////////////////////////////
    // ITypeCheckerContext interface, called by ITypeChecker implementations
    ///////////////////////////////////////////////////////////////////////////

    // NB. Invoke methods via `local` so that we have the same scheduling & ordering
    // guarantees as for remote calls.

    protected <Q> Tuple2<IActorRef<? extends IUnit<S, L, D, Q>>, IFuture<IUnitResult<S, L, D, Q>>> doAddSubUnit(
            String id, Function2<IActor<IUnit<S, L, D, Q>>, IUnitContext<S, L, D>, IUnit<S, L, D, Q>> unitProvider,
            List<S> rootScopes) {
        for(S rootScope : rootScopes) {
            assertOwnOrSharedScope(rootScope);
        }

        final Tuple2<IFuture<IUnitResult<S, L, D, Q>>, IActorRef<? extends IUnit<S, L, D, Q>>> result_subunit =
                context.add(id, unitProvider, rootScopes);
        final IActorRef<? extends IUnit<S, L, D, Q>> subunit = result_subunit._2();

        final ICompletableFuture<IUnitResult<S, L, D, Q>> internalResult = new CompletableFuture<>();
        final TypeCheckerResult<S, L, D> token = TypeCheckerResult.of(self, internalResult);
        waitFor(token, subunit);
        result_subunit._1().whenComplete(internalResult::complete); // must come after waitFor

        for(S rootScope : CapsuleUtil.toSet(rootScopes)) {
            doAddShare(subunit, rootScope);
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

    protected final S doFreshScope(String baseName, Iterable<L> edgeLabels, boolean data, boolean sharing) {
        final S scope = makeScope(baseName);

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

    @Override public IFuture<org.metaborg.util.unit.Unit> _isComplete(S scope, EdgeOrData<L> label) {
        assertOwnScope(scope);
        return isComplete(scope, label, self.sender(TYPE));
    }

    @Override public IFuture<Optional<D>> _datum(S scope) {
        assertOwnScope(scope);
        return isComplete(scope, EdgeOrData.data(), self.sender(TYPE)).thenApply(__ -> scopeGraph.get().getData(scope));
    }

    @Override public IFuture<Optional<S>> _match(S previousScope) {
        assertOwnScope(previousScope);
        return differ.match(previousScope);
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
        for(EdgeOrData<L> edge : edges) {
            waitFor(CloseLabel.of(self, scope, edge), sender);
        }
        if(sharing) {
            waitFor(CloseScope.of(self, scope), sender);
        }

        if(isOwner(scope)) {
            if(isScopeInitialized(scope)) {
                releaseDelays(scope);
            }
        } else {
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

        if(isOwner(scope)) {
            if(isScopeInitialized(scope)) {
                releaseDelays(scope);
            }
        } else {
            self.async(parent)._doneSharing(scope);
        }
    }

    protected final void doCloseLabel(IActorRef<? extends IUnit<S, L, D, ?>> sender, S scope, EdgeOrData<L> edge) {
        assertOwnOrSharedScope(scope);

        granted(CloseLabel.of(self, scope, edge), sender);

        if(isOwner(scope)) {
            if(isEdgeClosed(scope, edge)) {
                releaseDelays(scope, edge);
            }
        } else {
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

    protected final IFuture<Env<S, L, D>> doQuery(IActorRef<? extends IUnit<S, L, D, ?>> sender, ScopePath<S, L> path,
            LabelWf<L> labelWF, LabelOrder<L> labelOrder, DataWf<S, L, D> dataWF, DataLeq<S, L, D> dataEquiv,
            DataWf<S, L, D> dataWfInternal, DataLeq<S, L, D> dataEquivInternal) {
        logger.debug("got _query from {}", sender);
        final boolean external = !sender.equals(self);

        final NameResolution<S, L, D> nr = new NameResolution<S, L, D>(edgeLabels, labelOrder) {

            @Override public Optional<IFuture<Env<S, L, D>>> externalEnv(ScopePath<S, L> path, LabelWf<L> re,
                    LabelOrder<L> labelOrder) {
                final S scope = path.getTarget();
                if(canAnswer(scope)) {
                    logger.debug("local env {}", scope);
                    return Optional.empty();
                } else {
                    return Optional.of(getOwner(scope).thenCompose(owner -> {
                        logger.debug("remote env {} at {}", scope, owner);
                        // this code mirrors query(...)
                        final IFuture<Env<S, L, D>> result =
                                self.async(owner)._query(path, re, dataWF, labelOrder, dataEquiv);
                        final Query<S, L, D> wf = Query.of(sender, path, re, dataWF, labelOrder, dataEquiv, result);
                        waitFor(wf, owner);
                        if(external) {
                            stats.forwardedQueries += 1;
                        } else {
                            stats.outgoingQueries += 1;
                        }
                        return result.whenComplete((r, ex) -> {
                            logger.debug("got answer from {}", sender);
                            if(!external && ex == null) {
                                recordedQueries.add(RecordedQuery.of(scope, labelWF, dataWF, labelOrder, dataEquiv, r));
                            }
                            resume();
                            granted(wf, owner);
                        });
                    }));
                }
            }

            @Override protected IFuture<Optional<D>> getDatum(S scope) {
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

            @Override protected IFuture<Iterable<S>> getEdges(S scope, L label) {
                return isComplete(scope, EdgeOrData.edge(label), sender).thenApply(__ -> {
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

            @Override public IFuture<Boolean> dataLeqAlwaysTrue(ICancel cancel) {
                return dataEquiv.alwaysTrue(queryContext, cancel);
            }

        };

        final IFuture<Env<S, L, D>> result = nr.env(path, labelWF, context.cancel());
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
                ITypeChecker<S, L, D, Q> unitChecker, List<S> rootScopes, IInitialState<S, L, D, Q> initialState) {
            throw new UnsupportedOperationException("Unsupported in query context.");
        }

        @SuppressWarnings("unused") @Override public IFuture<IUnitResult<S, L, D, Unit>> add(String id,
                IScopeGraphLibrary<S, L, D> library, List<S> rootScopes) {
            throw new UnsupportedOperationException("Unsupported in query context.");
        }

        @SuppressWarnings("unused") @Override public void initScope(S root, Iterable<L> labels, boolean sharing) {
            throw new UnsupportedOperationException("Unsupported in query context.");
        }

        @SuppressWarnings("unused") @Override public S freshScope(String baseName, Iterable<L> edgeLabels, boolean data,
                boolean sharing) {
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
            final IFuture<Env<S, L, D>> result =
                    doQuery(self, path, labelWF, labelOrder, dataWF, dataEquiv, dataWfInternal, dataEquivInternal);
            final Query<S, L, D> wf = Query.of(self, path, labelWF, dataWF, labelOrder, dataEquiv, result);
            waitFor(wf, self);
            stats.localQueries += 1;
            return self.schedule(result).whenComplete((env, ex) -> {
                granted(wf, self);
            }).thenApply(CapsuleUtil::toSet);
        }

    };

    protected final boolean isOwner(S scope) {
        return context.scopeId(scope).equals(self.id());
    }

    protected IFuture<IActorRef<? extends IUnit<S, L, D, ?>>> getOwner(S scope) {
        return self.schedule(context.owner(scope)).whenComplete((r, ex) -> self.assertOnActorThread());
    }

    protected boolean canAnswer(S scope) {
        return isOwner(scope);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Wait fors & finalization
    ///////////////////////////////////////////////////////////////////////////

    protected MultiSet.Immutable<IWaitFor<S, L, D>> waitFors = MultiSet.Immutable.of();
    protected MultiSetMap.Immutable<IActorRef<? extends IUnit<S, L, D, ?>>, IWaitFor<S, L, D>> waitForsByActor =
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
        logger.debug("{} granted {} by {}", self, token, actor);
        waitFors = waitFors.remove(token);
        waitForsByActor = waitForsByActor.remove(actor, token);
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
            unitResult.complete(UnitResult.of(self.id(), scopeGraph.get(), localScopeGraph(), recordedQueries,
                    rootScopes, analysis.get(), failures, subUnitResults, stats).withFlow(transitions));
        } else {
            logger.trace("Still waiting for {}{}", innerResult ? "inner result and " : "", waitForsByActor);
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
            IActorRef<? extends IUnit<S, L, D, ?>> sender) {
        assertOwnScope(scope);
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
        if(!failDelays(nodes)) {
            logger.debug("No delays to fail. Still waiting for {}.", waitForsByActor);
            resume();
        }
    }

    protected void handleDeadlock(java.util.Set<IActorRef<? extends IUnit<S, L, D, ?>>> nodes) {
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
                    complete -> {},
                    datum -> {},
                    match -> {},
                    result  -> {},
                    typeCheckerState -> {
                        if(nodes.contains(typeCheckerState.origin())) {
                            logger.debug("{} fail {}", self, typeCheckerState);
                            deadlocked.__insert(typeCheckerState.future());
                        }
                    },
                    differResult -> {},
                    activate -> {}
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
                complete -> {
                    logger.error("Unexpected remaining completeness query: " + complete);
                    throw new IllegalStateException("Unexpected remaining completeness query: " + complete);
                },
                datum -> {
                    logger.error("Unexpected remaining datum query: " + datum);
                    throw new IllegalStateException("Unexpected remaining datum query: " + datum);
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
                activate -> {
                    logger.error("Unit neither activated nor released.");
                    self.complete(activate.future(), null, new DeadlockException("Type checker deadlocked."));
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
    // Scope graph diffing
    ///////////////////////////////////////////////////////////////////////////

    private class DifferContext implements IScopeGraphDifferContext<S, L, D> {

        private final IScopeGraph.Immutable<S, L, D> previousScopeGraph;
        private final IScopeGraphDifferOps<S, D> differOps;

        public DifferContext(IScopeGraph.Immutable<S, L, D> previousScopeGraph, IScopeGraphDifferOps<S, D> differOps) {
            this.previousScopeGraph = previousScopeGraph;
            this.differOps = differOps;
        }

        @Override public IFuture<Iterable<S>> getCurrentEdges(S scope, L label) {
            // Most be done via owner, because delays on a shared, non-owned scope are not activated
            // and not desired to be activated, because not any operation on them can proceed
            return getOwner(scope).thenCompose(owner -> {
                final Complete<S, L, D> complete = Complete.of(self, scope, EdgeOrData.edge(label));

                waitFor(complete, owner);
                return self.async(owner)._isComplete(scope, EdgeOrData.edge(label)).thenApply(__ -> {
                    granted(complete, owner);
                    resume();
                    return scopeGraph.get().getEdges(scope, label);
                });
            });
        }

        @Override public IFuture<Iterable<S>> getPreviousEdges(S scope, L label) {
            return CompletableFuture.completedFuture(previousScopeGraph.getEdges(scope, label));
        }

        @Override public IFuture<Iterable<L>> labels(S currentScope) {
            assertOwnOrSharedScope(currentScope);
            // TODO make more precise with labels for which scope was initialized.
            return CompletableFuture
                    .completedFuture(Set.Immutable.union(scopeGraph.get().getLabels(), previousScopeGraph.getLabels()));
        }

        @Override public IFuture<Optional<D>> currentDatum(S scope) {
            return getOwner(scope).thenCompose(owner -> {
                final Datum<S, L, D> datum = Datum.of(self, scope);
                waitFor(datum, owner);
                return self.async(owner)._datum(scope).whenComplete((__, ___) -> {
                    granted(datum, owner);
                    resume();
                });
            });
        }

        @Override public IFuture<Optional<D>> previousDatum(S scope) {
            return CompletableFuture.completedFuture(previousScopeGraph.getData(scope));
        }

        @Override public IFuture<Boolean> matchDatums(D currentDatum, D previousDatum,
                Function2<S, S, IFuture<Boolean>> scopeMatch) {
            return differOps.matchDatums(currentDatum, previousDatum, scopeMatch);
        }

        @Override public boolean isMatchAllowed(S currentScope, S previousScope) {
            return context.scopeId(previousScope).equals(context.scopeId(currentScope));
        }

        @Override public Set.Immutable<S> getCurrentScopes(D d) {
            return differOps.getScopes(d);
        }

        @Override public Set.Immutable<S> getPreviousScopes(D d) {
            return differOps.getScopes(d);
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
}