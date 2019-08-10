package mb.statix.taico.ndependencies.name;

import java.io.Serializable;
import java.util.Collection;

import mb.statix.scopegraph.terms.Scope;
import mb.statix.taico.dependencies.Dependency;
import mb.statix.taico.name.NameAndRelation;
import mb.statix.taico.solver.Context;

public interface INameDependencyManager extends Serializable {
    
    /**
     * The dependencies on the given name and relation in the given scope.
     * 
     * @param nameRel
     *      the name and relation
     * @param scope
     *      the scope
     * 
     * @return
     *      the dependencies
     */
    public Collection<Dependency> getDependencies(NameAndRelation nameRel, Scope scope);
    
    /**
     * Adds the given dependency.
     * 
     * @param nameRel
     *      the name and relation
     * @param scope
     *      the scope
     * @param dependency
     *      the dependency
     * 
     * @return
     *      true if the dependency was added, false if it was already present
     */
    public boolean addDependency(NameAndRelation nameRel, Scope scope, Dependency dependency);
    
    /**
     * Removes the given dependencies.
     * 
     * @param dependencies
     *      the dependencies to remove
     */
    public void removeDependencies(Collection<Dependency> dependencies);
    
    /**
     * Resets the dependencies of the given module.
     * 
     * @param module
     *      the module to reset
     */
    public default void resetDependencies(String module) {
        removeDependencies(Context.context().getDependencies(module).getDependencies().values());
    }
}
