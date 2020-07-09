package mb.statix.concurrent.p_raffrayi.impl;

import static com.google.common.collect.Streams.stream;

import java.util.Map.Entry;
import java.util.Optional;

import javax.annotation.Nullable;

import org.metaborg.util.Ref;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Streams;

import io.usethesource.capsule.Set;
import mb.nabl2.util.CapsuleUtil;
import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation3;
import mb.nabl2.util.collections.MultiSet;
import mb.nabl2.util.collections.MultiSetMap;
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
import mb.statix.concurrent.p_raffrayi.impl.tokens.CloseEdge;
import mb.statix.concurrent.p_raffrayi.impl.tokens.CloseScope;
import mb.statix.concurrent.p_raffrayi.impl.tokens.InitScope;
import mb.statix.concurrent.p_raffrayi.impl.tokens.Resolution;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.path.IScopePath;
import mb.statix.scopegraph.reference.Access;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.EdgeOrData;
import mb.statix.scopegraph.reference.Env;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ScopeGraph;
import mb.statix.scopegraph.terms.path.Paths;

class Unit<S, L, D, R> implements IUnit<S, L, D, R>, IActorMonitor {

    private final ILogger logger;

    private final TypeTag<IUnit<S, L, D, R>> TYPE = TypeTag.of(IUnit.class);

    private final IActor<? extends IUnit<S, L, D, R>> self;
    private final @Nullable IActorRef<? extends IUnit<S, L, D, R>> parent;
    private final IUnitContext<S, L, D, R> context;
    private final ITypeChecker<S, L, D, R> typeChecker;

    private final IUnit<S, L, D, R> local;
    private Clock<IActorRef<? extends IUnit<S, L, D, R>>> clock;
    private UnitState state;
    private final CompletableFuture<R> typeCheckerResult;

    private Ref<IScopeGraph.Immutable<S, L, D>> scopeGraph;
    private final MultiSet.Transient<S> sharedScopes;
    private final MultiSet.Transient<S> uninitializedScopes;
    private final MultiSetMap.Transient<S, EdgeOrData<L>> openEdges;
    private final IRelation3.Transient<S, EdgeOrData<L>, ICompletable<Void>> delays;

    private final MultiSet.Transient<String> scopeNameCounters;

    Unit(IActor<? extends IUnit<S, L, D, R>> self, @Nullable IActorRef<? extends IUnit<S, L, D, R>> parent,
            IUnitContext<S, L, D, R> context, ITypeChecker<S, L, D, R> unitChecker, Iterable<L> edgeLabels) {
        this.logger = LoggerUtils.logger("Unit[" + self.id() + "]");

        this.self = self;
        this.parent = parent;
        this.context = context;
        this.typeChecker = unitChecker;

        this.local = self.async(self);
        this.clock = Clock.of();
        this.state = UnitState.INIT;
        this.typeCheckerResult = new CompletableFuture<>();

        this.scopeGraph = new Ref<>(ScopeGraph.Immutable.of(edgeLabels));
        this.sharedScopes = MultiSet.Transient.of();
        this.uninitializedScopes = MultiSet.Transient.of();
        this.openEdges = MultiSetMap.Transient.of();
        this.delays = HashTrieRelation3.Transient.of();

        this.scopeNameCounters = MultiSet.Transient.of();

        self.addMonitor(this);
    }

    ///////////////////////////////////////////////////////////////////////////
    // IUnitProtocol interface, called by IUnit implementations
    ///////////////////////////////////////////////////////////////////////////

    @Override public void _start(@Nullable S root) {
        assertInState(UnitState.INIT);

        state = UnitState.ACTIVE;
        if(root != null) {
            uninitializedScopes.add(root);
            context.waitFor(InitScope.of(root), self);
        }

        // run() after inits are initialized before run, since unitChecker
        // can immediately call methods, that are executed synchronously

        this.typeChecker.run(this, root).whenComplete(this::handleResult);
    }

