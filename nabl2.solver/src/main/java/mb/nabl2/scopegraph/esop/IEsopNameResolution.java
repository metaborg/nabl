package mb.nabl2.scopegraph.esop;

import com.google.common.annotations.Beta;

import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.INameResolution;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IScope;

@Beta
public interface IEsopNameResolution<S extends IScope, L extends ILabel, O extends IOccurrence>
        extends INameResolution<S, L, O> {

    boolean addCached(IResolutionCache<S, L, O> cache);

    IResolutionCache<S, L, O> toCache();

    interface IResolutionCache<S extends IScope, L extends ILabel, O extends IOccurrence> {

        static <S extends IScope, L extends ILabel, O extends IOccurrence> IResolutionCache<S, L, O> empty() {
            return new IResolutionCache<S, L, O>() {};
        }

    }

}