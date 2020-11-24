package mb.statix.concurrent.p_raffrayi;

import java.util.List;

import javax.annotation.Nullable;

import mb.statix.scopegraph.IScopeGraph;

public interface IUnitResult<S, L, D, R> {

    IScopeGraph.Immutable<S, L, D> scopeGraph();

    @Nullable R analysis();

    List<Throwable> failures();

    IUnitStats stats();

}