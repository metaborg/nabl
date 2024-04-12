package mb.scopegraph.library;

import java.util.List;
import java.util.Set;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.tuple.Tuple2;

import mb.scopegraph.oopsla20.IScopeGraph;

public interface IScopeGraphLibrary<S, L, D> {

    /**
     * Initialize this library with the given root scopes and provide the function to generate fresh scopes for the
     * library's own scopes.
     * 
     * Returns the fresh scopes and the library's scope graph.
     * 
     * Must be called before any of the other methods can be called.
     */
    Tuple2<? extends Set<S>, IScopeGraph.Immutable<S, L, D>> initialize(List<S> rootScopes,
            Function1<String, S> freshScope);

}