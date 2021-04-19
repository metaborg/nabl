package mb.p_raffrayi.impl;

import java.util.List;
import java.util.Set;

import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.unit.Unit;

import mb.p_raffrayi.IUnitResult;
import mb.p_raffrayi.actors.IActor;
import mb.p_raffrayi.actors.IActorRef;
import mb.p_raffrayi.nameresolution.DataLeq;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.EdgeOrEps;
import mb.scopegraph.ecoop21.LabelOrder;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.oopsla20.IScopeGraph.Immutable;
import mb.scopegraph.oopsla20.reference.Env;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

class ScopeGraphLibraryWorker<S, L, D> extends AbstractUnit<S, L, D, Unit> {

    ScopeGraphLibraryWorker(IActor<? extends IUnit<S, L, D, Unit>> self, IActorRef<? extends IUnit<S, L, D, ?>> parent,
            IUnitContext<S, L, D> context, Iterable<L> edgeLabels, Set<S> scopes,
            Immutable<S, EdgeOrEps<L>, D> scopeGraph) {
        super(self, parent, context, edgeLabels);

        this.scopes.__insertAll(scopes);
        this.scopeGraph.set(scopeGraph);
    }

    @Override protected IFuture<D> getExternalDatum(D datum) {
        return CompletableFuture.completedFuture(datum);
    }

    ///////////////////////////////////////////////////////////////////////////
    // IBroker2UnitProtocol interface, called by IBroker implementations
    ///////////////////////////////////////////////////////////////////////////

    @Override public IFuture<IUnitResult<S, L, D, Unit>> _start(List<S> rootScopes) {
        doStart(rootScopes);
        return doFinish(CompletableFuture.completedFuture(Unit.unit));
    }

    @SuppressWarnings("unused") @Override public void _initShare(S scope, S childRep) {
        throw new UnsupportedOperationException("Not supported by static scope graph units.");
    }

    @Override public IFuture<Env<S, L, D>> _query(S scope, ScopePath<S, L> path, LabelWf<L> labelWF,
            DataWf<S, L, D> dataWF, LabelOrder<L> labelOrder, DataLeq<S, L, D> dataEquiv) {
        // duplicate of AbstractUnit::_query
        // resume(); // FIXME necessary?
        stats.incomingQueries += 1;
        return doQuery(self.sender(TYPE), scope, path, labelWF, labelOrder, dataWF, dataEquiv, null, null);
    }

    @Override protected boolean canAnswer(S scope) {
        final IActorRef<? extends IUnit<S, L, D, ?>> owner = context.owner(scope);
        return owner.equals(parent);
    }

}