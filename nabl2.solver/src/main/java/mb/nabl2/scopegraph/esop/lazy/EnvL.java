package mb.nabl2.scopegraph.esop.lazy;

import java.util.Map;

import io.usethesource.capsule.Set;
import mb.nabl2.regexp.IRegExpMatcher;
import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IScope;
import mb.nabl2.scopegraph.path.IPath;
import mb.nabl2.scopegraph.path.IScopePath;

interface EnvL<S extends IScope, L extends ILabel, O extends IOccurrence> {

    <P extends IPath<S, L, O>> IEsopEnv<S, L, O, P> apply(Set.Immutable<O> seenI, IRegExpMatcher<L> re,
            IScopePath<S, L, O> path, IEsopEnv.Filter<S, L, O, P> filter, Map<L, IEsopEnv<S, L, O, P>> env_lCache);

}