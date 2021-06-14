package mb.p_raffrayi.impl;

import java.util.Optional;

import org.metaborg.util.future.IFuture;

import mb.p_raffrayi.nameresolution.DataLeq;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelOrder;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.scopegraph.oopsla20.reference.Env;
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

    IFuture<IQueryAnswer<S, L, D>> _query(ScopePath<S, L> path, LabelWf<L> labelWF, DataWf<S, L, D> dataWF,
            LabelOrder<L> labelOrder, DataLeq<S, L, D> dataEquiv);

    default IFuture<Env<S, L, D>> _confirm(ScopePath<S, L> path, LabelWf<L> labelWF, DataWf<S, L, D> dataWF,
            LabelOrder<L> labelOrder, DataLeq<S, L, D> dataEquiv) {
        return _query(path, labelWF, dataWF, labelOrder, dataEquiv).thenApply(IQueryAnswer::env);
    }

    IFuture<org.metaborg.util.unit.Unit> _isComplete(S scope, EdgeOrData<L> label);

    IFuture<Optional<D>> _datum(S scope);

    IFuture<Optional<S>> _match(S previousScope);

}