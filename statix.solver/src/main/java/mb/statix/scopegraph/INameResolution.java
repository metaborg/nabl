package mb.statix.scopegraph;

import org.metaborg.util.functions.Predicate2;
import org.metaborg.util.task.ICancel;

import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.Env;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ResolutionException;

public interface INameResolution<S extends D, L, D> {

    Env<S, L, D> resolve(S scope, ICancel cancel) throws ResolutionException, InterruptedException;

    interface Builder<S extends D, L, D> {

        Builder<S, L, D> withLabelWF(LabelWF<L> labelWF);

        Builder<S, L, D> withLabelOrder(LabelOrder<L> labelOrder);

        Builder<S, L, D> withEdgeComplete(Predicate2<S, L> isEdgeComplete);

        Builder<S, L, D> withDataWF(DataWF<D> dataWF);

        Builder<S, L, D> withDataEquiv(DataLeq<D> dataEquiv);

        Builder<S, L, D> withDataComplete(Predicate2<S, L> isDataComplete);

        INameResolution<S, L, D> build(IScopeGraph<S, L, D> scopeGraph, L relation);

    }

}
