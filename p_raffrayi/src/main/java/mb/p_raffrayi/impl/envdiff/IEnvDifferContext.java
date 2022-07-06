package mb.p_raffrayi.impl.envdiff;

import java.util.Optional;

import org.metaborg.util.future.IFuture;

import io.usethesource.capsule.Set;
import mb.p_raffrayi.impl.diff.ScopeDiff;

public interface IEnvDifferContext<S, L, D> {

    IFuture<ScopeDiff<S, L, D>> scopeDiff(S previousScope, L label);

    IFuture<Optional<S>> match(S previousScope);

    Set.Immutable<L> edgeLabels();

}
