package mb.p_raffrayi.nameresolution;

import java.util.Optional;

import org.metaborg.util.future.IFuture;
import org.metaborg.util.task.ICancel;

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

    @Override public IFuture<Env<S, L, D>> resolve(IResolutionContext<S, L, D> context, ScopePath<S, L> path, ICancel cancel) {
        final INameResolutionContext<S, L, D> nrContext = new INameResolutionContext<S, L, D>() {

            @Override public IFuture<Env<S, L, D>> externalEnv(ScopePath<S, L> path, LabelWf<L> re,
                    LabelOrder<L> labelOrder, ICancel cancel) {
                return context.externalEnv(path, new NameResolutionQuery<>(re, labelOrder, edgeLabels), cancel);
            }

            @Override public IFuture<Optional<D>> getDatum(S scope) {
                return context.getDatum(scope);
            }

            @Override public IFuture<Iterable<S>> getEdges(S scope, L label) {
                return context.getEdges(scope, label);
            }

            @Override public IFuture<Boolean> dataWf(D datum, ICancel cancel) throws InterruptedException {
                return context.dataWf(datum, cancel);
            }

            @Override public IFuture<Boolean> dataLeq(D d1, D d2, ICancel cancel) throws InterruptedException {
                return context.dataEquiv(d1, d2, cancel);
            }

            @Override public IFuture<Boolean> dataLeqAlwaysTrue(ICancel cancel) {
                return context.dataEquivAlwaysTrue(cancel);
            }

        };

        final NameResolution<S, L, D> nr = new NameResolution<>(edgeLabels, labelOrder, nrContext);
        return nr.env(path, labelWf, cancel);
    }

    @Override public LabelWf<L> labelWf() {
        return labelWf;
    }

}
