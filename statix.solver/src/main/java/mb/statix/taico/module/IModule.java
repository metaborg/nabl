package mb.statix.taico.module;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.collect.Streams;

import mb.nabl2.terms.ITerm;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.constraint.CResolveQuery;
import mb.statix.spec.Spec;
import mb.statix.taico.scopegraph.IMInternalScopeGraph;
import mb.statix.taico.scopegraph.IOwnableScope;
import mb.statix.taico.scopegraph.IOwnableTerm;
import mb.statix.taico.solver.IMState;
import mb.statix.taico.solver.SolverContext;
import mb.statix.taico.solver.context.IContextAware;
import mb.statix.taico.solver.query.QueryDetails;
import mb.statix.taico.util.IOwnable;

/**
 * Interface to represent a module.
 */
public interface IModule extends IContextAware, Serializable {
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
                .flatMap(m -> StreamSupport.stream(m.getDescendantsIncludingSelf().spliterator(), false));
    }
    
    /**
     * @return
     *      all the modules that are descendent from this module, including this module itself
     */
    default Stream<IModule> getDescendantsIncludingSelf() {
        return Streams.concat(Stream.of(this), getChildren().stream()
                .flatMap(m -> StreamSupport.stream(m.getDescendantsIncludingSelf().spliterator(), false)));
    }
    
    /**
     * @param name
     *      the name of the child module
     * @param canExtend
     *      the list of scopes from this module and parents that the child can extend, in the order
     *      they are encountered in the rule
     * @param constraint
     *      the constraint which caused this modules creation
     *      (TODO IMPORTANT substitute scopes)
     *      (TODO does this preserve declaration (references) correctly?)
     * 
     * @return
     *      the child
     */
    IModule createChild(String name, List<IOwnableScope> canExtend, IConstraint constraint);
    
    /**
     * If the module with the given name already existed as a child of this module, that module is
     * returned. Otherwise, this method returns a new child module of this module.
     * 
     * @param name
     *      the name of the module to create or get
     * @param canExtend
     *      the list of scopes from this module and parents that the child can extend, in the order
     *      they are encountered in the rule
     * @param moduleBoundary
     *      the name of the module boundary which caused this modules creation
     * @param args
     *      the arguments with which the module boundary was called (TODO IMPORTANT substitute scopes)
     *      (TODO does this preserve declarations (references) correctly?)
     * 
     * @return
     *      the new/old child module
     */
    default IModule createOrGetChild(String name, List<IOwnableScope> canExtend, IConstraint constraint) throws Delay {
        //TODO Incrementality breaks if parent or child names are changed
        IModule oldModule = getChild(name);
        if (oldModule == null) {
            System.err.println("[" + getId() + "] Creating new child " + name);
            return createChild(name, canExtend, constraint);
        }
        
        if (oldModule.getFlag() == ModuleCleanliness.CLEAN) {
            //Update the edges to the new scopes and add it as a child of the current scope graph.
            oldModule.getScopeGraph().substitute(canExtend);
            oldModule.setParent(this);
            //TODO We potentially need to replace some of the old arguments with new ones in the old module results?
            oldModule.setInitialization(constraint);
            //Set the coordinator to our coordinator
//            oldModule.getCurrentState().setCoordinator(getCurrentState().coordinator());
            getScopeGraph().addChild(oldModule);
            return oldModule;
        } else {
            return createChild(name, canExtend, constraint);
        }
    }
    
    /**
     * @param name
     *      the name of the child
     * 
     * @return
     *      the child of this module
     */
    default IModule getChild(String name) throws Delay {
        return getCurrentState().context().getChildModuleByName(this, name);
    }
    
    /**
     * Gets the child with the given name, adding it as a child of this module.
     * The child will have its parent and coordinator updated.
     * 
     * @param name
     *      the name of the child
     * 
     * @return
     *      the child, or null if no such child exists
     */
    default IModule getChildAndAdd(String name) throws Delay {
        IModule child = getChild(name);
        if (child == null) return null;
        
        child.setParent(this);
//        child.getCurrentState().setCoordinator(getCurrentState().coordinator());
        getScopeGraph().addChild(child);
        getCurrentState().context().addModule(child);
        return child;
    }
    
    /**
     * Removes the given module as child of this module.
     * 
     * @param module
     *      the module
     */
    default void removeChild(IModule module) {
        getScopeGraph().removeChild(module);
    }
    
    /**
     * @return
     *      the initialization cause of this module
     */
    IConstraint getInitialization();
    
    /**
     * Sets the constraint causing the initialization of this module.
     * 
     * @param constraint
     *      the constraint that caused this module to be created
     */
    void setInitialization(IConstraint constraint);

    // --------------------------------------------------------------------------------------------
    // Convenience methods
    // --------------------------------------------------------------------------------------------
    
    /**
     * Convenience method.
     * 
     * @see SolverContext#getState(IModule)
     */
    default IMState getCurrentState() {
        return getContext().getState(this);
    }
    
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
    
    default boolean flagIfClean(ModuleCleanliness cleanliness) {
        if (getFlag() != ModuleCleanliness.CLEAN) return false;
        flag(cleanliness);
        return true;
    }
    
    ModuleCleanliness getFlag();
    
    /**
     * Resets the module to a clean module: no children, no scope graph.
     */
    void reset(Spec spec);
    
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
