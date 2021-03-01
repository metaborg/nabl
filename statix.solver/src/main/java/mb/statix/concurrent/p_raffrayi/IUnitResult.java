package mb.statix.concurrent.p_raffrayi;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import mb.statix.scopegraph.IScopeGraph;

public interface IUnitResult<S, L, D, R> {

    String id();

    IScopeGraph.Immutable<S, L, D> scopeGraph();
    
    Set<IRecordedQuery<S, L, D>> queries();

    List<S> rootScopes();

    @Nullable R analysis();

    List<Throwable> failures();

    IUnitStats stats();

}