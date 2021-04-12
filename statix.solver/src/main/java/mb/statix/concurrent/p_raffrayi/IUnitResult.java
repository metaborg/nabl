package mb.statix.concurrent.p_raffrayi;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import mb.scopegraph.oopsla20.IScopeGraph;

public interface IUnitResult<S, L, D, R> {

    String id();

    IScopeGraph.Immutable<S, L, D> scopeGraph();

    @Nullable R analysis();

    List<Throwable> failures();

    Map<String, IUnitResult<S, L, D, ?>> subUnitResults();

    IUnitStats stats();

}