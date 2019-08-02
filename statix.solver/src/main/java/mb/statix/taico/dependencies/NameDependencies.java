package mb.statix.taico.dependencies;

import static mb.statix.taico.solver.Context.context;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import mb.statix.constraints.CResolveQuery;
import mb.statix.taico.module.IModule;
import mb.statix.taico.solver.query.QueryDetails;

public class NameDependencies extends Dependencies {
    private static final long serialVersionUID = 1L;
    
    private Map<CResolveQuery, QueryDetails> queries = new HashMap<>();
    
    public NameDependencies(String owner) {
        super(owner);
    }
    
    public Set<IModule> getQueryModuleDependencies() {
        return queries.values().stream().flatMap(d -> d.getReachedModules().stream()).map(d -> context().getModuleUnchecked(d)).collect(Collectors.toSet());
    }
    
    public Set<String> getQueryModuleDependencyIds() {
        return queries.values().stream().flatMap(d -> d.getReachedModules().stream()).collect(Collectors.toSet());
    }
    
    public void addQuery(CResolveQuery query, QueryDetails details) {
        queries.put(query, details);
    }
    
    public Map<CResolveQuery, QueryDetails> queries() {
        return queries;
    }
    
    @Override
    public NameDependencies copy() {
        NameDependencies copy = new NameDependencies(owner);
        copy.dependencies.putAll(dependencies);
        copy.dependants.putAll(dependants);
        copy.queries.putAll(queries);
        return copy;
    }
}
