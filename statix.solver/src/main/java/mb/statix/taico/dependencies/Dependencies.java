package mb.statix.taico.dependencies;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;

import mb.statix.taico.dependencies.details.IDependencyDetail;
import mb.statix.taico.module.IModule;
import mb.statix.taico.solver.Context;
import mb.statix.taico.util.Modules;

/**
 * Class to represent dependencies of a module.
 */
public class Dependencies implements Serializable {
    private static final long serialVersionUID = 1L;
    
    protected final String owner;
    protected final ListMultimap<String, Dependency> dependencies = MultimapBuilder.hashKeys().arrayListValues().build();
    protected final ListMultimap<String, Dependency> dependants = MultimapBuilder.hashKeys().arrayListValues().build();
    
    public Dependencies(String owner) {
        this.owner = owner;
    }

    /**
     * @return
     *      the owner of these dependencies
     */
    public String getOwner() {
        return owner;
    }
    
    // --------------------------------------------------------------------------------------------
    // Dependencies
    // --------------------------------------------------------------------------------------------
    
    /**
     * @return
     *      a multimap with all the modules that this module depends on
     */
    public ListMultimap<String, Dependency> getDependencies() {
        return dependencies;
    }
    
    /**
     * @param dependency
     *      the dependency
     * 
     * @return
     *      all the dependences on this module, or an empty list if there is no dependency
     */
    public List<Dependency> getDependencies(String dependency) {
        return dependencies.get(dependency);
    }
    
    /**
     * @param dependency
     *      the dependency
     * 
     * @return
     *      all the dependences on this module, or an empty list if there is no dependency
     */
    public List<Dependency> getDependencies(IModule dependency) {
        return getDependencies(dependency.getId());
    }
    
    /**
     * Convenience method.
     * <p>
     * NOTE: This method looks up all the modules in the context via {@link Modules#toModules}.
     * 
     * @return
     *      a set of all the modules that depend on this module
     */
    public Set<IModule> getModuleDependencies() {
        return Modules.toModules(getModuleDependencyIds());
    }
    
    /**
     * @return
     *      a set of all the module ids that depend on this module
     */
    public Set<String> getModuleDependencyIds() {
        return dependencies.keySet();
    }
    
    /**
     * Adds multiple dependencies, one for each module.
     * 
     * TODO This should potentially be refined to only record a single dependency in this case.
     * All these dependencies are in fact caused by one thing.
     * 
     * @param modules
     *      the modules that are depended upon
     * @param details
     *      the details
     */
    public void addMultiDependency(Set<String> modules, IDependencyDetail... details) {
        //TODO Do we want to group dependencies together in some way?
        //TODO We can also solve that problem by keeping track of dependencies we have checked when redoing queries.
        for (String module : modules) {
            addDependency(module, details);
        }
    }
    
    /**
     * @param module
     *      the module depended upon
     * @param details
     *      all the recorded details for this dependency
     * 
     * @return
     *      the created dependency details for this module
     */
    public Dependency addDependency(String module, IDependencyDetail... details) {
        Dependency dependency = new Dependency(owner, module, details);
        dependencies.put(module, dependency);
        
        Context.context().getDependencies(module).addDependant(owner, dependency);
        
        return dependency;
    }
    
    /**
     * Creates a new dependency {@code A -> B | <details>} where A is this module and B is the
     * given module.
     * 
     * @param module
     *      the module that is depended upon
     * @param details
     *      all the recorded details for this dependency
     * 
     * @return
     *      the created dependency
     */
    public Dependency addDependency(IModule module, IDependencyDetail... details) {
        return addDependency(module.getId(), details);
    }
    
    // --------------------------------------------------------------------------------------------
    // Dependants
    // --------------------------------------------------------------------------------------------
    
    /**
     * @return
     *      a multimap with all the modules that depend on this module
     */
    public ListMultimap<String, Dependency> getDependants() {
        return dependants;
    }
    
    /**
     * Convenience method.
     * <p>
     * NOTE: This method looks up all the modules in the context via {@link Modules#toModules}.
     * 
     * @return
     *      a set of all the modules that depend on this module
     */
    public Set<IModule> getModuleDependants() {
        return Modules.toModules(getModuleDependantIds());
    }
    
    /**
     * @return
     *      a set of all the module ids that depend on this module
     */
    public Set<String> getModuleDependantIds() {
        return dependants.keySet();
    }
    
    /**
     * Adds the given dependency as a dependant. More specifically, for the given dependency
     * {@code A -> B}, the given string represents module B and this dependencies object represents
     * the dependencies of A.
     * 
     * @param module
     *      the module that is depended upon
     * @param dependency
     *      the dependency A -> B
     * 
     * @return
     *      the given dependency
     */
    protected Dependency addDependant(String module, Dependency dependency) {
        dependants.put(module, dependency);
        return dependency;
    }
    
    // --------------------------------------------------------------------------------------------
    // Copy
    // --------------------------------------------------------------------------------------------
    
    /**
     * @return
     *      a copy of this dependencies object
     */
    public Dependencies copy() {
        Dependencies copy = new Dependencies(owner);
        copy.dependencies.putAll(dependencies);
        copy.dependants.putAll(dependants);
        return copy;
    }
    
    // --------------------------------------------------------------------------------------------
    // Object methods
    // --------------------------------------------------------------------------------------------
    
    @Override
    public String toString() {
        return dependencies.toString();
    }
}
