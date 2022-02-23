package mb.scopegraph.ecoop21;

import java.util.Optional;

import org.metaborg.util.future.IFuture;
import org.metaborg.util.task.ICancel;

import mb.scopegraph.oopsla20.reference.Env;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;


public interface INameResolutionContext<S, L, D> {

    IFuture<Env<S, L, D>> externalEnv(ScopePath<S, L> path, LabelWf<L> re, LabelOrder<L> labelOrder, ICancel cancel);

    IFuture<Optional<D>> getDatum(S scope);

    IFuture<Iterable<S>> getEdges(S scope, L label);

    IFuture<Boolean> dataWf(D datum, ICancel cancel) throws InterruptedException;

    IFuture<Boolean> dataLeq(D d1, D d2, ICancel cancel) throws InterruptedException;

    IFuture<Boolean> dataLeqAlwaysTrue(ICancel cancel);

}