    private void handleResult(R result, Throwable ex) {
        assertInState(UnitState.INIT, UnitState.ACTIVE);

        state = UnitState.DONE;

        // FIXME Only do this once all wait-fors are gone!
        if(ex != null) {
            typeCheckerResult.completeExceptionally(ex);
        } else {
            typeCheckerResult.complete(result);
        }
    }

    @Override public IFuture<IUnitResult<S, L, D, R>> _done() {
        assertInState(UnitState.DONE);
        final CompletableFuture<IUnitResult<S, L, D, R>> unitResult = new CompletableFuture<>();
        typeCheckerResult.whenComplete((analysis, ex) -> {
            if(ex != null) {
                unitResult.completeExceptionally(ex);
            } else {
                unitResult.complete(UnitResult.of(analysis, scopeGraph.get()));
            }
        });
        return unitResult;
    }

    @Override public void _fail() {
        // TODO What to do here?
        throw new IllegalStateException("Failed, do not know how to handle.");
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

        assertShared(root);
        // ASSERT root is owned by us, or shared with us

        final IActorRef<? extends IUnit<S, L, D, R>> subunit = context.add(id, unitChecker, root);

        uninitializedScopes.add(root);
        context.waitFor(InitScope.of(root), subunit);
    }

    @Override public void initRoot(S root, Iterable<L> labels, boolean shared) {
        assertInState(UnitState.ACTIVE);

        local._initRoot(root, labels, shared);
    }

    @Override public S freshScope(String baseName, Iterable<L> labels, Iterable<Access> data, boolean shared) {
        assertInState(UnitState.ACTIVE);

        final String name = baseName.replace("-", "_");
        final int n = scopeNameCounters.add(name);
        final S scope = context.makeScope(name + "-" + n);

        Streams.concat(stream(labels).map(EdgeOrData::edge), stream(data).map(EdgeOrData::<L>data)).forEach(edge -> {
            openEdges.put(scope, edge);
            context.waitFor(CloseEdge.of(scope, edge), self);
        });

        if(shared) {
            sharedScopes.add(scope);
            context.waitFor(CloseScope.of(scope), self);
        }

        return scope;
    }

    @Override public void setDatum(S scope, D datum, Access access) {
        assertInState(UnitState.ACTIVE);

        local._setDatum(scope, datum, access);
    }

    @Override public void addEdge(S source, L label, S target) {
        assertInState(UnitState.ACTIVE);

        local._addEdge(source, label, target);
    }

    @Override public void closeEdge(S source, L label) {
        assertInState(UnitState.ACTIVE);

        local._closeEdge(source, label);
    }

    @Override public void closeScope(S scope) {
        assertInState(UnitState.ACTIVE);

        local._closeScope(scope);
    }

    @Override public IFuture<Set<IResolutionPath<S, L, D>>> query(S scope, LabelWF<L> labelWF, DataWF<D> dataWF,
            LabelOrder<L> labelOrder, DataLeq<D> dataEquiv) {
        assertInState(UnitState.ACTIVE);

        final IFuture<Env<S, L, D>> result = local._query(Paths.empty(scope), labelWF, dataWF, labelOrder, dataEquiv);
        context.waitFor(Resolution.of(result), self);
        return result.whenComplete((env, ex) -> {
            context.granted(Resolution.of(result), self);
        }).thenApply(CapsuleUtil::toSet);
    }

    ///////////////////////////////////////////////////////////////////////////
    // IUnitProtocol interface, called by IUnit implementations
    ///////////////////////////////////////////////////////////////////////////

