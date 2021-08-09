package mb.p_raffrayi.impl.envdiff;

import io.usethesource.capsule.Set;
import mb.scopegraph.oopsla20.terms.newPath.ResolutionPath;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

public interface IEnvDiff<S, L, D> {

    boolean isEmpty();

    Set.Immutable<ResolutionPath<S, L, IEnvDiff<S, L, D>>> diffPaths();

    Set.Immutable<ResolutionPath<S, L, IEnvDiff<S, L, D>>> diffPaths(ScopePath<S, L> prefix);

    // TODO match

}
