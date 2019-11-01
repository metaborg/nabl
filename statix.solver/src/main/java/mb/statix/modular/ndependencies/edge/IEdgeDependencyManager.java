package mb.statix.modular.ndependencies.edge;

import java.io.Serializable;
import java.util.Collection;

import mb.nabl2.terms.ITerm;
import mb.statix.modular.dependencies.Dependency;
import mb.statix.modular.dependencies.affect.IEdgeAdditionAffect;
import mb.statix.modular.dependencies.affect.IEdgeRemovalAffect;
import mb.statix.modular.ndependencies.observer.IDependencyObserver;
import mb.statix.scopegraph.terms.Scope;

public interface IEdgeDependencyManager<T> extends IDependencyObserver, IEdgeAdditionAffect, IEdgeRemovalAffect, Serializable {
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
    
    // --------------------------------------------------------------------------------------------
    // Affect
    // --------------------------------------------------------------------------------------------
    
    @Override
    default Iterable<Dependency> affectedByEdgeAddition(Scope scope, ITerm label) {
        return getDependencies(scope, label);
    }
    
    @Override
    default Iterable<Dependency> affectedByEdgeRemoval(Scope scope, ITerm label) {
        return getDependencies(scope, label);
    }
}