    @Override public final void _initRoot(S root, Iterable<L> labels, boolean shared) {
        assertUninitialized(root);

        uninitializedScopes.remove(root);
        context.granted(InitScope.of(root), self.sender(TYPE));

        stream(labels).map(EdgeOrData::edge).forEach(edge -> {
            openEdges.put(root, edge);
            context.waitFor(CloseEdge.of(root, edge), self.sender(TYPE));
        });

        if(shared) {
            sharedScopes.add(root);
            context.waitFor(CloseScope.of(root), self.sender(TYPE));
        }

        final IActorRef<? extends IUnit<S, L, D, R>> owner = context.owner(root);
        if(owner.equals(self)) {
            if(isScopeComplete(root)) {
                releaseDelays(root);
            }
        } else {
            if(parent == null) {
                throw new IllegalArgumentException(root + " not our own scope, and no parent: cannot propagate up.");
            }
            self.async(parent)._initRoot(root, labels, shared);
        }

    }

    @Override public final void _setDatum(S scope, D datum, Access access) {
        final EdgeOrData<L> edge = EdgeOrData.data(access);
        assertEdgeOpen(scope, edge);
        if(access.equals(Access.EXTERNAL)) {
            assertEdgeClosed(scope, EdgeOrData.data(Access.INTERNAL));
        }

        scopeGraph.set(scopeGraph.get().setDatum(scope, datum));
        openEdges.remove(scope, edge);
        context.granted(CloseEdge.of(scope, edge), self);

        final IActorRef<? extends IUnit<S, L, D, R>> owner = context.owner(scope);
        if(owner.equals(self)) {
            if(isEdgeComplete(scope, edge)) {
                releaseDelays(scope, edge);
            }
        } else {
            throw new IllegalArgumentException("Scope " + scope + " is not owned by this actor: cannot set datum.");

        }

    }

    @Override public final void _addEdge(S source, L label, S target) {
        assertEdgeOpen(source, EdgeOrData.edge(label));

        scopeGraph.set(scopeGraph.get().addEdge(source, label, target));

        final IActorRef<? extends IUnit<S, L, D, R>> owner = context.owner(source);
        if(!owner.equals(self)) {
            self.async(owner)._addEdge(source, label, target);
        }
    }

    @Override public final void _closeEdge(S source, L label) {
        assertEdgeOpen(source, EdgeOrData.edge(label));

        final EdgeOrData<L> edge = EdgeOrData.edge(label);
        openEdges.remove(source, edge);
        context.granted(CloseEdge.of(source, edge), self.sender(TYPE));

        final IActorRef<? extends IUnit<S, L, D, R>> owner = context.owner(source);
        if(owner.equals(self)) {
            if(isEdgeComplete(source, edge)) {
                releaseDelays(source, edge);
            }
        } else {
            self.async(owner)._closeEdge(source, label);
        }
    }

    @Override public final void _closeScope(S scope) {
        assertShared(scope);

        sharedScopes.remove(scope);
        context.granted(CloseScope.of(scope), self.sender(TYPE));

        final IActorRef<? extends IUnit<S, L, D, R>> owner = context.owner(scope);
        if(owner.equals(self)) {
            if(isScopeComplete(scope)) {
                releaseDelays(scope);
            }
        } else {
            self.async(owner)._closeScope(scope);
        }
    }

    @Override public final IFuture<Env<S, L, D>> _query(IScopePath<S, L> path, LabelWF<L> labelWF, DataWF<D> dataWF,
            LabelOrder<L> labelOrder, DataLeq<D> dataEquiv) {
        final IActorRef<? extends IUnit<S, L, D, R>> sender = self.sender(TYPE); // capture for correct use in whenComplete

        logger.info("got _query from {}", sender);
        final Access access = sender.equals(self) ? Access.INTERNAL : Access.EXTERNAL;
        final NameResolution<S, L, D> nr = new NameResolution<S, L, D>(scopeGraph, labelOrder, dataWF, dataEquiv,
                access, this::isComplete, logger) {

            @Override public Optional<IFuture<Env<S, L, D>>> externalEnv(IScopePath<S, L> path, LabelWF<L> re,
                    LabelOrder<L> labelOrder, DataWF<D> dataWF, DataLeq<D> dataEquiv) {
                final IActorRef<? extends IUnit<S, L, D, R>> owner = context.owner(path.getTarget());
                if(owner.equals(self)) {
                    return Optional.empty();
                } else {
                    logger.info("have _query for {}", owner);
                    // this code mirrors query(...)
                    final IFuture<Env<S, L, D>> result =
                            self.async(owner)._query(path, labelWF, dataWF, labelOrder, dataEquiv);
                    context.waitFor(Resolution.of(result), owner);
                    return Optional.of(result.whenComplete((r, ex) -> {
                        logger.info("got answer from {}", sender);
                        context.granted(Resolution.of(result), owner);
                    }));
                }
            }

        };

        return nr.env(path, labelWF, context.cancel()).whenComplete((env, ex) -> {
            logger.info("have answer for {}", sender);
        });
    }

