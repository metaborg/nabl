package mb.p_raffrayi.nameresolution;

import java.util.Optional;

import org.metaborg.util.future.IFuture;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.tuple.Tuple2;

import io.usethesource.capsule.Set;
import mb.scopegraph.ecoop21.INameResolutionContext;
import mb.scopegraph.ecoop21.LabelOrder;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.ecoop21.NameResolution;
import mb.scopegraph.oopsla20.reference.Env;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

public class NameResolutionQuery<S, L, D> implements IQuery<S, L, D> {

    private final LabelWf<L> labelWf;
    private final LabelOrder<L> labelOrder;

    private final Set.Immutable<L> edgeLabels;

    public NameResolutionQuery(LabelWf<L> labelWf, LabelOrder<L> labelOrder, Set.Immutable<L> edgeLabels) {
        this.labelWf = labelWf;
        this.labelOrder = labelOrder;
        this.edgeLabels = edgeLabels;
    }

    @Override public <M> IFuture<Tuple2<Env<S, L, D>, M>> resolve(IResolutionContext<S, L, D, M> context, ScopePath<S, L> path, ICancel cancel) {
        final INameResolutionContext<S, L, D, M> nrContext = new INameResolutionContext<S, L, D, M>() {

            @Override public IFuture<Tuple2<Env<S, L, D>, M>> externalEnv(ScopePath<S, L> path, LabelWf<L> re,
                    LabelOrder<L> labelOrder, ICancel cancel) {
                return context.externalEnv(path, new NameResolutionQuery<>(re, labelOrder, edgeLabels), cancel);
            }

            @Override public IFuture<Optional<D>> getDatum(S scope) {
                return context.getDatum(scope);
            }

            @Override public IFuture<Iterable<S>> getEdges(S scope, L label) {
                return context.getEdges(scope, label);
            }

            @Override public IFuture<Tuple2<Boolean, M>> dataWf(D datum, ICancel cancel) throws InterruptedException {
                return context.dataWf(datum, cancel);
            }

            @Override public IFuture<Tuple2<Boolean, M>> dataLeq(D d1, D d2, ICancel cancel) throws InterruptedException {
                return context.dataEquiv(d1, d2, cancel);
            }

            @Override public IFuture<Boolean> dataLeqAlwaysTrue(ICancel cancel) {
                return context.dataEquivAlwaysTrue(cancel);
            }

            @Override public M unitMetadata() {
                return context.unitMetadata();
            }

            @Override public M compose(M metadata1, M metadata2) {
                return context.compose(metadata1, metadata2);
            }

        };

        final NameResolution<S, L, D, M> nr = new NameResolution<>(edgeLabels, labelOrder, nrContext);
        return nr.env(path, labelWf, cancel);
    }

    @Override public LabelWf<L> labelWf() {
        return labelWf;
    }

}
