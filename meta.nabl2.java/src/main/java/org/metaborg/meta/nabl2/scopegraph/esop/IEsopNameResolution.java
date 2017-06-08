package org.metaborg.meta.nabl2.scopegraph.esop;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.INameResolution;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.util.functions.Predicate2;

import com.google.common.annotations.Beta;
import com.google.common.collect.SetMultimap;

@Beta
public interface IEsopNameResolution<S extends IScope, L extends ILabel, O extends IOccurrence>
        extends INameResolution<S, L, O> {

    interface Immutable<S extends IScope, L extends ILabel, O extends IOccurrence>
            extends IEsopNameResolution<S, L, O>, INameResolution.Immutable<S, L, O> {

        IEsopNameResolution.Transient<S, L, O> melt(IEsopScopeGraph<S, L, O, ?> scopeGraph,
                Predicate2<S, L> isEdgeClosed);

    }

    interface Transient<S extends IScope, L extends ILabel, O extends IOccurrence>
            extends IEsopNameResolution<S, L, O>, INameResolution.Transient<S, L, O> {

        boolean addAll(IEsopNameResolution<S, L, O> other);

        Update<S, L, O> resolve();

        IEsopNameResolution.Immutable<S, L, O> freeze();

    }

    interface Update<S extends IScope, L extends ILabel, O extends IOccurrence> {

        SetMultimap<O, IResolutionPath<S, L, O>> resolved();

        SetMultimap<S, O> visible();

        SetMultimap<S, O> reachable();

        default boolean isEmpty() {
            return resolved().isEmpty() && visible().isEmpty() && reachable().isEmpty();
        }

    }

}