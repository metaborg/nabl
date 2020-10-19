package mb.statix.concurrent.p_raffrayi.impl;

import mb.statix.concurrent.actors.MessageTags;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataLeq;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataWF;
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelOrder;
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelWF;
import mb.statix.scopegraph.path.IScopePath;
import mb.statix.scopegraph.reference.EdgeOrData;
import mb.statix.scopegraph.reference.Env;

/**
 * Protocol accepted by clients, from other clients
 */
public interface IUnit2UnitProtocol<S, L, D, R> {

    @MessageTags("stuckness") void _initShare(S scope, Iterable<EdgeOrData<L>> edges, boolean sharing);

    @MessageTags("stuckness") void _addShare(S scope);

    @MessageTags("stuckness") void _doneSharing(S scope);

    void _addEdge(S source, L label, S target);

    @MessageTags("stuckness") void _closeEdge(S scope, EdgeOrData<L> edge);

    @MessageTags("stuckness") IFuture<Env<S, L, D>> _query(IScopePath<S, L> path, LabelWF<L> labelWF, DataWF<D> dataWF,
            LabelOrder<L> labelOrder, DataLeq<D> dataEquiv);

}