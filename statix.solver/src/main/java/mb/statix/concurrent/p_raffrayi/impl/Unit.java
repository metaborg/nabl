package mb.statix.concurrent.p_raffrayi.impl;

import static com.google.common.collect.Streams.stream;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.metaborg.util.Ref;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Lists;

import io.usethesource.capsule.Set;
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
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.ITypeChecker;
import mb.statix.concurrent.p_raffrayi.IUnitResult;
import mb.statix.concurrent.p_raffrayi.TypeCheckingFailedException;
import mb.statix.concurrent.p_raffrayi.impl.tokens.CloseLabel;
import mb.statix.concurrent.p_raffrayi.impl.tokens.CloseScope;
import mb.statix.concurrent.p_raffrayi.impl.tokens.InitScope;
import mb.statix.concurrent.p_raffrayi.impl.tokens.Query;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.path.IScopePath;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.EdgeOrData;
import mb.statix.scopegraph.reference.Env;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;
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

    private final CompletableFuture<R> typeCheckerResult;
    private final CompletableFuture<IUnitResult<S, L, D, R>> unitResult;

    private final Ref<IScopeGraph.Immutable<S, L, D>> scopeGraph;
    private final Set.Transient<S> scopes;
    private final IRelation3.Transient<S, EdgeOrData<L>, ICompletable<Void>> delays;

    private final MultiSet.Transient<String> scopeNameCounters;

    Unit(IActor<? extends IUnit<S, L, D, R>> self, @Nullable IActorRef<? extends IUnit<S, L, D, R>> parent,
            IUnitContext<S, L, D, R> context, ITypeChecker<S, L, D, R> unitChecker, Iterable<L> edgeLabels) {
        this.self = self;
        this.parent = parent;
        this.context = context;
        this.typeChecker = unitChecker;

        this.clock = Clock.of();
        this.state = UnitState.INIT;

        this.typeCheckerResult = new CompletableFuture<>();
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

        this.typeChecker.run(this, root).whenComplete(this::handleResult);

        return unitResult;
    }

    private void handleResult(R result, Throwable ex) {
        assertInState(UnitState.INIT, UnitState.ACTIVE);
        state = UnitState.DONE;
        typeCheckerResult.complete(result, ex);
    }

    @Override public void _done() {
        assertInState(UnitState.DONE);

        // typeCheckerResult should be completed here, since we go to DONE only
        // in handleResult
        typeCheckerResult.whenComplete((analysis, ex) -> {
            if(ex != null) {
                unitResult.completeExceptionally(ex);
            } else {
                unitResult.complete(UnitResult.of(analysis, scopeGraph.get()));
            }
        });
    }

    @Override public void _deadlocked() {
        fail(new Exception("Deadlocked"));
    }

    private void fail(Throwable ex) {
        final TypeCheckingFailedException tcfe = new TypeCheckingFailedException(ex);
        unitResult.completeExceptionally(tcfe);
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

    @Override public IFuture<Set<IResolutionPath<S, L, D>>> query(S scope, LabelWF<L> labelWF, DataWF<D> dataWF,
            LabelOrder<L> labelOrder, DataLeq<D> dataEquiv) {
        assertInState(UnitState.ACTIVE);

        final IScopePath<S, L> path = Paths.empty(scope);
        final IFuture<Env<S, L, D>> result = doQuery(self, path, labelWF, dataWF, labelOrder, dataEquiv);
        final Query<S, L, D> wf = Query.of(path, labelWF, dataWF, labelOrder, dataEquiv, result);
        context.waitFor(wf, self);

        return self.schedule(result).whenComplete((env, ex) -> {
            context.granted(wf, self);
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

    @Override public final IFuture<Env<S, L, D>> _query(IScopePath<S, L> path, LabelWF<L> labelWF, DataWF<D> dataWF,
            LabelOrder<L> labelOrder, DataLeq<D> dataEquiv) {
        return doQuery(self.sender(TYPE), path, labelWF, dataWF, labelOrder, dataEquiv);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implementations -- independent of message handling context
    ///////////////////////////////////////////////////////////////////////////

    private final void doAddLocalShare(IActorRef<? extends IUnit<S, L, D, R>> sender, S scope) {
        scopes.__insert(scope);
        context.waitFor(InitScope.of(scope), sender);
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

        context.granted(InitScope.of(scope), sender);
        for(EdgeOrData<L> edge : edges) {
            context.waitFor(CloseLabel.of(scope, edge), sender);
        }
        if(sharing) {
            context.waitFor(CloseScope.of(scope), sender);
        }

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

        context.granted(CloseScope.of(scope), sender);

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

        context.granted(CloseLabel.of(scope, edge), sender);

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
            LabelWF<L> labelWF, DataWF<D> dataWF, LabelOrder<L> labelOrder, DataLeq<D> dataEquiv) {
        logger.debug("got _query from {}", sender);
        final boolean external = !sender.equals(self);
        final NameResolution<S, L, D> nr =
                new NameResolution<S, L, D>(scopeGraph.get().getEdgeLabels(), labelOrder, dataWF, dataEquiv) {

                    @Override public Optional<IFuture<Env<S, L, D>>> externalEnv(IScopePath<S, L> path, LabelWF<L> re,
                            LabelOrder<L> labelOrder, DataWF<D> dataWF, DataLeq<D> dataEquiv) {
                        final IActorRef<? extends IUnit<S, L, D, R>> owner = context.owner(path.getTarget());
                        if(owner.equals(self)) {
                            return Optional.empty();
                        } else {
                            logger.debug("have _query for {}", owner);
                            // this code mirrors query(...)
                            final IFuture<Env<S, L, D>> result =
                                    self.async(owner)._query(path, re, dataWF, labelOrder, dataEquiv);
                            final Query<S, L, D> wf = Query.of(path, re, dataWF, labelOrder, dataEquiv, result);
                            context.waitFor(wf, owner);
                            return Optional.of(result.whenComplete((r, ex) -> {
                                logger.debug("got answer from {}", sender);
                                context.granted(wf, owner);
                            }));
                        }
                    }

                    @Override protected IFuture<Optional<D>> getDatum(S scope) {
                        return isComplete(scope, EdgeOrData.data()).thenCompose(__ -> {
                            final Optional<D> datum;
                            if(!(datum = scopeGraph.get().getData(scope)).isPresent()) {
                                return CompletableFuture.completedFuture(Optional.empty());
                            }
                            if(external) {
                                return typeChecker.getExternalRepresentation(datum.get()).thenApply(Optional::of);
                            } else {
                                return CompletableFuture.completedFuture(datum);
                            }
                        });
                    }

                    @Override protected IFuture<Iterable<S>> getEdges(S scope, L label) {
                        return isComplete(scope, EdgeOrData.edge(label)).thenApply(__ -> {
                            return scopeGraph.get().getEdges(scope, label);
                        });
                    }

                };

        return nr.env(path, labelWF, context.cancel()).whenComplete((env, ex) -> {
            logger.debug("have answer for {}", sender);
        });
    }

    ///////////////////////////////////////////////////////////////////////////
    // Resolution & delays
    ///////////////////////////////////////////////////////////////////////////

    private void releaseDelays(S scope) {
        for(Entry<EdgeOrData<L>, ICompletable<Void>> entry : delays.get(scope)) {
            final EdgeOrData<L> edge = entry.getKey();
            if(!context.isWaitingFor(CloseLabel.of(scope, edge))) {
                final ICompletable<Void> future = entry.getValue();
                logger.debug("released {} on {}(/{})", future, scope, edge);
                delays.remove(scope, edge, future);
                self.complete(future, null, null);
            }
        }
    }

    private void releaseDelays(S scope, EdgeOrData<L> edge) {
        for(ICompletable<Void> future : delays.get(scope, edge)) {
            logger.debug("released {} on {}/{}", future, scope, edge);
            delays.remove(scope, edge, future);
            self.complete(future, null, null);
        }
    }

    private boolean isScopeInitialized(S scope) {
        return !context.isWaitingFor(InitScope.of(scope)) && !context.isWaitingFor(CloseScope.of(scope));
    }

    private boolean isEdgeClosed(S scope, EdgeOrData<L> edge) {
        return isScopeInitialized(scope) && !context.isWaitingFor(CloseLabel.of(scope, edge));
    }

    private IFuture<Void> isComplete(S scope, EdgeOrData<L> edge) {
        final CompletableFuture<Void> result = new CompletableFuture<>();
        if(isEdgeClosed(scope, edge)) {
            result.complete(null);
        } else {
            logger.debug("delayed {} on {}/{}", result, scope, edge);
            delays.put(scope, edge, result);
        }
        return result;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Deadlock handling
    ///////////////////////////////////////////////////////////////////////////

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
            return;
        }
        // even when DONE it is important to report suspend, to
        // have correct deadlock detection of querying units

        context.suspended(state, clock);
    }

    @Override public void stopped(IActor<?> self) {
        context.stopped(clock);
    }

    @Override public void failed(IActor<?> self, Throwable ex) {
        fail(ex);
        context.stopped(clock);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Assertions
    ///////////////////////////////////////////////////////////////////////////

    private void assertInState(UnitState... states) {
        for(UnitState s : states) {
            if(state.equals(s)) {
                return;
            }
        }
        throw new IllegalStateException("Expected state " + Arrays.toString(states) + ", was " + state);
    }

    private void assertOwnOrSharedScope(S scope) {
        if(!scopes.contains(scope)) {
            throw new IllegalArgumentException("Scope " + scope + " is not owned or shared.");
        }
    }

    private void assertLabelOpen(S scope, EdgeOrData<L> edge) {
        assertOwnOrSharedScope(scope);
        if(isEdgeClosed(scope, edge)) {
            throw new IllegalArgumentException("Edge " + edge + " is not open.");
        }
    }

}