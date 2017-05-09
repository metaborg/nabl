package org.metaborg.meta.nabl2.scopegraph.esop;

import java.util.Optional;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.INameResolution;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IResolutionParameters;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.IScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.OpenCounter;
import org.metaborg.meta.nabl2.scopegraph.path.IDeclPath;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;

import com.google.common.annotations.Beta;

import io.usethesource.capsule.Set;

@Beta
public interface IEsopNameResolution<S extends IScope, L extends ILabel, O extends IOccurrence>
		extends INameResolution<S, L, O> {

	static <S extends IScope, L extends ILabel, O extends IOccurrence> IEsopNameResolution<S, L, O> resolveFrom(
			IScopeGraph<S, L, O> scopeGraph, IResolutionParameters<L> params, OpenCounter<S, L> scopeCounter) {
		return new org.metaborg.meta.nabl2.scopegraph.esop.reference.EsopNameResolution<>(scopeGraph, params, scopeCounter);
	}

	Optional<Set.Immutable<IResolutionPath<S, L, O>>> tryResolve(O ref);

	Optional<Set.Immutable<IDeclPath<S, L, O>>> tryVisible(S scope);

	Optional<Set.Immutable<IDeclPath<S, L, O>>> tryReachable(S scope);

}
