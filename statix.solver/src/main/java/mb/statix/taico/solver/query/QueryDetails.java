package mb.statix.taico.solver.query;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import mb.nabl2.terms.ITerm;
import mb.statix.taico.module.IModule;

public class QueryDetails<S, L, R> implements IQueryDetails<S, L, R> {
    private Map<IModule, Map<S, L>> edges;
    private Map<IModule, Map<S, R>> data;
    private Collection<? extends IModule> modules;
    private List<ITerm> queryResult;
    
    public QueryDetails(Map<IModule, Map<S, L>> edges, Map<IModule, Map<S, R>> data, Collection<? extends IModule> modules, List<ITerm> queryResult) {
        this.edges = edges;
        this.data = data;
        this.modules = modules;
        this.queryResult = queryResult;
    }
    @Override
    public Map<IModule, Map<S, L>> getRelevantEdges() {
        return edges;
    }

    @Override
    public Map<IModule, Map<S, R>> getRelevantData() {
        return data;
    }
    
    @Override
    public Collection<? extends IModule> getReachedModules() {
        return modules;
    }
    
    @Override
    public List<ITerm> getQueryResult() {
        return queryResult;
    }
    
    @Override
    public String toString() {
        return "QueryDetails<edges=" + edges + ", data=" + data + ", modules=" + modules + ">";
    }
}
