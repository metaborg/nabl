package mb.statix.solver.concurrent2.impl;

import static com.google.common.collect.Streams.stream;

import java.util.Set;
import java.util.stream.Collectors;

import org.spoofax.terms.util.NotImplementedException;

import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation3;
import mb.nabl2.util.collections.MultiSet;
import mb.nabl2.util.collections.MultiSetMap;
import mb.statix.actors.IActor;
import mb.statix.actors.IActorRef;
import mb.statix.actors.IFuture;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.reference.Access;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.EdgeOrData;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ScopeGraph;
import mb.statix.solver.concurrent2.ITypeChecker;
import mb.statix.solver.concurrent2.IUnit;

public class UnitActor<S, L, D> implements IUnit<S, L, D> {

    private final IActor<IUnit<S, L, D>> self;
    private final IActorRef<IUnitProtocol<S, L, D>> parent;
    private final IUnitContext<S, L, D> context;
    private final ITypeChecker<S, L, D, ?> unitChecker;

    private final IScopeGraph.Transient<S, L, D> scopeGraph;
    private final MultiSet.Transient<S> openScopes;
    private final MultiSetMap.Transient<S, EdgeOrData<L>> openEdges;
    private final MultiSet.Transient<String> scopeNameCounters;
    private final IRelation3.Transient<S, EdgeOrData<L>, IFuture<Set<IResolutionPath<S, L, D>>>> delays;

    public UnitActor(IActor<IUnit<S, L, D>> self, IActorRef<IUnitProtocol<S, L, D>> parent,
            IUnitContext<S, L, D> context, ITypeChecker<S, L, D, ?> unitChecker, Iterable<L> edgeLabels) {
        this.self = self;
        this.parent = parent;
        this.context = context;
        this.unitChecker = unitChecker;

        this.scopeGraph = ScopeGraph.Transient.of(edgeLabels);
        this.openScopes = MultiSet.Transient.of();
        this.openEdges = MultiSetMap.Transient.of();
        this.scopeNameCounters = MultiSet.Transient.of();
        this.delays = HashTrieRelation3.Transient.of();
    }

    ///////////////////////////////////////////////////////////////////////////
    // IClientProtocol interface, called by ITypeChecker implementations
    ///////////////////////////////////////////////////////////////////////////

    @Override public void initRoot(S root, Iterable<L> labels) {
        assertOpen(root);
        _initRoot(root, labels, self);
    }

    @Override public S freshScope(String baseName, Iterable<L> labels, Iterable<Access> data) {
        final String name = baseName.replace("-", "_");
        final int n = scopeNameCounters.add(name);
        final S scope = context.makeScope(name + "-" + n);

        openEdges.putAll(scope, stream(labels).map(EdgeOrData::edge).collect(Collectors.toSet()));
        openEdges.putAll(scope, stream(data).map(EdgeOrData::<L>data).collect(Collectors.toSet()));

        return scope;
    }

    @Override public void setDatum(S scope, D datum, Access access) {
        final EdgeOrData<L> edge = EdgeOrData.data(access);
        assertOpen(scope, edge);
        if(access.equals(Access.EXTERNAL)) {
            assertClosed(scope, EdgeOrData.data(Access.INTERNAL));
        }
        final IActorRef<? extends IUnitProtocol<S, L, D>> owner = context.owner(scope);
        if(!owner.equals(self)) {
            throw new IllegalArgumentException("Scope " + scope + " is not owned by this actor: cannot set datum.");
        }
        scopeGraph.setDatum(scope, datum);
        final int remaining = openEdges.remove(scope, edge);
        if(remaining == 0) {
            releaseDelays(scope, edge);
        }
    }

    @Override public void addEdge(S source, L label, S target) {
        assertOpen(source, EdgeOrData.edge(label));
        _addEdge(source, label, target);
    }

    @Override public void closeEdge(S source, L label) {
        assertOpen(source, EdgeOrData.edge(label));
        _closeEdge(source, label);
    }

