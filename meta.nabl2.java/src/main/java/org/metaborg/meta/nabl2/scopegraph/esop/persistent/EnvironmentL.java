package org.metaborg.meta.nabl2.scopegraph.esop.persistent;

import java.util.Map;

import org.metaborg.meta.nabl2.regexp.IRegExpMatcher;
import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.path.IPath;
import org.metaborg.meta.nabl2.scopegraph.path.IScopePath;

import io.usethesource.capsule.Set;

interface EnvironmentL<S extends IScope, L extends ILabel, O extends IOccurrence> {

    <P extends IPath<S, L, O>> IPersistentEnvironment<S, L, O, P> apply(Set.Immutable<O> seenI, IRegExpMatcher<L> re,
            IScopePath<S, L, O> path, IPersistentEnvironment.Filter<S, L, O, P> filter,
            Map<L, IPersistentEnvironment<S, L, O, P>> env_lCache);

}