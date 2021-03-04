package mb.statix.concurrent.p_raffrayi.impl;

import static com.google.common.collect.Streams.stream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.metaborg.util.Ref;
import org.metaborg.util.functions.Function2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import io.usethesource.capsule.Set.Immutable;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
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
import mb.statix.concurrent.actors.futures.AggregateFuture;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.ICompletable;
import mb.statix.concurrent.actors.futures.ICompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.DeadlockException;
import mb.statix.concurrent.p_raffrayi.IRecordedQuery;
import mb.statix.concurrent.p_raffrayi.IScopeImpl;
import mb.statix.concurrent.p_raffrayi.ITypeChecker;
import mb.statix.concurrent.p_raffrayi.ITypeCheckerContext;
import mb.statix.concurrent.p_raffrayi.IUnitResult;
import mb.statix.concurrent.p_raffrayi.IUnitStats;
import mb.statix.concurrent.p_raffrayi.TypeCheckingFailedException;
import mb.statix.concurrent.p_raffrayi.confirmation.DenyingConfirmation;
import mb.statix.concurrent.p_raffrayi.confirmation.IQueryConfirmation;
import mb.statix.concurrent.p_raffrayi.diff.AddingDiffer;
import mb.statix.concurrent.p_raffrayi.diff.IScopeGraphDiffer;
import mb.statix.concurrent.p_raffrayi.diff.IScopeGraphDifferContext;
import mb.statix.concurrent.p_raffrayi.diff.IScopeGraphDifferOps;
import mb.statix.concurrent.p_raffrayi.diff.ScopeGraphDiffer;
import mb.statix.concurrent.p_raffrayi.impl.tokens.ADifferResult;
import mb.statix.concurrent.p_raffrayi.impl.tokens.CloseLabel;
import mb.statix.concurrent.p_raffrayi.impl.tokens.CloseScope;
import mb.statix.concurrent.p_raffrayi.impl.tokens.Complete;
import mb.statix.concurrent.p_raffrayi.impl.tokens.Datum;
import mb.statix.concurrent.p_raffrayi.impl.tokens.DifferResult;
import mb.statix.concurrent.p_raffrayi.impl.tokens.IWaitFor;
import mb.statix.concurrent.p_raffrayi.impl.tokens.InitScope;
import mb.statix.concurrent.p_raffrayi.impl.tokens.Match;
import mb.statix.concurrent.p_raffrayi.impl.tokens.Query;
import mb.statix.concurrent.p_raffrayi.impl.tokens.TypeCheckerResult;
import mb.statix.concurrent.p_raffrayi.impl.tokens.TypeCheckerState;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataLeq;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataWf;
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelOrder;
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelWf;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.diff.BiMap;
import mb.statix.scopegraph.diff.ScopeGraphDiff;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.reference.EdgeOrData;
import mb.statix.scopegraph.reference.Env;
import mb.statix.scopegraph.reference.ScopeGraph;
import mb.statix.scopegraph.terms.newPath.ScopePath;

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
    private final List<S> rootScopes = new ArrayList<>();
    private final IRelation3.Transient<S, EdgeOrData<L>, Delay> delays;
    private final IScopeImpl<S, D> scopeImpl;

    // TODO unwrap old scope graph(?)
    private final IInitialState<S, L, D, R> initialState;
    private final IQueryConfirmation<S, L, D> confirmation = new DenyingConfirmation<>();
    private final IScopeGraphDiffer<S, L, D> differ;
    private final Ref<ScopeGraphDiff<S, L, D>> diffResult = new Ref<>();

    private final MultiSet.Transient<String> scopeNameCounters;

    private final java.util.Set<IRecordedQuery<S, L, D>> recordedQueries = new HashSet<>();

    private final Stats stats;

    Unit(IActor<? extends IUnit<S, L, D, R>> self, @Nullable IActorRef<? extends IUnit<S, L, D, ?>> parent,
            IUnitContext<S, L, D> context, ITypeChecker<S, L, D, R> unitChecker, Iterable<L> edgeLabels,
            IScopeImpl<S, D> scopeImpl, IInitialState<S, L, D, R> initialState, IScopeGraphDifferOps<S, D> differOps) {
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
        this.scopeImpl = scopeImpl;

        this.initialState = initialState;
        final IScopeGraph.Immutable<S, L, D> previousScopeGraph = initialState.previousResult()
            .map(IUnitResult::scopeGraph)
            .orElse(ScopeGraph.Immutable.of());
        this.differ = initialState.previousResult().isPresent() ?
            new ScopeGraphDiffer<>(new DifferContext(previousScopeGraph, differOps)) :
                new AddingDiffer<>(new DifferContext(previousScopeGraph, differOps));

        this.scopeNameCounters = MultiSet.Transient.of();

        this.stats = new Stats(self.stats());
    }

    ///////////////////////////////////////////////////////////////////////////
    // IBroker2UnitProtocol interface, called by IBroker implementations
    ///////////////////////////////////////////////////////////////////////////

    @Override public IFuture<IUnitResult<S, L, D, R>> _start(List<S> rootScopes) {
        assertInState(UnitState.INIT);
        resume();

        this.rootScopes.addAll(rootScopes);
        for(S rootScope : CapsuleUtil.toSet(rootScopes)) {
            scopes.__insert(rootScope);
            doAddLocalShare(self, rootScope);
        }

        // Handle diff output
        final ADifferResult<S, L, D> differResult = DifferResult.of(self);
        waitFor(differResult, self);
        self.schedule(differ.diff(rootScopes, initialState.previousResult()
            .map(IUnitResult::rootScopes)
            .orElse(Collections.emptyList())))
            .whenComplete((r, ex) -> {
                logger.debug("{} scope graph differ finished", this);
                resume(); // FIXME necessary
                if(ex != null) {
                    logger.error("type checker errored: {}", ex);
                    failures.add(ex);
                } else {
                    diffResult.set(r);
                }
                granted(differResult, self);
            });

        startTypeChecker(rootScopes);
        return unitResult;
    }

    private void startTypeChecker(List<S> rootScopes) {
        state = UnitState.ACTIVE;
        // run() after inits are initialized before run, since unitChecker
        // can immediately call methods, that are executed synchronously

        final ICompletableFuture<R> typeCheckerResult = new CompletableFuture<>();
        final TypeCheckerResult<S, L, D> result = TypeCheckerResult.of(self, typeCheckerResult);
        waitFor(result, self);
        self.schedule(this.typeChecker.run(this, rootScopes, initialState)).whenComplete(typeCheckerResult::complete);
        typeCheckerResult.whenComplete((r, ex) -> {
            logger.debug("{} type checker finished", this);
            resume(); // FIXME necessary?
            differ.typeCheckerFinished();
            if(ex != null) {
                logger.error("type checker errored: {}", ex);
                failures.add(ex);
            } else {
                analysis.set(r);
            }
            granted(result, self);
            final MultiSet.Immutable<IWaitFor<S, L, D>> selfTokens = getTokens(self);
            if(!selfTokens.isEmpty()) {
                logger.debug("{} returned while waiting on {}", self, selfTokens);
            }
        });
    }

    private void confirmQueries(List<S> rootScopes) {
        assertInState(UnitState.INIT, /* or */ UnitState.UNKNOWN);
        resume();

        state = UnitState.UNKNOWN;
        ICompletableFuture<Tuple2<Boolean, Map.Immutable<S, S>>> confirmationsComplete = new CompletableFuture<>();
        // TODO deadlock handling
        confirmationsComplete.whenComplete((v, ex) -> {
            if (ex != null) {
                logger.error("{} confirmation failed: {}", this, ex);
                failures.add(ex);
                tryFinish();
            } else if(!v._1()) {
                logger.info("{} confirmations denied, restarting", this);
                assertInState(UnitState.UNKNOWN);
                startTypeChecker(rootScopes);
            } else {
                logger.info("{} confirmations confirmed", this);                
                release(v._2());
            }
        });

        // @formatter:off
        new AggregateFuture<Tuple2<Boolean, Map.Immutable<S, S>>>(initialState.previousResult()
            .orElseThrow(() -> new IllegalStateException("Cannot confirm queries when no previous result is provided"))
            .queries()
            .stream()
            .map(recordedQuery -> confirmation.confirm(recordedQuery)
                .whenComplete((v, ex) -> {
                    // When confirmation denied, eagerly restart type-checker
                    if(ex == null && (v == null || !v._1())) {
                        confirmationsComplete.complete(v, ex);
                    }
                }))
            .collect(Collectors.toSet()))
            .whenComplete((v, ex) -> {
                if(ex != null) {
                    confirmationsComplete.complete(Tuple2.of(false, CapsuleUtil.immutableMap()), ex);
                } else if(v.stream().allMatch(x -> x != null && x._1())) {
                    // All queries confirmed, aggregate patches and complete
                    Map.Transient<S, S> patches = CapsuleUtil.transientMap();
                    // TODO: optimize for duplicates?
                    v.forEach(result -> patches.__putAll(result._2()));
                    confirmationsComplete.complete(Tuple2.of(true, patches.freeze()), ex);
                }
                // in the else case, one of the futures has restarted the type checker, so we don't handle that case here.
            });
        // @formatter:on
    }

    private void release(Map.Immutable<S, S> patches) {
        IUnitResult<S, L, D, R> previousResult = initialState.previousResult().get();
        
        IScopeGraph.Transient<S, L, D> newScopeGraph = ScopeGraph.Transient.of();
        previousResult.scopeGraph().getEdges().forEach((entry, targets) -> {
            S oldSource = entry.getKey();
            S newSource = patches.getOrDefault(oldSource, oldSource);
            targets.forEach(targetScope -> {
                newScopeGraph.addEdge(newSource, entry.getValue(), patches.getOrDefault(targetScope, targetScope));
            });
        });
        previousResult.scopeGraph().getData().forEach((oldScope, datum) -> {
            S newScope = patches.getOrDefault(oldScope, oldScope);
            newScopeGraph.setDatum(newScope, scopeImpl.subtituteScopes(datum, patches));
        });
        
        scopeGraph.set(newScopeGraph.freeze());
        analysis.set(initialState.previousResult().get().analysis());
        tryFinish();
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
            List<S> rootScopes, IInitialState<S, L, D, Q> initialState) {
        assertInState(UnitState.ACTIVE);
        for(S rootScope : rootScopes) {
            assertOwnOrSharedScope(rootScope);
        }

        initialState.previousResult().map(IUnitResult::rootScopes).ifPresent(previousRootScopes -> {
            // When a scope is shared, the shares must be consistent.
            // Also, it is not necessary that shared scopes are reachable from the root scopes
            // (A unit started by the Broker does not even have root scopes)
            // Therefore we enforce here that the current root scopes and the previous ones match.

            if(rootScopes.size() != previousRootScopes.size()) {
                logger.error("Unit {} adds subunit {} with initial state but with different root scope count.");
                throw new IllegalStateException("Different root scope count.");
            }

            BiMap.Transient<S> req = BiMap.Transient.of();
            for(int i = 0; i < rootScopes.size(); i++) {
                req.put(rootScopes.get(i), previousRootScopes.get(i));
            }

            if(!differ.matchScopes(req.freeze())) {
                logger.error("Unit {} adds subunit {} with initial state but with different root scope count.");
                throw new IllegalStateException("Could not match.");
            }

        });

        final Tuple2<IFuture<IUnitResult<S, L, D, Q>>, IActorRef<? extends IUnit<S, L, D, Q>>> result_subunit =
                context.add(id, unitChecker, rootScopes, initialState);
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

    @Override public IFuture<Boolean> confirmQueries() {
        if(initialState.changed()) {
            logger.debug("Unit changed or no previous result wa");
            return CompletableFuture.completedFuture(false);
        }

        // Invariant: added units are marked as changed.
        // Therefore, if unit is not changed, a previous result must be given.
        IUnitResult<S, L, D, R> previousResult = initialState.previousResult().get();

        final ICompletableFuture<Boolean> confirmationResult = new CompletableFuture<>();
        // @formatter:off
        final Set<IFuture<Tuple2<Boolean, Map.Immutable<S, S>>>> confirmations = previousResult
            .queries()
            .stream()
            .map(q -> {
                return confirmation.confirm(q).whenComplete((res, ex) -> {
                    // If one of the confirmations fails, eagerly fail result
                    if(!res._1()) {
                        confirmationResult.complete(false);
                    }
                });
            })
            .collect(CapsuleCollectors.toSet());
        // @formatter:on
        new AggregateFuture<>(confirmations).whenComplete((r, ex) -> {
            if(r.stream().allMatch(Tuple2::_1)) {
                // Aggregate patches: remove duplicates
                final Map.Transient<S, S> patches = CapsuleUtil.transientMap();
                r.stream().map(Tuple2::_2).forEach(patches::__putAll);

                // Index scope graph data by source scope
                IScopeGraph.Immutable<S, L, D> previousScopeGraph = previousResult.scopeGraph();

                final HashMap<S, D> dataEntries = Maps.newHashMap();
                final HashMap<S, ListMultimap<L, S>> edgeEntries = Maps.newHashMap();

                previousScopeGraph.getData().forEach((s, d) -> {
                    dataEntries.put(s, d);
                });

                previousScopeGraph.getEdges().forEach((src_lbl, tgt) -> {
                    ListMultimap<L, S> edges = edgeEntries.getOrDefault(src_lbl.getKey(), LinkedListMultimap.create());
                    edges.putAll(src_lbl.getValue(), tgt);
                    edgeEntries.put(src_lbl.getKey(), edges);
                });

                // Insert patched scope graph
                for(S oldScope : Sets.union(edgeEntries.keySet(), dataEntries.keySet())) {
                    Optional<D> datum = previousScopeGraph.getData(oldScope);
                    ListMultimap<L, S> edges = edgeEntries.get(oldScope);

                    S scope;
                    if(context.owner(oldScope) != self) {
                        // When scope is not our own, it should be shared with us. This implies that
                        // it is remote, and hence the differ supplied a patch for its previous representation.
                        // Therefore looking up the patch must result in a scope shared with us.
                        scope = patches.getOrDefault(oldScope, oldScope);
                        assertOwnOrSharedScope(scope);
                        if(datum.isPresent()) {
                            throw new IllegalStateException("Cannot set datum for shared scope");
                        }

                        // TODO: if we are not owner, send parent message that we added this edge
                        // Owner should verify correctness: we may only add edge that was in its previous
                        // result.
                        // This should trivially hold for this case (queries confirmed) but not
                        // trivially when the unit is restarted.
                        // Probably this should just happen when the owner receives _addEdge

                        initScope(scope, edges.keySet(), false);
                    } else {
                        scope = oldScope;
                        // Avoid generation of new identity when freshScope would be used.
                        scopes.__insert(scope);
                        doAddLocalShare(self, scope);

                        // Collect labels for scope
                        Set.Transient<EdgeOrData<L>> labels = CapsuleUtil.transientSet();
                        edges.keySet().forEach(l -> labels.__insert(EdgeOrData.edge(l)));
                        datum.ifPresent(d -> labels.__insert(EdgeOrData.data()));

                        // TODO: If scope was shared in original result, share here as well
                        // TODO: When scope is shared, handle following situations:
                        // - Subunit changed: wait for any declaration before closing scope
                        // - Subunit cached: assert that it precisely declares the same stuff (modulo patching)
                        // Strategy: track edge sources, that initialize expectations (multiset)
                        // if _addEdge not in expectations: error
                        // if scope closed, but remaining expectations: error
                        doInitShare(self, scope, labels, false);

                        datum.ifPresent(d -> setDatum(scope, scopeImpl.subtituteScopes(d, patches)));
                    }

                    // No sharing, hence no closeScope needed
                    edges.forEach((lbl, tgt) -> {
                        addEdge(scope, lbl, patches.getOrDefault(tgt, tgt));
                    });

                    edges.keySet().forEach(lbl -> closeEdge(scope, lbl));
                }

                // TODO assert no waits for self

                // All queries confirmed, transition to released state
                confirmationResult.complete(true);
            } else if(!confirmationResult.isDone()) {
                logger.warn("Not all queries confirmed, but confirmation result was not falsified either.");
                confirmationResult.complete(false);
            }
        });

        return confirmationResult;
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
            DataWf<S, L, D> dataWF, DataLeq<S, L, D> dataEquiv, @Nullable DataWf<S, L, D> dataWfInternal,
            @Nullable DataLeq<S, L, D> dataEquivInternal) {
        assertInState(UnitState.ACTIVE);
        recordedQueries
            .add(RecordedQuery.of(scope, labelWF, dataWF, labelOrder, dataEquiv));

        final ScopePath<S, L> path = new ScopePath<>(scope);
        final IFuture<Env<S, L, D>> result =
                doQuery(self, path, labelWF, labelOrder, dataWF, dataEquiv, dataWfInternal, dataEquivInternal);
        final Query<S, L, D> wf = Query.of(self, path, labelWF, dataWF, labelOrder, dataEquiv, result);
        waitFor(wf, self);
        stats.ownQueries += 1;
        return self.schedule(result).whenComplete((env, ex) -> {
            granted(wf, self);
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

    @Override public final IFuture<Env<S, L, D>> _query(ScopePath<S, L> path, LabelWf<L> labelWF,
            DataWf<S, L, D> dataWF, LabelOrder<L> labelOrder, DataLeq<S, L, D> dataEquiv) {
        // resume(); // FIXME necessary?
        stats.foreignQueries += 1;
        return doQuery(self.sender(TYPE), path, labelWF, labelOrder, dataWF, dataEquiv, null, null);
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

        final IActorRef<? extends IUnit<S, L, D, ?>> owner = context.owner(scope);
        if(owner.equals(self)) {
            if(isEdgeClosed(scope, edge)) {
                releaseDelays(scope, edge);
            }
        } else {
            self.async(parent)._closeEdge(scope, edge);
        }
    }

    private final void doAddEdge(IActorRef<? extends IUnit<S, L, D, ?>> sender, S source, L label, S target) {
        assertOwnOrSharedScope(source);
        assertLabelOpen(source, EdgeOrData.edge(label));

        scopeGraph.set(scopeGraph.get().addEdge(source, label, target));

        final IActorRef<? extends IUnit<S, L, D, ?>> owner = context.owner(source);
        if(!owner.equals(self)) {
            self.async(parent)._addEdge(source, label, target);
        }
    }

    private final IFuture<Env<S, L, D>> doQuery(IActorRef<? extends IUnit<S, L, D, ?>> sender, ScopePath<S, L> path,
            LabelWf<L> labelWF, LabelOrder<L> labelOrder, DataWf<S, L, D> dataWF, DataLeq<S, L, D> dataEquiv,
            DataWf<S, L, D> dataWfInternal, DataLeq<S, L, D> dataEquivInternal) {
        logger.debug("got _query from {}", sender);
        final boolean external = !sender.equals(self);

        final NameResolution<S, L, D> nr = new NameResolution<S, L, D>(edgeLabels, labelOrder) {

            @Override public Optional<IFuture<Env<S, L, D>>> externalEnv(ScopePath<S, L> path, LabelWf<L> re,
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
                final ICompletableFuture<Boolean> result = new CompletableFuture<>();
                if(external || dataWfInternal == null) {
                    dataWF.wf(d, queryContext, cancel).whenComplete(result::complete);
                } else {
                    dataWfInternal.wf(d, queryContext, cancel).whenComplete(result::complete);
                }
                final TypeCheckerState<S, L, D> token = TypeCheckerState.of(sender, ImmutableList.of(d), result);
                waitFor(token, self);
                return result.whenComplete((r, ex) -> {
                    self.assertOnActorThread();
                    granted(token, self);
                });
            }

            @Override protected IFuture<Boolean> dataLeq(D d1, D d2, ICancel cancel) throws InterruptedException {
                final ICompletableFuture<Boolean> result = new CompletableFuture<>();
                if(external || dataEquivInternal == null) {
                    dataEquiv.leq(d1, d2, queryContext, cancel).whenComplete(result::complete);
                } else {
                    dataEquivInternal.leq(d1, d2, queryContext, cancel).whenComplete(result::complete);
                }
                final TypeCheckerState<S, L, D> token = TypeCheckerState.of(sender, ImmutableList.of(d1, d2), result);
                waitFor(token, self);
                return result.whenComplete((r, ex) -> {
                    self.assertOnActorThread();
                    granted(token, self);
                });
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
            // similar to Unit#query, except it does not require the Unit to be ACTIVE

            final ScopePath<S, L> path = new ScopePath<>(scope);
            final IFuture<Env<S, L, D>> result =
                    doQuery(self, path, labelWF, labelOrder, dataWF, dataEquiv, dataWfInternal, dataEquivInternal);
            final Query<S, L, D> wf = Query.of(self, path, labelWF, dataWF, labelOrder, dataEquiv, result);
            waitFor(wf, self);
            stats.ownQueries += 1;
            return self.schedule(result).whenComplete((env, ex) -> {
                granted(wf, self);
            }).thenApply(CapsuleUtil::toSet);
        }

        @Override public IFuture<Boolean> confirmQueries() {
            // TODO Auto-generated method stub
            return null;
        }

    };

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
        logger.debug("{} granted {} by {}", self, token, actor);
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
        if((state.equals(UnitState.ACTIVE) || state.equals(UnitState.UNKNOWN)) && !isWaiting()) {
            logger.debug("{} finish", this);
            state = UnitState.DONE;
            unitResult.complete(UnitResult.of(id(), scopeGraph.get(), recordedQueries, rootScopes, analysis.get(), failures, stats));
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
                failAll();
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
                    differResult -> {
                        // TODO: complete differ result with exception
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
                    logger.error("Differ result could not complete.");
                    throw new IllegalStateException("Differ result could not complete.");
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

        public DifferContext(mb.statix.scopegraph.IScopeGraph.Immutable<S, L, D> previousScopeGraph,
            IScopeGraphDifferOps<S, D> differOps) {
            this.previousScopeGraph = previousScopeGraph;
            this.differOps = differOps;
        }

        @Override public IFuture<Iterable<S>> getCurrentEdges(S scope, L label) {
            // Most be done via owner, because delays on a shared, non-owned scope are not activated
            // and not desired to be activated, because not any operation on them can proceed
            final IActorRef<? extends IUnit<S, L, D, ?>> owner = context.owner(scope);
            final Complete<S, L, D> complete = Complete.of(owner, scope, EdgeOrData.edge(label));

            waitFor(complete, owner);
            return self.async(context.owner(scope))._isComplete(scope, EdgeOrData.edge(label)).thenApply(__ -> {
                granted(complete, owner);
                return scopeGraph.get().getEdges(scope, label);
            });
        }

        @Override public IFuture<Iterable<S>> getPreviousEdges(S scope, L label) {
            return CompletableFuture.completedFuture(previousScopeGraph.getEdges(scope, label));
        }

        @Override public IFuture<Iterable<L>> labels(S currentScope) {
            assertOwnOrSharedScope(currentScope);
            // TODO make more precise with labels for which scope was initialized.
            return CompletableFuture.completedFuture(Set.Immutable.union(scopeGraph.get().getLabels(), previousScopeGraph.getLabels()));
        }

        @Override public IFuture<Optional<D>> currentDatum(S scope) {
            final IActorRef<? extends IUnit<S, L, D, ?>> owner = context.owner(scope);
            final Datum<S, L, D> datum = Datum.of(self, scope);
            waitFor(datum, owner);
            return self.async(owner)._datum(scope)
                .whenComplete((__, ___) -> granted(datum, owner));
        }

        @Override public IFuture<Optional<D>> previousDatum(S scope) {
            return CompletableFuture.completedFuture(previousScopeGraph.getData(scope));
        }

        @Override public IFuture<Boolean> matchDatums(D currentDatum, D previousDatum,
            Function2<S, S, IFuture<Boolean>> scopeMatch) {
            return differOps.matchDatums(currentDatum, previousDatum, scopeMatch);
        }

        @Override public boolean isMatchAllowed(S currentScope, S previousScope) {
            return context.owner(previousScope).equals(context.owner(currentScope));
        }

        @Override public Immutable<S> getCurrentScopes(D d) {
            return differOps.getScopes(d);
        }

        @Override public Immutable<S> getPreviousScopes(D d) {
            return differOps.getScopes(d);
        }

        @Override public boolean ownScope(S scope) {
            return context.owner(scope).equals(self);
        }

        @Override public boolean ownOrSharedScope(S currentScope) {
            return scopes.contains(currentScope);
        }

        @Override public IFuture<Optional<S>> externalMatch(S previousScope) {
            final IActorRef<? extends IUnit<S, L, D, ?>> owner = context.owner(previousScope);
            final Match<S, L, D> match = Match.of(self, previousScope);
            waitFor(match, owner);
            return self.async(context.owner(previousScope))._match(previousScope).
                whenComplete((__, ___) -> granted(match, owner));
        }

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

    private void assertInState(UnitState s1, UnitState s2) {
        if(!state.equals(s1) && !state.equals(s2)) {
            logger.error("Expected state {} or {}, was {}", s1, s2, state);
            throw new IllegalStateException("Expected state " + s1 + "or" + s2 + ", was " + state);
        }
    }

    private void assertOwnScope(S scope) {
        if(!context.owner(scope).equals(self)) {
            logger.error("Scope {} is not owned {}", scope, this);
            throw new IllegalArgumentException("Scope " + scope + " is not owned " + this);
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