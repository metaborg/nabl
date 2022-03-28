package mb.p_raffrayi.impl;

import java.util.Optional;

import org.metaborg.util.future.IFuture;

import mb.p_raffrayi.actors.IActorRef;
import mb.p_raffrayi.impl.confirm.ConfirmResult;
import mb.p_raffrayi.nameresolution.DataLeq;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.p_raffrayi.nameresolution.IQuery;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

/**
 * Protocol accepted by clients, from other clients
 */
public interface IUnit2UnitProtocol<S, L, D> {

    void _initShare(S scope, Iterable<EdgeOrData<L>> edges, boolean sharing);

    void _addShare(S scope);

    void _doneSharing(S scope);

    void _addEdge(S source, L label, S target);

    void _closeEdge(S scope, EdgeOrData<L> edge);

    IFuture<IQueryAnswer<S, L, D>> _query(IActorRef<? extends IUnit<S, L, D, ?>> origin, ScopePath<S, L> path,
            IQuery<S, L, D> query, DataWf<S, L, D> dataWF, DataLeq<S, L, D> dataEquiv);

    IFuture<IQueryAnswer<S, L, D>> _queryPrevious(ScopePath<S, L> path, IQuery<S, L, D> query, DataWf<S, L, D> dataWF,
            DataLeq<S, L, D> dataEquiv);

    IFuture<ConfirmResult<S, L, D>> _confirm(ScopePath<S, L> path, LabelWf<L> labelWF, DataWf<S, L, D> dataWF,
            boolean prevEnvEmpty);

    IFuture<Optional<S>> _match(S previousScope);

}
