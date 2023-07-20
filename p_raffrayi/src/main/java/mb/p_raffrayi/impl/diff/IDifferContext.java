package mb.p_raffrayi.impl.diff;

import java.util.Collection;
import java.util.Optional;

import org.metaborg.util.future.IFuture;

public interface IDifferContext<S, L, D> {

    IFuture<Collection<S>> getEdges(S scope, L label);

    IFuture<Optional<D>> datum(S scope);

    Optional<D> rawDatum(S scope);

    boolean available(S scope);
}
