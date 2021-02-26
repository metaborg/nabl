package mb.statix.concurrent.p_raffrayi.diff;

import java.util.List;

import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.scopegraph.diff.ScopeGraphDiff;

public interface IScopeGraphDiffer<S, L, D>  {
    
    IFuture<ScopeGraphDiff<S, L, D>> diff(List<S> currentRootScopes, List<S> previousRootScopes);

}
