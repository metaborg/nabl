package mb.statix.taico.module;

import static mb.statix.taico.solver.SolverContext.context;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.Streams;

import mb.nabl2.terms.ITerm;
import mb.statix.constraints.CResolveQuery;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.spec.Spec;
import mb.statix.taico.incremental.Flaggable;
import mb.statix.taico.scopegraph.IMInternalScopeGraph;
import mb.statix.taico.scopegraph.reference.ModuleDelayException;
import mb.statix.taico.solver.SolverContext;
import mb.statix.taico.solver.query.QueryDetails;
import mb.statix.taico.solver.state.IMState;
import mb.statix.taico.util.IOwnable;

/**
 * Interface to represent a module.
 */
public interface IModule extends Flaggable, Serializable {
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
     * @return
     *      the id of the parent of this module
     */
    String getParentId();
    
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
        return Iterables2.stream(getScopeGraph().getChildren()).map(IOwnable::getOwner).collect(Collectors.toSet());
    }
    
    /**
     * Returns the mutable scope graph belonging to this module. Additions can be made to the
     * returned scope graph, so consecutive calls can yield different results.
     * 
     * @return
     *      the scope graph of this module
     */
    IMInternalScopeGraph<Scope, ITerm, ITerm> getScopeGraph();
    
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
     * Creates a child module, but not the state.
     * 
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
    IModule createChild(String name, List<Scope> canExtend, IConstraint constraint);
    
    /**
     * Adds a (newly created) child module.
     * 
     * @param child
     *      the child module
     */
    default void addChild(IModule child) {
        getScopeGraph().addChild(child);
    }
    
    /**
     * If the module with the given name already existed as a child of this module, that module is
     * returned. Otherwise, this method returns a new child module of this module.
     * <p>
     * <b>NOTE:</b> the child module must still be added as a child after creation.
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
    default IModule createOrGetChild(String name, List<Scope> canExtend, IConstraint constraint) {
        //TODO This method might no longer be neccessary, or it might not need to check for the old module flag.
        //TODO Incrementality breaks if parent or child names are changed
        IModule oldModule = getChild(name);
        if (oldModule == null) {
            return createChild(name, canExtend, constraint);
        }
        
        if (oldModule.getTopCleanliness() == ModuleCleanliness.CLEAN) {
            //Update the edges to the new scopes and add it as a child of the current scope graph.
            oldModule.getScopeGraph().substitute(canExtend);
            oldModule.setParent(this);
            //TODO We potentially need to replace some of the old arguments with new ones in the old module results?
            oldModule.setInitialization(constraint);
            return oldModule;
        } else {
            return createChild(name, canExtend, constraint);
        }
    }
    
    /**
     * Gets the child with the given name if it is clean. If no child with the given name exists
     * or if the child is not clean, this method returns null.
     * <p>
     * If the child exists, its scopes are substituted for the given scopes and its initialization
     * is updated. The state of the child is reused.
     * 
     * @param name
     *      the name of the child
     * @param canExtend
     *      the scope the child can extend
     * @param constraint
     *      the new initialization constraint for the child
     * 
     * @return
     *      the child if clean, or null otherwise
     */
    default IModule getChildIfClean(String name, List<Scope> canExtend, IConstraint constraint) {
        IModule oldModule;
        try {
            oldModule = getChild(name);
        } catch (ModuleDelayException ex) {
            return null;
        }
        if (oldModule == null || oldModule.getTopCleanliness() != ModuleCleanliness.CLEAN) return null;
        
        oldModule.getScopeGraph().substitute(canExtend);
        oldModule.setParent(this);
        //TODO We potentially need to replace some of the old arguments with new ones in the old module results?
        oldModule.setInitialization(constraint);
        context().reuseOldState(oldModule);
        return oldModule;
    }
    
    /**
     * @param name
     *      the name of the child
     * 
     * @return
     *      the child of this module
     */
    default IModule getChild(String name) throws ModuleDelayException {
        return context().getChildModuleByName(this, name);
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
        context().addModule(child);
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
        IMState state = SolverContext.context().getState(this);
        if (state == null) System.err.println("State of " + this + " is null!");
        return state;
    }
    
    /**
     * Adds a query with its resolution details to determine the dependencies.
     * 
     * @param query
     *      the constraint representing the query
     * @param details
     *      the details relevant for dependencies related to this query
     */
    void addQuery(CResolveQuery query, QueryDetails details);
    
    Map<CResolveQuery, QueryDetails> queries();
    
    /**
     * The aggregated set of all dependencies based on all the queries in this module.
     * 
     * @return
     *      the dependencies of this module
     */
    Set<? extends IModule> getDependencies();
    
    void addDependant(String module, CResolveQuery query);
    
    Map<IModule, CResolveQuery> getDependants();
    
    /**
     * Resets the module to a clean module: no children, no scope graph.
     */
    void reset(Spec spec);
    
    /**
     * @return
     *      a copy of this module, not added to the context
     */
    IModule copy();
    
    //Set<IQuery<IOwnableTerm, ITerm, ITerm, ITerm>> queries();
}
