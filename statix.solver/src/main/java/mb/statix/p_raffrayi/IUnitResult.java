package mb.statix.p_raffrayi;

import mb.statix.scopegraph.IScopeGraph;

public interface IUnitResult<S, L, D, R> {

    IScopeGraph<S, L, D> scopeGraphs();

    R analysis();

}