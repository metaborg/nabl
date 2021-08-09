package mb.p_raffrayi.impl.envdiff;

import org.immutables.value.Value;
import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.IRelation3;

import io.usethesource.capsule.Set;
import mb.scopegraph.oopsla20.terms.newPath.ResolutionPath;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

@Value.Immutable
public abstract class ADiffTree<S, L, D> implements IEnvDiff<S, L, D> {

    @Value.Parameter public abstract S scope();

    @Value.Parameter public abstract IRelation3.Immutable<L, S, IEnvDiff<S, L, D>> edges();

    @Override @Value.Lazy public boolean isEmpty() {
        return edges().valueSet().stream().allMatch(IEnvDiff::isEmpty);
    }

    @Override @Value.Lazy public Set.Immutable<ResolutionPath<S, L, IEnvDiff<S, L, D>>> diffPaths() {
        return diffPaths(new ScopePath<S, L>(scope()));
    }

    @Override public Set.Immutable<ResolutionPath<S, L, IEnvDiff<S, L, D>>> diffPaths(ScopePath<S, L> prefix) {
        final Set.Transient<ResolutionPath<S, L, IEnvDiff<S, L, D>>> _paths = CapsuleUtil.transientSet();
        edges().stream().forEach(edge -> {
            prefix.step(edge._1(), edge._2()).ifPresent(path -> {
                if(!edge._3().isEmpty()) {
                    _paths.__insertAll(edge._3().diffPaths(path));
                }
            });
        });
        return _paths.freeze();
    }

}
