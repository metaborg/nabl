package mb.statix.concurrent.p_raffrayi;

import mb.statix.scopegraph.IScopeGraph;

public interface IUnitResult<S, L, D, R> {

    IScopeGraph.Immutable<S, L, D> scopeGraph();

    R analysis();

}