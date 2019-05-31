package mb.statix.taico.solver.query;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import mb.nabl2.terms.ITerm;

public class QueryDetails<S, L> implements IQueryDetails<S, L> {
    private static final long serialVersionUID = 1L;
    
    private Map<String, Map<S, L>> edges;
    private Map<String, Map<S, L>> data;
    private Collection<String> modules;
    private List<ITerm> queryResult;
    
    public QueryDetails(Map<String, Map<S, L>> edges, Map<String, Map<S, L>> data, Collection<String> modules, List<ITerm> queryResult) {
        this.edges = edges;
        this.data = data;
        this.modules = modules;
        this.queryResult = queryResult;
    }
    
    @Override
    public Map<String, Map<S, L>> getRelevantEdges() {
        return edges;
    }

    @Override
    public Map<String, Map<S, L>> getRelevantData() {
        return data;
    }
    
    @Override
    public Collection<String> getReachedModules() {
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
