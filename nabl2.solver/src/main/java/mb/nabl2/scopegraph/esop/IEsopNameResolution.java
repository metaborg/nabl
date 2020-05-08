package mb.nabl2.scopegraph.esop;

import java.util.Collection;

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

    boolean addCached(ResolutionCache<S, L, O> cache);

    ResolutionCache<S, L, O> toCache();

    interface ResolutionCache<S extends IScope, L extends ILabel, O extends IOccurrence> {

        Map.Immutable<O, Collection<IResolutionPath<S, L, O>>> resolutionEntries();

        Map.Immutable<S, Collection<O>> visibilityEntries();

        Map.Immutable<S, Collection<O>> reachabilityEntries();

    }

}