package mb.statix.solver.concurrent2.impl;

import static com.google.common.collect.Streams.stream;

import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.spoofax.terms.util.NotImplementedException;

import io.usethesource.capsule.SetMultimap;
import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation3;
import mb.nabl2.util.collections.MultiSetMap;
import mb.statix.actors.IActor;
import mb.statix.actors.IActorRef;
import mb.statix.actors.futures.CompletableFuture;
import mb.statix.actors.futures.IFuture;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.path.IScopePath;
import mb.statix.scopegraph.reference.Access;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.EdgeOrData;
import mb.statix.scopegraph.reference.Env;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ScopeGraph;

public abstract class AbstractClient<S, L, D> implements IUnit2UnitProtocol<S, L, D> {

    private final IActor<? extends IUnit2UnitProtocol<S, L, D>> self;
    private final @Nullable IActorRef<? extends IUnit2UnitProtocol<S, L, D>> parent;
    private final IUnitContext<S, L, D> context;

    private final S root;
    private final IScopeGraph.Transient<S, L, D> scopeGraph;
    private final SetMultimap.Transient<S, IActorRef<? extends IUnit2UnitProtocol<S, L, D>>> openScopes;
    private final MultiSetMap.Transient<S, EdgeOrData<L>> openEdges;
    private final IRelation3.Transient<S, EdgeOrData<L>, IFuture<Void>> delays;

    public AbstractClient(IActor<? extends IUnit2UnitProtocol<S, L, D>> self,
            @Nullable IActorRef<? extends IUnit2UnitProtocol<S, L, D>> parent, IUnitContext<S, L, D> context, S root,
            Iterable<L> edgeLabels) {
        this.self = self;
        this.parent = parent;
        this.context = context;

        this.root = root;
        this.scopeGraph = ScopeGraph.Transient.of(edgeLabels);
        this.openScopes = SetMultimap.Transient.of();
        this.openEdges = MultiSetMap.Transient.of();
        this.delays = HashTrieRelation3.Transient.of();
    }

    protected S root() {
        return root;
    }

    protected void openScope(S scope, IActorRef<? extends IUnit2UnitProtocol<S, L, D>> unit) {
        openScopes.__insert(scope, unit);
    }

    protected void openEdge(S scope, EdgeOrData<L> edge) {
        openEdges.put(scope, edge);
    }

    ///////////////////////////////////////////////////////////////////////////
    // IUnitProtocol interface, called by IUnit implementations
    ///////////////////////////////////////////////////////////////////////////

    @Override public final void _initRoot(S root, Iterable<L> labels,
            IActorRef<? extends IUnit2UnitProtocol<S, L, D>> unit) {
        assertOpen(root, unit);

        context.grantedInit(unit, root);
        context.waitForClose(unit, root, labels);

        openEdges.putAll(root, stream(labels).map(EdgeOrData::edge).collect(Collectors.toList()));
        openScopes.__remove(root, unit);
        final int remaining = openScopes.get(root).size();
        if(context.owner(root).equals(self)) {
            if(remaining == 0) {
                releaseDelays(root);
            }
        } else {
            if(parent == null) {
                throw new IllegalArgumentException("Not our own scope, and no parent: cannot propagate up.");
            }
            parent.async()._initRoot(root, labels, unit);
        }

    }

    protected final void _setDatum(S scope, D datum, Access access) {
        final EdgeOrData<L> edge = EdgeOrData.data(access);
        assertOpen(scope, edge);
        if(access.equals(Access.EXTERNAL)) {
            assertClosed(scope, EdgeOrData.data(Access.INTERNAL));
        }
        final IActorRef<? extends IUnit2UnitProtocol<S, L, D>> owner = context.owner(scope);
        if(!owner.equals(self)) {
            throw new IllegalArgumentException("Scope " + scope + " is not owned by this actor: cannot set datum.");
        }
        scopeGraph.setDatum(scope, datum);
        final int remaining = openEdges.remove(scope, edge);
        if(remaining == 0) {
            releaseDelays(scope, edge);
        }
    }

