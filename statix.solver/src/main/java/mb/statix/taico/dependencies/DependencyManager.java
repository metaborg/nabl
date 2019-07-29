package mb.statix.taico.dependencies;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Supplier;

import mb.statix.taico.util.TOverrides;

public class DependencyManager<D extends Dependencies> implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final Supplier<D> supplier;
    private final Map<String, D> map = TOverrides.hashMap();
    
    public DependencyManager(Supplier<D> supplier) {
        this.supplier = supplier;
    }
    
    /**
     * @param moduleId
     *      the id of the module
     * 
     * @return
     *      true if dependencies have been requested for this module before, false otherwise
     */
    public boolean hasDependencies(String moduleId) {
        return map.containsKey(moduleId);
    }
    
    /**
     * @param moduleId
     *      the id of the module to get dependencies of
     * 
     * @return
     *      the dependencies of the given module
     */
    public D getDependencies(String moduleId) {
        return map.computeIfAbsent(moduleId, k -> supplier.get());
    }
}