    @Override public IFuture<Set<IResolutionPath<S, L, D>>> query(S scope, LabelWF<L> labelWF, DataWF<D> dataWF,
            LabelOrder<L> labelOrder, DataLeq<D> dataEquiv) {
        final IFuture<Set<IResolutionPath<S, L, D>>> future = _query(scope, labelWF, dataWF, labelOrder, dataEquiv);
        final IActorRef<? extends IUnitProtocol<S, L, D>> owner = context.owner(scope);
        context.waitForAnswer(owner, future);
        return future.whenComplete((r, ex) -> {
            context.grantedAnswer(owner, future);
        });
    }

    ///////////////////////////////////////////////////////////////////////////
    // IUnitProtocol interface, called by IUnit implementations
    ///////////////////////////////////////////////////////////////////////////

    @Override public void _start(S root) {
        try {
            context.waitForInit(self, root);
            openScopes.add(root);
            this.unitChecker.run(this, root);
        } catch(InterruptedException e) {
            // FIXME Handle this
            e.printStackTrace();
        }
    }

    @Override public void _initRoot(S root, Iterable<L> labels, IActorRef<? extends IUnitProtocol<S, L, D>> unit) {
        context.grantedInit(unit, root);
        context.waitForClose(unit, root, labels);

        openEdges.putAll(root, stream(labels).map(EdgeOrData::edge).collect(Collectors.toList()));
        final int remaining = openScopes.remove(root);
        if(context.owner(root).equals(self)) {
            if(remaining == 0) {
                releaseDelays(root);
            }
        } else {
            parent.async()._initRoot(root, labels, unit);
        }

    }

    @Override public void _addEdge(S source, L label, S target) {
        scopeGraph.addEdge(source, label, target);
        final IActorRef<? extends IUnitProtocol<S, L, D>> owner = context.owner(source);
        if(!owner.equals(self)) {
            owner.async()._addEdge(source, label, target);
        }
    }

    @Override public void _closeEdge(S source, L label) {
        final EdgeOrData<L> edge = EdgeOrData.edge(label);
        final int remaining = openEdges.remove(source, edge);
        final IActorRef<? extends IUnitProtocol<S, L, D>> owner = context.owner(source);
        if(owner.equals(self)) {
            if(remaining == 0) {
                releaseDelays(source, edge);
            }
        } else {
            owner.async()._closeEdge(source, label);
        }
    }

    @Override public IFuture<Set<IResolutionPath<S, L, D>>> _query(S scope, LabelWF<L> labelWF, DataWF<D> dataWF,
            LabelOrder<L> labelOrder, DataLeq<D> dataEquiv) {
        final IActorRef<? extends IUnitProtocol<S, L, D>> owner = context.owner(scope);
        if(owner.equals(self)) {
            return resolve(scope, labelWF, dataWF, labelOrder, dataEquiv);
        } else {
            IFuture<Set<IResolutionPath<S, L, D>>> future =
                    owner.async()._query(scope, labelWF, dataWF, labelOrder, dataEquiv);
            context.waitForAnswer(owner, future);
            return future.whenComplete((r, ex) -> {
                context.grantedAnswer(owner, future);
            });
        }


    }

    ///////////////////////////////////////////////////////////////////////////
    // Deadlock
    ///////////////////////////////////////////////////////////////////////////

    @Override public void deadlocked() {
        // FIXME Handle this
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

    private IFuture<Set<IResolutionPath<S, L, D>>> resolve(S scope, LabelWF<L> labelWF, DataWF<D> dataWF,
            LabelOrder<L> labelOrder, DataLeq<D> dataEquiv) {
        // TODO Implement 
        throw new NotImplementedException();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Internal helper methods
    ///////////////////////////////////////////////////////////////////////////

    private void assertOpen(S source) {
        if(openScopes.contains(source)) {
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