package mb.scopegraph.oopsla20;

import java.util.Set;

import org.metaborg.util.functions.Predicate2;
import org.metaborg.util.task.ICancel;

import mb.scopegraph.oopsla20.reference.DataLeq;
import mb.scopegraph.oopsla20.reference.DataWF;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.scopegraph.oopsla20.reference.Env;
import mb.scopegraph.oopsla20.reference.LabelOrder;
import mb.scopegraph.oopsla20.reference.LabelWF;
import mb.scopegraph.oopsla20.reference.ResolutionException;

public interface INameResolution<S, L, D> {

    Env<S, L, D> resolve(S scope, ICancel cancel) throws ResolutionException, InterruptedException;

    interface Builder<S, L, D> {

        Builder<S, L, D> withLabelWF(LabelWF<L> labelWF);

        Builder<S, L, D> withLabelOrder(LabelOrder<L> labelOrder);

        Builder<S, L, D> withDataWF(DataWF<D> dataWF);

        Builder<S, L, D> withDataEquiv(DataLeq<D> dataEquiv);

        Builder<S, L, D> withIsComplete(Predicate2<S, EdgeOrData<L>> isComplete);

        INameResolution<S, L, D> build(IScopeGraph<S, L, D> scopeGraph, Set<L> edgeLabels);

    }

}
