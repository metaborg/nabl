package org.metaborg.meta.nabl2.scopegraph.esop;

import org.metaborg.meta.nabl2.regexp.IRegExpMatcher;
import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.path.IScopePath;
import org.metaborg.meta.nabl2.util.functions.Function3;
import org.pcollections.PSet;

interface EnvL<S extends IScope, L extends ILabel, O extends IOccurrence>
    extends Function3<PSet<O>, IRegExpMatcher<L>, IScopePath<S, L, O>, EsopEnv<S, L, O>> {

}