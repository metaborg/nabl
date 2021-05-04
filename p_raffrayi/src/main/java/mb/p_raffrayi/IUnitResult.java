package mb.p_raffrayi;

import java.util.List;
import java.util.Set;
import java.util.Map;

import javax.annotation.Nullable;

import mb.scopegraph.oopsla20.IScopeGraph;

public interface IUnitResult<S, L, D, R> {

    String id();

    IScopeGraph.Immutable<S, L, D> scopeGraph();

    Set<IRecordedQuery<S, L, D>> queries();

    List<S> rootScopes();

    @Nullable R analysis();

    List<Throwable> failures();

    Map<String, IUnitResult<S, L, D, ?>> subUnitResults();

    IUnitStats stats();

}