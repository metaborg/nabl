package mb.p_raffrayi.impl.envdiff;

import org.metaborg.util.functions.Function1;

import io.usethesource.capsule.Set;
import mb.scopegraph.oopsla20.terms.newPath.ResolutionPath;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

public interface IEnvDiff<S, L, D> {

    boolean isEmpty();

    Set.Immutable<ResolutionPath<S, L, IEnvDiff<S, L, D>>> diffPaths();

    Set.Immutable<ResolutionPath<S, L, IEnvDiff<S, L, D>>> diffPaths(ScopePath<S, L> prefix);

    <T> T match(Function1<AddedEdge<S, L, D>, T> onAddedEdge, Function1<RemovedEdge<S, L, D>, T> onRemovedEdge,
            Function1<External<S, L, D>, T> onExternal, Function1<DiffTree<S, L, D>, T> onDiffTree);

}
