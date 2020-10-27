package mb.statix.concurrent.p_raffrayi;

import java.util.List;

import mb.statix.scopegraph.IScopeGraph;

public interface IUnitResult<S, L, D, R> {

    IScopeGraph.Immutable<S, L, D> scopeGraph();

    R analysis();

    List<Throwable> failures();

    IUnitStats stats();

}