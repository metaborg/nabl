package mb.statix.p_raffrayi.impl;

import static com.google.common.collect.Streams.stream;

import java.util.Map.Entry;
import java.util.Optional;

import javax.annotation.Nullable;

import com.google.common.collect.Streams;

import io.usethesource.capsule.Set;
import mb.nabl2.util.CapsuleUtil;
import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation3;
import mb.nabl2.util.collections.MultiSet;
import mb.nabl2.util.collections.MultiSetMap;
import mb.statix.actors.IActor;
import mb.statix.actors.IActorMonitor;
import mb.statix.actors.IActorRef;
import mb.statix.actors.futures.CompletableFuture;
import mb.statix.actors.futures.ICompletable;
import mb.statix.actors.futures.IFuture;
import mb.statix.p_raffrayi.ITypeChecker;
import mb.statix.p_raffrayi.IUnitResult;
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

    private final IActor<? extends IUnit<S, L, D, R>> self;
    private final @Nullable IActorRef<? extends IUnit2UnitProtocol<S, L, D>> parent;
    private final IUnitContext<S, L, D, R> context;
    private final ITypeChecker<S, L, D, R> unitChecker;

    private final IUnit<S, L, D, R> local;
    private Clock<S, L, D> clock;
    private final CompletableFuture<IUnitResult<S, L, D, R>> unitResult;

    private final IScopeGraph.Transient<S, L, D> scopeGraph;
    private final MultiSet.Transient<S> sharedScopes;
    private final MultiSet.Transient<S> uninitializedScopes;
    private final MultiSetMap.Transient<S, EdgeOrData<L>> openEdges;
    private final IRelation3.Transient<S, EdgeOrData<L>, ICompletable<Void>> delays;

    private final MultiSet.Transient<String> scopeNameCounters;

    public Unit(IActor<? extends IUnit<S, L, D, R>> self,
            @Nullable IActorRef<? extends IUnit2UnitProtocol<S, L, D>> parent, IUnitContext<S, L, D, R> context,
            ITypeChecker<S, L, D, R> unitChecker, Iterable<L> edgeLabels) {
        this.self = self;
        this.parent = parent;
        this.context = context;
        this.unitChecker = unitChecker;

        this.local = self.async(self);
        this.clock = Clock.of();
        this.unitResult = new CompletableFuture<>();

        this.scopeGraph = ScopeGraph.Transient.of(edgeLabels);
        this.sharedScopes = MultiSet.Transient.of();
        this.uninitializedScopes = MultiSet.Transient.of();
        this.openEdges = MultiSetMap.Transient.of();
        this.delays = HashTrieRelation3.Transient.of();

        this.scopeNameCounters = MultiSet.Transient.of();
    }

    @SuppressWarnings("unchecked") private IActorRef<? extends IUnit<S, L, D, R>> sender() {
        return (IActorRef<? extends IUnit<S, L, D, R>>) self.sender();
    }

    ///////////////////////////////////////////////////////////////////////////
    // IUnitProtocol interface, called by IUnit implementations
    ///////////////////////////////////////////////////////////////////////////

    @Override public IFuture<IUnitResult<S, L, D, R>> _start(@Nullable S root) {
        try {
            uninitializedScopes.add(root);
            context.waitFor(IWaitFor.of("init", root), self);

            // run() after inits are initialized before run, since unitChecker
            // can immediately call methods, that are executed synchronously

            this.unitChecker.run(this, root).whenComplete(this::handleResult);
        } catch(InterruptedException e) {
            // FIXME Handle this
            e.printStackTrace();
        }
        return unitResult;
    }

    private void handleResult(R result, Throwable ex) {
        // TODO Change state
        if(ex != null) {
            unitResult.completeExceptionally(ex);
        } else {
            unitResult.completeValue(UnitResult.of(result, scopeGraph.freeze()));
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // ITypeCheckerContext interface, called by ITypeChecker implementations
    ///////////////////////////////////////////////////////////////////////////

    // NB. Invoke methods via `local` so that we have the same scheduling & ordering
    // guarantees as for remote calls.

    @Override public void add(String id, ITypeChecker<S, L, D, R> unitChecker, S root) {
        assertShared(root);
        // ASSERT root is owned by us, or shared with us

        final IActorRef<? extends IUnit2UnitProtocol<S, L, D>> subunit = context.add(id, unitChecker, root);

        uninitializedScopes.add(root);
        context.waitFor(IWaitFor.of("init", root), subunit);
    }

    @Override public void initRoot(S root, Iterable<L> labels, boolean shared) {
        local._initRoot(root, labels, shared);
    }

    @Override public S freshScope(String baseName, Iterable<L> labels, Iterable<Access> data, boolean shared) {
        final String name = baseName.replace("-", "_");
        final int n = scopeNameCounters.add(name);
        final S scope = context.makeScope(name + "-" + n);

        Streams.concat(stream(labels).map(EdgeOrData::edge), stream(data).map(EdgeOrData::<L>data)).forEach(edge -> {
            openEdges.put(scope, edge);
            context.waitFor(IWaitFor.of("closeEdge", scope, edge), self);
        });

        if(shared) {
            sharedScopes.add(scope);
            context.waitFor(IWaitFor.of("closeScope", scope), self);
        }

        return scope;
    }

    @Override public void setDatum(S scope, D datum, Access access) {
        local._setDatum(scope, datum, access);
    }

    @Override public void addEdge(S source, L label, S target) {
        local._addEdge(source, label, target);
    }

    @Override public void closeEdge(S source, L label) {
        local._closeEdge(source, label);
    }

    @Override public void closeShare(S scope) {
        local._closeShare(scope);
    }

    @Override public IFuture<Set<IResolutionPath<S, L, D>>> query(S scope, LabelWF<L> labelWF, DataWF<D> dataWF,
            LabelOrder<L> labelOrder, DataLeq<D> dataEquiv) {
        final IFuture<Env<S, L, D>> result = local._query(Paths.empty(scope), labelWF, dataWF, labelOrder, dataEquiv);
        clock = clock.sent(self);
        context.waitFor(IWaitFor.of("answer", result), self);
        return result.whenComplete((env, ex) -> {
            clock = clock.received(self);
            context.granted(IWaitFor.of("answer", result), self);
        }).thenApply(CapsuleUtil::toSet);
    }

    ///////////////////////////////////////////////////////////////////////////
    // IUnitProtocol interface, called by IUnit implementations
    ///////////////////////////////////////////////////////////////////////////

    @Override public final void _initRoot(S root, Iterable<L> labels, boolean shared) {
        clock = clock.received(sender());

        assertUninitialized(root);

        uninitializedScopes.remove(root);
        context.granted(IWaitFor.of("init", root), sender());

        stream(labels).map(EdgeOrData::edge).forEach(edge -> {
            openEdges.put(root, edge);
            context.waitFor(IWaitFor.of("closeEdge", root, edge), sender());
        });

        if(shared) {
            sharedScopes.add(root);
            context.waitFor(IWaitFor.of("closeScope", root), sender());
        }

        final IActorRef<? extends IUnit2UnitProtocol<S, L, D>> owner = context.owner(root);
        if(owner.equals(self)) {
            if(isScopeComplete(root)) {
                releaseDelays(root);
            }
        } else {
            if(parent == null) {
                throw new IllegalArgumentException("Not our own scope, and no parent: cannot propagate up.");
            }
            self.async(parent)._initRoot(root, labels, shared);
            clock = clock.sent(parent);
        }

    }

    @Override public final void _setDatum(S scope, D datum, Access access) {
        clock = clock.received(sender());

        final EdgeOrData<L> edge = EdgeOrData.data(access);
        assertEdgeOpen(scope, edge);
        if(access.equals(Access.EXTERNAL)) {
            assertEdgeClosed(scope, EdgeOrData.data(Access.INTERNAL));
        }

        scopeGraph.setDatum(scope, datum);
        openEdges.remove(scope, edge);
        context.granted(IWaitFor.of("closeEdge", scope, edge), self);

        final IActorRef<? extends IUnit2UnitProtocol<S, L, D>> owner = context.owner(scope);
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

        scopeGraph.addEdge(source, label, target);

        final IActorRef<? extends IUnit2UnitProtocol<S, L, D>> owner = context.owner(source);
        if(!owner.equals(self)) {
            self.async(owner)._addEdge(source, label, target);
        }
    }

    @Override public final void _closeEdge(S source, L label) {
        clock = clock.received(sender());

        assertEdgeOpen(source, EdgeOrData.edge(label));

        final EdgeOrData<L> edge = EdgeOrData.edge(label);
        openEdges.remove(source, edge);
        context.granted(IWaitFor.of("closeEdge", source, edge), sender());

        final IActorRef<? extends IUnit2UnitProtocol<S, L, D>> owner = context.owner(source);
        if(owner.equals(self)) {
            if(isEdgeComplete(source, edge)) {
                releaseDelays(source, edge);
            }
        } else {
            self.async(owner)._closeEdge(source, label);
            clock = clock.sent(owner);
        }
    }

    @Override public final void _closeShare(S scope) {
        clock = clock.received(sender());

        assertShared(scope);

        sharedScopes.remove(scope);
        context.granted(IWaitFor.of("closeScope", scope), sender());

        final IActorRef<? extends IUnit2UnitProtocol<S, L, D>> owner = context.owner(scope);
        if(owner.equals(self)) {
            if(isScopeComplete(scope)) {
                releaseDelays(scope);
            }
        } else {
            self.async(owner)._closeShare(scope);
            clock = clock.sent(owner);
        }
    }

    @Override public final IFuture<Env<S, L, D>> _query(IScopePath<S, L> path, LabelWF<L> labelWF, DataWF<D> dataWF,
            LabelOrder<L> labelOrder, DataLeq<D> dataEquiv) {
        final NameResolution<S, L, D> nr =
                new NameResolution<S, L, D>(scopeGraph, labelOrder, dataWF, dataEquiv, this::isComplete) {

                    @Override public Optional<IFuture<Env<S, L, D>>> externalEnv(IScopePath<S, L> path, LabelWF<L> re,
                            LabelOrder<L> labelOrder, DataWF<D> dataWF, DataLeq<D> dataEquiv) {
                        final IActorRef<? extends IUnit2UnitProtocol<S, L, D>> owner = context.owner(path.getTarget());
                        if(owner.equals(self)) {
                            return Optional.empty();
                        } else {
                            final IFuture<Env<S, L, D>> result =
                                    (self.async(owner)._query(path, labelWF, dataWF, labelOrder, dataEquiv));
                            context.waitFor(IWaitFor.of("answer", result), owner);
                            return Optional.of(result.whenComplete((r, ex) -> {
                                context.granted(IWaitFor.of("answer", result), owner);
                                clock = clock.received(owner);
                            }));
                        }
                    }

                };
        final IActorRef<? extends IUnit<S, L, D, R>> sender = sender(); // capture for correct use in whenComplete
        final IFuture<Env<S, L, D>> result = nr.env(path, labelWF, context.cancel());
        return result.whenComplete((r, ex) -> {
            clock = clock.sent(sender);
        });
    }

    @Override public final void _complete(ICompletable<Void> future) {
        future.completeValue(null);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Resolution & delays
    ///////////////////////////////////////////////////////////////////////////

    private void releaseDelays(S scope) {
        for(Entry<EdgeOrData<L>, ICompletable<Void>> entry : delays.get(scope)) {
            final ICompletable<Void> future = entry.getValue();
            final EdgeOrData<L> edge = entry.getKey();
            if(!openEdges.contains(scope, edge)) {
                delays.remove(scope, edge, future);
                local._complete(future);
            }
        }
    }

    private void releaseDelays(S scope, EdgeOrData<L> edge) {
        for(ICompletable<Void> future : delays.get(scope, edge)) {
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
            result.completeValue(null);
        } else {
            delays.put(scope, edge, result);
        }
        return result;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Assertions
    ///////////////////////////////////////////////////////////////////////////

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

    ///////////////////////////////////////////////////////////////////////////
    // Deadlock handling
    ///////////////////////////////////////////////////////////////////////////

    @Override public void suspended(IActorRef<?> actor) {
        context.suspend(clock);
    }

}