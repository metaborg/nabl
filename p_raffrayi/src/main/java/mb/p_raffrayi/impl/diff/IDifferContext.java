package mb.p_raffrayi.impl.diff;

import java.util.Optional;

import org.metaborg.util.future.IFuture;

import io.usethesource.capsule.Set;

public interface IDifferContext<S, L, D> {

    IFuture<Iterable<S>> getEdges(S scope, L label);

    IFuture<Set.Immutable<L>> labels(S scope);

    IFuture<Optional<D>> datum(S scope);

    boolean available(S scope);
}
