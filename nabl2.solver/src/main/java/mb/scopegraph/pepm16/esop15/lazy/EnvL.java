package mb.scopegraph.pepm16.esop15.lazy;

import java.util.Map;

import io.usethesource.capsule.Set;
import mb.scopegraph.pepm16.ILabel;
import mb.scopegraph.pepm16.IOccurrence;
import mb.scopegraph.pepm16.IScope;
import mb.scopegraph.pepm16.path.IPath;
import mb.scopegraph.pepm16.path.IScopePath;
import mb.scopegraph.regexp.IRegExpMatcher;

interface EnvL<S extends IScope, L extends ILabel, O extends IOccurrence> {

    <P extends IPath<S, L, O>> IEsopEnv<S, L, O, P> apply(Set.Immutable<O> seenI, IRegExpMatcher<L> re,
            IScopePath<S, L, O> path, IEsopEnv.Filter<S, L, O, P> filter, Map<L, IEsopEnv<S, L, O, P>> env_lCache);

}