    @Override public final void _complete(ICompletable<Void> future) {
        future.complete(null);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Resolution & delays
    ///////////////////////////////////////////////////////////////////////////

    private void releaseDelays(S scope) {
        for(Entry<EdgeOrData<L>, ICompletable<Void>> entry : delays.get(scope)) {
            final EdgeOrData<L> edge = entry.getKey();
            if(!openEdges.contains(scope, edge)) {
                final ICompletable<Void> future = entry.getValue();
                logger.info("released {} on {}(/{})", future, scope, edge);
                delays.remove(scope, edge, future);
                local._complete(future);
            }
        }
    }

    private void releaseDelays(S scope, EdgeOrData<L> edge) {
        for(ICompletable<Void> future : delays.get(scope, edge)) {
            logger.info("released {} on {}/{}", future, scope, edge);
            delays.remove(scope, edge, future);
            local._complete(future);
        }
    }

    private boolean isScopeComplete(S scope) {
        return !sharedScopes.contains(scope) && !uninitializedScopes.contains(scope);
    }

    private boolean isEdgeComplete(S scope, EdgeOrData<L> edge) {
        return isScopeComplete(scope) && !openEdges.contains(scope, edge);
    }

    private IFuture<Void> isComplete(S scope, EdgeOrData<L> edge) {
        final CompletableFuture<Void> result = new CompletableFuture<>();
        if(isEdgeComplete(scope, edge)) {
            result.complete(null);
        } else {
            logger.info("delayed {} on {}/{}", result, scope, edge);
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
        }
    }

    @SuppressWarnings("unchecked") @Override public void delivered(IActor<?> self, IActorRef<?> source,
            java.util.Set<String> tags) {
        if(tags.contains("stuckness")) {
            clock = clock.delivered((IActorRef<? extends IUnit<S, L, D, R>>) source);
        }
    }

    @Override public void suspended(IActor<?> self) {
        if(state.equals(UnitState.INIT)) {
            return;
        }

        context.suspended(state, clock);
    }

    @Override public void stopped(IActor<?> self) {
        if(!state.equals(UnitState.DONE)) {
            state = UnitState.DONE;
            // TODO Cleanup
        }
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
        throw new IllegalStateException("Expected state " + states + ", was " + state);
    }

    private void assertShared(S scope) {
        if(!sharedScopes.contains(scope)) {
            throw new IllegalArgumentException("Scope " + scope + " must be shared.");
        }
    }

    private void assertUninitialized(S scope) {
        if(!uninitializedScopes.contains(scope)) {
            throw new IllegalArgumentException("Scope " + scope + " must be uninitialized.");
        }
    }

    private void assertEdgeOpen(S source, EdgeOrData<L> edgeOrDatum) {
        if(!openEdges.contains(source, edgeOrDatum)) {
            throw new IllegalArgumentException("Edge or datum " + source + "/" + edgeOrDatum + " must be open.");
        }
    }

    private void assertEdgeClosed(S source, EdgeOrData<L> edgeOrDatum) {
        if(openEdges.contains(source, edgeOrDatum)) {
            throw new IllegalArgumentException("Edge or datum " + source + "/" + edgeOrDatum + " must be closed.");
        }
    }

}