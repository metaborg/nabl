package mb.nabl2.scopegraph.esop;

import java.util.Set;

import org.metaborg.util.iterators.Iterables2;

import com.google.common.annotations.Beta;

import io.usethesource.capsule.Map;
import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.INameResolution;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IScope;
import mb.nabl2.scopegraph.path.IResolutionPath;

@Beta
public interface IEsopNameResolution<S extends IScope, L extends ILabel, O extends IOccurrence>
        extends INameResolution<S, L, O> {

    boolean addAll(ResolutionCache<S, L, O> cache);

    void resolveAll();

    default void resolveAll(Iterable<? extends O> refs) {
        Iterables2.stream(refs).forEach(this::resolve);
    }

    ResolutionCache<S, L, O> toCache();

    interface ResolutionCache<S extends IScope, L extends ILabel, O extends IOccurrence> {

        Map.Immutable<O, Set<IResolutionPath<S, L, O>>> resolutionEntries();

        Map.Immutable<S, Set<O>> visibilityEntries();

        Map.Immutable<S, Set<O>> reachabilityEntries();

    }

}