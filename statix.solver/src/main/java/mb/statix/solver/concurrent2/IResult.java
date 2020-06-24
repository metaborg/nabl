package mb.statix.solver.concurrent2;

import java.util.Map;

import mb.statix.scopegraph.IScopeGraph;

public interface IResult<S, L, D> {

    Map<String, IScopeGraph<S, L, D>> scopeGraphs();

}