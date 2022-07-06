package mb.p_raffrayi.impl.diff;

import java.util.Optional;

import org.metaborg.util.future.IFuture;

public interface IDifferContext<S, L, D> {

    IFuture<Iterable<S>> getEdges(S scope, L label);

    IFuture<Optional<D>> datum(S scope);

    Optional<D> rawDatum(S scope);

    boolean available(S scope);
}
