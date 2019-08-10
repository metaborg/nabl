package mb.statix.taico.ndependencies.edge;

import java.io.Serializable;
import java.util.Collection;

import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.taico.dependencies.Dependency;
import mb.statix.taico.solver.Context;

public interface IEdgeDependencyManager<T> extends Serializable {
    /**
     * The dependencies of the given scope.
     * 
     * @param scope
     *      the scope
     * 
     * @return
     *      the dependencies
     */
    public Iterable<Dependency> getDependencies(Scope scope);
    
    /**
     * The dependencies on the given edge (scope and label).
     * 
     * @param scope
     *      the scope
     * @param label
     *      the label
     * 
     * @return
     *      the dependencies
     */
    public Collection<Dependency> getDependencies(Scope scope, ITerm label);
    
    /**
     * Adds the given dependency.
     * 
     * @param scope
     *      the scope
     * @param label
     *      the label / label matcher
     * @param dependency
     *      the dependency
     * 
     * @return
     *      true if the dependency was added, false if it was already present
     */
    public boolean addDependency(Scope scope, T label, Dependency dependency);
    
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
