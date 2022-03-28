package mb.p_raffrayi.nameresolution;

import java.util.Optional;

import org.metaborg.util.future.IFuture;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.tuple.Tuple2;

import mb.scopegraph.oopsla20.reference.Env;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

public interface IResolutionContext<S, L, D, M> {

    IFuture<Tuple2<Env<S, L, D>, M>> externalEnv(ScopePath<S, L> path, IQuery<S, L, D> query, ICancel cancel);

    IFuture<Optional<D>> getDatum(S scope);

    IFuture<Iterable<S>> getEdges(S scope, L label);

    IFuture<Tuple2<Boolean, M>> dataWf(D d, ICancel cancel) throws InterruptedException;

    IFuture<Tuple2<Boolean, M>> dataEquiv(D d1, D d2, ICancel cancel) throws InterruptedException;

    IFuture<Boolean> dataEquivAlwaysTrue(ICancel cancel);

    M unitMetadata();

    M compose(M metadata1, M metadata2);

}
