package mb.p_raffrayi.nameresolution;

import java.util.Optional;

import org.metaborg.util.future.IFuture;
import org.metaborg.util.task.ICancel;

import mb.scopegraph.oopsla20.reference.Env;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

public interface IResolutionContext<S, L, D> {

    IFuture<Optional<D>> getDatum(S scope);

    IFuture<Iterable<S>> getEdges(S scope, L label);

    IFuture<Boolean> dataWf(D d, ICancel cancel) throws InterruptedException;

    IFuture<Boolean> dataEquiv(D d1, D d2, ICancel cancel) throws InterruptedException;

    IFuture<Boolean> dataEquivAlwaysTrue(ICancel cancel);

    IFuture<Env<S, L, D>> externalEnv(ScopePath<S, L> path, IQuery<S, L, D> query, ICancel cancel);

}
