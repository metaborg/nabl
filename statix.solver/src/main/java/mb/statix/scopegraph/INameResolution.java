package mb.statix.scopegraph;

import org.metaborg.util.functions.Predicate2;

import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.EdgeOrData;
import mb.statix.scopegraph.reference.Env;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ResolutionException;

public interface INameResolution<S, L, D> {

    Env<S, L, D> resolve(S scope) throws ResolutionException, InterruptedException;

    interface Builder<S, L, D> {

        Builder<S, L, D> withLabelWF(LabelWF<L> labelWF);

        Builder<S, L, D> withLabelOrder(LabelOrder<L> labelOrder);

        Builder<S, L, D> withDataWF(DataWF<D> dataWF);

        Builder<S, L, D> withDataEquiv(DataLeq<D> dataEquiv);

        Builder<S, L, D> withIsComplete(Predicate2<S, EdgeOrData<L>> isComplete);

        INameResolution<S, L, D> build(IScopeGraph<S, L, D> scopeGraph);

    }

}