    @Override public final void _addEdge(S source, L label, S target) {
        assertOpen(source, EdgeOrData.edge(label));

        scopeGraph.addEdge(source, label, target);
        final IActorRef<? extends IUnit2UnitProtocol<S, L, D>> owner = context.owner(source);
        if(!owner.equals(self)) {
            owner.async()._addEdge(source, label, target);
        }
    }

    @Override public final void _closeEdge(S source, L label) {
        assertOpen(source, EdgeOrData.edge(label));

        final EdgeOrData<L> edge = EdgeOrData.edge(label);
        final int remaining = openEdges.remove(source, edge);
        final IActorRef<? extends IUnit2UnitProtocol<S, L, D>> owner = context.owner(source);
        if(owner.equals(self)) {
            if(remaining == 0) {
                releaseDelays(source, edge);
            }
        } else {
            owner.async()._closeEdge(source, label);
        }
    }

    @Override public final IFuture<Env<S, L, D>> _query(IScopePath<S, L> path, LabelWF<L> labelWF, DataWF<D> dataWF,
            LabelOrder<L> labelOrder, DataLeq<D> dataEquiv) {
        return resolve(path, labelWF, dataWF, labelOrder, dataEquiv);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Resolution & delays
    ///////////////////////////////////////////////////////////////////////////

    private void releaseDelays(S scope) {
        // TODO Implement 
        throw new NotImplementedException();
    }

    private void releaseDelays(S scope, EdgeOrData<L> edge) {
        // TODO Implement 
        throw new NotImplementedException();
    }

    private IFuture<Env<S, L, D>> resolve(IScopePath<S, L> path, LabelWF<L> labelWF, DataWF<D> dataWF,
            LabelOrder<L> labelOrder, DataLeq<D> dataEquiv) {
        final NameResolution<S, L, D> nr =
                new NameResolution<S, L, D>(scopeGraph, labelOrder, dataWF, dataEquiv, this::isComplete) {

                    @Override public Optional<IFuture<Env<S, L, D>>> externalEnv(IScopePath<S, L> path, LabelWF<L> re,
                            LabelOrder<L> labelOrder, DataWF<D> dataWF, DataLeq<D> dataEquiv) {
                        final IActorRef<? extends IUnit2UnitProtocol<S, L, D>> owner = context.owner(path.getTarget());
                        if(owner.equals(self)) {
                            return Optional.empty();
                        } else {
                            final IFuture<Env<S, L, D>> future =
                                    (owner.async()._query(path, labelWF, dataWF, labelOrder, dataEquiv));
                            context.waitForAnswer(owner, future);
                            return Optional.of(future.whenComplete((r, ex) -> {
                                context.grantedAnswer(owner, future);
                            }));
                        }
                    }

                };
        return nr.env(path, labelWF, context.cancel());
    }

    private IFuture<Void> isComplete(S scope, EdgeOrData<L> edge) {
        final CompletableFuture<Void> result = new CompletableFuture<>();
        // FIXME Add waitFor to detect local deadlock?
        if(openScopes.containsKey(scope) || openEdges.contains(scope, edge)) {
            delays.put(scope, edge, result);
        } else {
            result.completeValue(null);
        }
        return result;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Internal helper methods
    ///////////////////////////////////////////////////////////////////////////

    private void assertOpen(S source, IActorRef<? extends IUnit2UnitProtocol<S, L, D>> unit) {
        if(openScopes.containsEntry(source, unit)) {
            throw new IllegalArgumentException("Scope " + source + " must be open.");
        }
    }

    private void assertOpen(S source, EdgeOrData<L> edgeOrDatum) {
        if(!openEdges.contains(source, edgeOrDatum)) {
            throw new IllegalArgumentException("Edge or datum " + source + "/" + edgeOrDatum + " must be open.");
        }
    }

    private void assertClosed(S source, EdgeOrData<L> edgeOrDatum) {
        if(openEdges.contains(source, edgeOrDatum)) {
            throw new IllegalArgumentException("Edge or datum " + source + "/" + edgeOrDatum + " must be closed.");
        }
    }

}