package org.metaborg.meta.nabl2.scopegraph.esop;

import java.util.Map;

import org.metaborg.meta.nabl2.regexp.IRegExpMatcher;
import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.path.IPath;
import org.metaborg.meta.nabl2.scopegraph.path.IScopePath;
import org.pcollections.PSet;

interface EnvL<S extends IScope, L extends ILabel, O extends IOccurrence> {

    <P extends IPath<S, L, O>> IEsopEnv<S, L, O, P> apply(PSet<O> seenI, IRegExpMatcher<L> re, IScopePath<S, L, O> path,
            IEsopEnv.Filter<S, L, O, P> filter, Map<L, IEsopEnv<S, L, O, P>> env_lCache);

}