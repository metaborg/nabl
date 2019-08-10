package mb.statix.modular.ndependencies.query;

import java.io.Serializable;
import java.util.Collection;

import mb.nabl2.terms.ITerm;
import mb.statix.modular.dependencies.Dependency;
import mb.statix.modular.dependencies.affect.IDataRemovalOrChangeAffect;
import mb.statix.modular.dependencies.affect.IEdgeRemovalAffect;
import mb.statix.modular.dependencies.details.QueryResultDependencyDetail;
import mb.statix.modular.name.NameAndRelation;
import mb.statix.modular.ndependencies.observer.IDependencyObserver;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.path.IStep;
import mb.statix.scopegraph.terms.Scope;

/**
 * Optimal for edge removal, data removal and data change.
 */
public interface IQueryDependencyManager extends IDependencyObserver, IDataRemovalOrChangeAffect, IEdgeRemovalAffect, Serializable {
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
     * The dependencies on the given edge/data edge.
     * 
     * @param scope
     *      the scope
     * @param label
     *      the label / relation
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
     *      the label / relation
     * @param dependency
     *      the dependency
     * 
     * @return
     *      true if the dependency was added, false if it was already present
     */
    public boolean addDependency(Scope scope, ITerm label, Dependency dependency);
    
    @Override
    public default void onDependencyAdded(Dependency dependency) {
        QueryResultDependencyDetail qrdd = dependency.getDetails(QueryResultDependencyDetail.class);
        for (IResolutionPath<Scope, ITerm, ITerm> path : qrdd.getPaths()) {
            addDependency(path.getPath().getTarget(), path.getLabel(), dependency);
            for (IStep<Scope, ITerm> step : path.getPath()) {
                addDependency(step.getSource(), step.getLabel(), dependency);
            }
        }
    }
    
    // --------------------------------------------------------------------------------------------
    // Affect
    // --------------------------------------------------------------------------------------------
    
    @Override
    default Iterable<Dependency> affectedByDataRemovalOrChange(NameAndRelation nameAndRelation, Scope scope) {
        return getDependencies(scope, nameAndRelation.getRelation());
    }
    
    @Override
    default Iterable<Dependency> affectedByEdgeRemoval(Scope scope, ITerm label) {
        return getDependencies(scope, label);
    }
}
