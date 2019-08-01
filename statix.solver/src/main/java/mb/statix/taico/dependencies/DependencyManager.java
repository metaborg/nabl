package mb.statix.taico.dependencies;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import mb.statix.taico.util.TOverrides;

public class DependencyManager<D extends Dependencies> implements Serializable, Iterable<Entry<String, D>> {
    private static final long serialVersionUID = 1L;
    
    private final Function<String, D> creator;
    private final Map<String, D> map = TOverrides.hashMap();
    
    public DependencyManager(Function<String, D> creator) {
        this.creator = creator;
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
        //TODO We might want to switch to not automatically creating dependencies
        return map.computeIfAbsent(moduleId, k -> creator.apply(moduleId));
    }
    
    /**
     * @param moduleId
     *      the id of the module
     * @param dependencies
     *      the dependencies to set
     * 
     * @return
     *      the dependencies that were replaced, null if this is the first time dependencies are
     *      set for this module
     */
    public D setDependencies(String moduleId, D dependencies) {
        D old;
        if ((old = map.putIfAbsent(moduleId, dependencies)) != null) {
            System.err.println("Replacing dependencies of " + moduleId + " with new dependencies!");
            return old;
        }
        return null;
    }
    
    @Override
    public Iterator<Entry<String, D>> iterator() {
        return map.entrySet().iterator();
    }
    
    /**
     * NOTE: Changes to the returned set write through to the manager.
     * 
     * @return
     *      a set with the ids of all the modules in the manager
     */
    public Set<String> modules() {
        return map.keySet();
    }
    
    /**
     * Changes to the returned collection write through to the manager.
     * 
     * @return
     *      a collection with all the dependencies in the manager
     */
    public Collection<D> dependencies() {
        return map.values();
    }
}
