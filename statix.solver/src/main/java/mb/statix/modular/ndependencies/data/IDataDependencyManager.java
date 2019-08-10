package mb.statix.modular.ndependencies.data;

import java.io.Serializable;
import java.util.Collection;

import mb.nabl2.terms.ITerm;
import mb.statix.modular.dependencies.Dependency;
import mb.statix.modular.dependencies.affect.IDataAdditionAffect;
import mb.statix.modular.dependencies.affect.IDataRemovalOrChangeAffect;
import mb.statix.modular.name.NameAndRelation;
import mb.statix.modular.ndependencies.observer.IDependencyObserver;
import mb.statix.scopegraph.terms.Scope;

public interface IDataDependencyManager<T> extends IDependencyObserver, IDataAdditionAffect, IDataRemovalOrChangeAffect, Serializable {
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
    default Iterable<Dependency> affectedByDataAddition(NameAndRelation nameAndRelation, Scope scope) {
        return getDependencies(scope, nameAndRelation.getRelation());
    }
    
    @Override
    default Iterable<Dependency> affectedByDataRemovalOrChange(NameAndRelation nameAndRelation, Scope scope) {
        return getDependencies(scope, nameAndRelation.getRelation());
    }
}
