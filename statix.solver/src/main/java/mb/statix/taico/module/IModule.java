package mb.statix.taico.module;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import mb.nabl2.terms.ITerm;
import mb.statix.solver.constraint.CResolveQuery;
import mb.statix.taico.scopegraph.IMInternalScopeGraph;
import mb.statix.taico.scopegraph.IOwnableScope;
import mb.statix.taico.scopegraph.IOwnableTerm;
import mb.statix.taico.solver.MState;
import mb.statix.taico.solver.query.QueryDetails;
import mb.statix.taico.util.IOwnable;

/**
 * Interface to represent a module.
 */
public interface IModule {
    /**
     * @return
     *      the name of this module, could be non unique
     */
    String getName();
    
    /**
     * The unique identifier for this module, defined in terms of a path separated by $.
     * 
     * @return
     *      the full unique identifier for this module
     */
    String getId();
    
    /**
     * @return
     *      the parent of this module
     */
    IModule getParent();
    
    /**
     * Sets the parent of this module to the given module.
     * 
     * Used for moving modules in the module tree.
     * 
     * @param module
     *      the module
     */
    void setParent(IModule module);
    
    /**
     * @return
     *      the children of this module
     */
    default Set<IModule> getChildren() {
        return getScopeGraph().getChildren().stream().map(IOwnable::getOwner).collect(Collectors.toSet());
    }
    
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
    default Stream<IModule> getDescendants() {
        return getChildren().stream()
                .flatMap(m -> StreamSupport.stream(m.getDescendants().spliterator(), false));
    }
    
    /**
     * @param name
     *      the name of the child module
     * @param canExtend
     *      the list of scopes from this module and parents that the child can extend, in the order
     *      they are encountered in the rule
     * 
     * @return
     *      the child
     */
    IModule createChild(String name, List<IOwnableScope> canExtend);
    
    /**
     * If the module with the given name already existed as a child of this module, that module is
     * returned. Otherwise, this method returns a new child module of this module.
     * 
     * @param name
     *      the name of the module to create or get
     * @param canExtend
     *      the list of scopes from this module and parents that the child can extend, in the order
     *      they are encountered in the rule
     * 
     * @return
     *      the new/old child module
     */
    default IModule createOrGetChild(String name, List<IOwnableScope> canExtend) {
        //TODO Incrementality breaks if parent or child names are changed
        String id = ModulePaths.build(getId(), name);
        IModule oldModule = getCurrentState().manager().getModule(id);
        if (oldModule != null && oldModule.getFlag() == ModuleCleanliness.CLEAN) {
            //Update the edges to the new scopes and add it as a child of the current scope graph.
            oldModule.getScopeGraph().substitute(canExtend);
            oldModule.setParent(this);
            getScopeGraph().addChild(oldModule);
            return oldModule;
        } else {
            return createChild(name, canExtend);
        }
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
