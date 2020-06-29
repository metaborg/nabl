package mb.statix.solver.concurrent2.impl;

import java.util.Set;

import mb.statix.actors.IActorRef;
import mb.statix.actors.IFuture;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;

/**
 * Protocol accepted by clients, from other clients
 */
public interface IUnitProtocol<S, L, D> {

    void _start(S root);

    void _initRoot(S root, Iterable<L> labels, IActorRef<? extends IUnitProtocol<S, L, D>> unit);

    void _addEdge(S source, L label, S target);

    void _closeEdge(S source, L label);

    IFuture<Set<IResolutionPath<S, L, D>>> _query(S scope, LabelWF<L> labelWF, DataWF<D> dataWF,
            LabelOrder<L> labelOrder, DataLeq<D> dataEquiv);
}