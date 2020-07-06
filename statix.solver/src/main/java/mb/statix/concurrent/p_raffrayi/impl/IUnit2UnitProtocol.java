package mb.statix.concurrent.p_raffrayi.impl;

import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.scopegraph.path.IScopePath;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.Env;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;

/**
 * Protocol accepted by clients, from other clients
 */
public interface IUnit2UnitProtocol<S, L, D> {

    void _initRoot(S root, Iterable<L> labels, boolean shared);

    void _addEdge(S source, L label, S target);

    void _closeEdge(S source, L label);

    void _closeShare(S scope);

    IFuture<Env<S, L, D>> _query(IScopePath<S, L> path, LabelWF<L> labelWF, DataWF<D> dataWF, LabelOrder<L> labelOrder,
            DataLeq<D> dataEquiv);

}