package mb.statix.concurrent.p_raffrayi;

import java.util.List;

import javax.annotation.Nullable;

import mb.statix.scopegraph.IScopeGraph;

public interface IUnitResult<S, L, D, R> {

    String id();

    IScopeGraph.Immutable<S, L, D> scopeGraph();
    
    // TODO add root scopes (??)
	
	// TODO add logs of queries

    @Nullable R analysis();

    List<Throwable> failures();

    IUnitStats stats();

}