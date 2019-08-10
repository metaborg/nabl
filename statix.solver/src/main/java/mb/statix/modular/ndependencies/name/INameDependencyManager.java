package mb.statix.modular.ndependencies.name;

import java.io.Serializable;
import java.util.Collection;

import mb.statix.modular.dependencies.Dependency;
import mb.statix.modular.dependencies.affect.IDataAdditionAffect;
import mb.statix.modular.dependencies.affect.IDataRemovalOrChangeAffect;
import mb.statix.modular.dependencies.details.NameDependencyDetail;
import mb.statix.modular.name.NameAndRelation;
import mb.statix.modular.ndependencies.observer.IDependencyObserver;
import mb.statix.scopegraph.terms.Scope;

/**
 * Optimal (if not simple) for data addition, suboptimal for removal/changes.
 */
public interface INameDependencyManager extends IDependencyObserver, IDataAdditionAffect, IDataRemovalOrChangeAffect, Serializable {
    
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
     * @param dependency
     *      the dependency
     * 
     * @return
     *      the name and relation of the given dependency
     */
    public static NameAndRelation getNameFromDependency(Dependency dependency) {
        NameDependencyDetail detail = dependency.getDetails(NameDependencyDetail.class);
        return detail.toNameAndRelation();
    }
    
    // --------------------------------------------------------------------------------------------
    // Affect
    // --------------------------------------------------------------------------------------------
    
    @Override
    default Iterable<Dependency> affectedByDataAddition(NameAndRelation nameAndRelation, Scope scope) {
        return getDependencies(nameAndRelation, scope);
    }
    
    @Override
    default Iterable<Dependency> affectedByDataRemovalOrChange(NameAndRelation nameAndRelation, Scope scope) {
        return getDependencies(nameAndRelation, scope);
    }
}
