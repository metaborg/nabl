package mb.statix.concurrent.p_raffrayi;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import mb.statix.concurrent.p_raffrayi.impl.EdgeOrEps;
import mb.statix.scopegraph.IScopeGraph;

public interface IUnitResult<S, L, D, R> {

    String id();

    IScopeGraph.Immutable<S, EdgeOrEps<L>, D> scopeGraph();

    @Nullable R analysis();

    List<Throwable> failures();

    Map<String, IUnitResult<S, L, D, ?>> subUnitResults();

    IUnitStats stats();

}