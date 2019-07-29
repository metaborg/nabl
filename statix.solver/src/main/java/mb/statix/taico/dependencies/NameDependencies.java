package mb.statix.taico.dependencies;

import static mb.statix.taico.solver.SolverContext.context;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import mb.statix.constraints.CResolveQuery;
import mb.statix.taico.module.IModule;
import mb.statix.taico.solver.query.QueryDetails;
import mb.statix.taico.util.TOverrides;

public class NameDependencies extends Dependencies {
    private static final long serialVersionUID = 1L;
    
    private Map<CResolveQuery, QueryDetails> queries = new HashMap<>();
    private Map<String, CResolveQuery> dependants = TOverrides.hashMap();
    
    @Override
    public Set<IModule> getDependencies() {
        return queries.values().stream().flatMap(d -> d.getReachedModules().stream()).map(d -> context().getModuleUnchecked(d)).collect(Collectors.toSet());
    }
    
    public void addQuery(CResolveQuery query, QueryDetails details) {
        queries.put(query, details);
    }
    
    public Map<CResolveQuery, QueryDetails> queries() {
        return queries;
    }
    
    public void addDependant(String module, CResolveQuery query) {
        dependants.put(module, query);
    }
    
    public Map<IModule, CResolveQuery> getDependants() {
        return dependants.entrySet().stream()
                .collect(Collectors.toMap(e -> context().getModuleUnchecked(e.getKey()), Entry::getValue));
    }
    
    public Map<String, CResolveQuery> getDependantIds() {
        return dependants;
    }
}
