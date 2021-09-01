package mb.p_raffrayi.impl.envdiff;

import java.util.Optional;

import org.metaborg.util.future.IFuture;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.Set.Immutable;
import mb.p_raffrayi.impl.diff.IScopeGraphDiffer;
import mb.p_raffrayi.impl.diff.ScopeDiff;

public class DifferBasedContext<S, L, D> implements IEnvDifferContext<S, L, D> {

    private final IScopeGraphDiffer<S, L, D> differ;
    private final Set.Immutable<L> edgeLabels;

    public DifferBasedContext(IScopeGraphDiffer<S, L, D> differ, Set.Immutable<L> edgeLabels) {
        this.edgeLabels = edgeLabels;
        this.differ = differ;
    }


    @Override public IFuture<ScopeDiff<S, L, D>> scopeDiff(S previousScope, L label) {
        return differ.scopeDiff(previousScope, label);
    }

    @Override public IFuture<Optional<S>> match(S previousScope) {
        return differ.match(previousScope);
    }

    @Override public Immutable<L> edgeLabels() {
        return edgeLabels;
    }

}
