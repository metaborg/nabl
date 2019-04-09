package mb.statix.taico.module;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

import mb.nabl2.terms.ITerm;
import mb.statix.solver.constraint.CResolveQuery;
import mb.statix.taico.scopegraph.IMInternalScopeGraph;
import mb.statix.taico.scopegraph.IOwnableScope;
import mb.statix.taico.scopegraph.IOwnableTerm;
import mb.statix.taico.solver.MState;
import mb.statix.taico.solver.query.QueryDetails;

/**
 * Interface to represent a module.
 */
public interface IModule {
    /**
     * @return
     *      the unique identifier for this module
     */
    String getId();
    
    /**
     * @return
     *      the parent of this module, or null if this is the top level module
     */
    IModule getParent();
    
    /**
     * @return
     *      the children of this module
     */
    Set<IModule> getChildren();
    
//    /**
//     * This method returns the previous version of this module, residing in a different
//     * module manager.
//     * 
//     * @return
//     *      the previous version of this module, or null if there was none
//     */
//    IModule getPreviousVersion();
    
    /**
     * Returns the mutable scope graph belonging to this module. Additions can be made to the
     * returned scope graph, so consecutive calls can yield different results.
     * 
     * @return
     *      the scope graph of this module
     */
    IMInternalScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> getScopeGraph();
    
    /**
     * @return
     *      all the modules that are descendent from this module
     */
    default Iterable<IModule> getDescendants() {
        return getChildren().stream()
                .flatMap(m -> StreamSupport.stream(m.getDescendants().spliterator(), false))
                ::iterator;
    }
    
    /**
     * @param canExtend
     *      the scopes from this module and parents that the child can extend, in the order they
     *      are encountered in the rule
     * 
     * @return
     *      the child
     */
    IModule createChild(List<IOwnableScope> canExtend);
    
    //TODO IMPORTANT This should be implemented
    default IModule createChildOrCopyOld(String name, List<IOwnableScope> canExtend) {
        //The old version is in the 
        return null;
    }
    
    /**
     * @return
     *      the state of this module
     */
    MState getCurrentState();
    
    /**
     * Method used once when a module state has been constructed.
     * 
     * @param state
     *      the state of this module
     *      
     * @throws IllegalStateException
     *      If the state of this module has already been set.
     */
    void setCurrentState(MState state);
    
    /**
     * Adds a query with its resolution details to determine the dependencies.
     * 
     * @param query
     *      the constraint representing the query
     * @param details
     *      the details relevant for dependencies related to this query
     */
    void addQuery(CResolveQuery query, QueryDetails<IOwnableTerm, ITerm, ITerm> details);
    
    
    /**
     * The aggregated set of all dependencies based on all the queries in this module.
     * 
     * @return
     *      the dependencies of this module
     */
    Set<? extends IModule> getDependencies();
    
    void addDependant(IModule module, CResolveQuery query);
    
    Map<IModule, CResolveQuery> getDependants();
    
    void flag(ModuleCleanliness cleanliness);
    
    ModuleCleanliness getFlag();
    
    //Set<IQuery<IOwnableTerm, ITerm, ITerm, ITerm>> queries();
    
//    /**
//     * Creates a copy of this module with the given manager, the same id as this module and the
//     * given new parent as well as the scopes that need to be substituted.
//     * 
//     * @param newManager
//     *      the new manager
//     * @param newParent
//     *      the new parent module (transitive copying)
//     * @param newScopes
//     *      the scopes of the parent that will substitute the old parent scopes
//     * 
//     * @return
//     *      the copied module
//     */
//    IModule copy(ModuleManager newManager, IModule newParent, List<IOwnableScope> newScopes);
    
}
