package mb.statix.modular.module;

import static mb.statix.modular.solver.Context.context;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Streams;

import mb.nabl2.terms.ITerm;
import mb.statix.modular.dependencies.Dependencies;
import mb.statix.modular.dependencies.Dependency;
import mb.statix.modular.dependencies.details.QueryDependencyDetail;
import mb.statix.modular.incremental.Flaggable;
import mb.statix.modular.scopegraph.IMInternalScopeGraph;
import mb.statix.modular.scopegraph.reference.ModuleDelayException;
import mb.statix.modular.solver.Context;
import mb.statix.modular.solver.state.IMState;
import mb.statix.modular.util.IOwnable;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;

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
     *      the id of the parent of this module
     */
    String getParentId();
    
    /**
     * Convenience method for getting the parent.
     * 
     * @return
     *      the parent of this module
     */
    default IModule getParent() {
        final String parentId = getParentId();
        return parentId == null ? null : context().getModuleUnchecked(parentId);
    }
    
    /**
     * Convenience method for getting all parent modules.
     * 
     * @return
     *      the parents of this module
     */
    default Iterable<IModule> getParents() {
        List<IModule> tbr = new ArrayList<>();
        IModule parent = this;
        while ((parent = parent.getParent()) != null) {
            tbr.add(parent);
        }
        return tbr;
    }
    
    // --------------------------------------------------------------------------------------------
    // Children convenience
    // --------------------------------------------------------------------------------------------
    
    /**
     * @return
     *      the children of this module
     */
    default Set<IModule> getChildren() {
        return Iterables2.stream(getScopeGraph().getChildren()).map(IOwnable::getOwner).collect(Collectors.toSet());
    }
    
    /**
     * Convenience method. Returns the scope graph of the current state of this module.
     * <p>
     * Please note that scope graphs are mutable, so consecutive calls on the scope graph can yield
     * differing results.
     * 
     * @return
     *      the scope graph of this module, or null if this module does not have a state
     */
    default IMInternalScopeGraph<Scope, ITerm, ITerm> getScopeGraph() {
        IMState state = getCurrentState();
        return state == null ? null : state.scopeGraph();
    }
    
    /**
     * @return
     *      all the modules that are descendent from this module
     */
    default Stream<IModule> getDescendants() {
        return getChildren().stream()
                .flatMap(m -> m.getDescendantsIncludingSelf());
    }
    
    /**
     * @return
     *      all the modules that are descendent from this module, including this module itself
     */
    default Stream<IModule> getDescendantsIncludingSelf() {
        return Streams.concat(Stream.of(this),
                getChildren().stream().flatMap(m -> m.getDescendantsIncludingSelf()));
    }
    
    /**
     * @param context
     *      the context to look up modules in
     * 
     * @return
     *      all the modules that are descendent from this module, including this module itself
     */
    default Stream<IModule> getDescendantsIncludingSelf(Context context) {
        return Streams.concat(Stream.of(this),
                context.getScopeGraph(getId()).getChildIds().stream()
                .flatMap(s -> context.getModuleUnchecked(s).getDescendantsIncludingSelf(context)));
    }
    
    /**
     * Executes the given consumer on each descendant of this module, excluding this module itself.
     * The consumer will be called in a depth first fashion.
     * 
     * @param context
     *      the context to look up modules in
     * @param consumer
     *      the consumer to execute on each module
     */
    default void getDescendants(Context context, Consumer<IModule> consumer) {
        IMInternalScopeGraph<Scope, ITerm, ITerm> graph = context.getScopeGraph(getId());
        if (graph == null) {
            System.err.println("Unable to get children of " + this + ": state is null!");
            return;
        }
        for (String childId : graph.getChildIds()) {
            context.getModuleUnchecked(childId).getDescendantsIncludingSelf(context, consumer);
        }
    }
    
    /**
     * Executes the given consumer on each descendant of this module, including this module itself.
     * The consumer will be called in a depth first fashion.
     * 
     * @param context
     *      the context to look up modules in
     * @param consumer
     *      the consumer to execute on each module
     */
    default void getDescendantsIncludingSelf(Context context, Consumer<IModule> consumer) {
        consumer.accept(this);
        IMInternalScopeGraph<Scope, ITerm, ITerm> graph = context.getScopeGraph(getId());
        if (graph == null) {
            System.err.println("Unable to get children of " + this + ": state is null!");
            return;
        }
        
        for (String childId : graph.getChildIds()) {
            context.getModuleUnchecked(childId).getDescendantsIncludingSelf(context, consumer);
        }
    }
    
    /**
     * Creates a child module and state.
     * <p>
     * NOTE: If resetDependencies is false, dependencies are neither reset nor transferred. The
     * dependencies reported by the context will be the old ones, if no new ones are created.
     * 
     * @param name
     *      the name of the child module
     * @param canExtend
     *      the list of scopes from this module and parents that the child can extend, in the
     *      order they are encountered in the rule
     * @param constraint
     *      the constraint which caused this modules creation
     *      (TODO IMPORTANT substitute scopes)
     *      (TODO does this preserve declaration (references) correctly?)
     * @param transferDependencies
     *      if true, dependencies are transferred. Otherwise, new dependencies are created
     * 
     * @return
     *      the child
     */
    IModule createChild(String name, List<Scope> canExtend, IConstraint constraint, boolean transferDependencies);
    
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
     * Gets the child with the given name if it is allowed by the incremental manager. If no child
     * with the given name exists or if the child may not be transferred, this method returns null.
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
    default IModule getChildIfAllowed(String name, List<Scope> canExtend, IConstraint constraint) {
        IModule oldModule;
        try {
            oldModule = getChild(name);
        } catch (ModuleDelayException ex) {
            return null;
        }
        if (oldModule == null || !context().getIncrementalManager().allowTransferChild(oldModule, constraint)) return null;
        
        context().transferModule(oldModule, true);
        oldModule.getScopeGraph().substitute(canExtend);
        //TODO We potentially need to replace some of the old arguments with new ones in the old module results?
        oldModule.setInitialization(constraint);
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
     * Removes the given module as child of this module.
     * 
     * @param module
     *      the module
     */
    default void removeChild(IModule module) {
        getScopeGraph().removeChild(module);
    }
    
    // --------------------------------------------------------------------------------------------
    // Initialization
    // --------------------------------------------------------------------------------------------
    
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
     * @see Context#getState(IModule)
     */
    default IMState getCurrentState() {
        IMState state = Context.context().getState(this);
        if (state == null) System.err.println("State of " + this + " is null!"); //TODO Remove
        return state;
    }
    
    // --------------------------------------------------------------------------------------------
    // Dependencies
    // --------------------------------------------------------------------------------------------
    
    /**
     * @return
     *      the name dependencies of this module
     */
    default Dependencies dependencies() {
        return context().getDependencies(getId());
    }
    
    /**
     * NOTE: This value is not cached and is computed from the dependencies each time this method
     * is called.
     * 
     * @return
     *      for each dependency, the query with details 
     */
    default SetMultimap<String, QueryDependencyDetail> reverseQueries() {
        SetMultimap<String, QueryDependencyDetail> details = MultimapBuilder.hashKeys().hashSetValues().build();
        for (Entry<String, Dependency> entry : dependencies().getDependencies().entries()) {
            QueryDependencyDetail detail = entry.getValue().getDetails(QueryDependencyDetail.class);
            if (detail == null) continue;
            details.put(entry.getKey(), detail);
        }
        return details;
    }
    
    /**
     * NOTE: This value is not cached and is computed from the dependencies each time this method
     * is called.
     * 
     * @return
     *      a map mapping a query with details to the modules that it depends on
     */
    default SetMultimap<QueryDependencyDetail, String> queries() {
        SetMultimap<QueryDependencyDetail, String> details = MultimapBuilder.hashKeys().hashSetValues().build();
        for (Entry<String, Dependency> entry : dependencies().getDependencies().entries()) {
            QueryDependencyDetail detail = entry.getValue().getDetails(QueryDependencyDetail.class);
            if (detail == null) continue;
            details.put(detail, entry.getKey());
        }
        return details;
    }
    
    /**
     * The aggregated set of all dependencies based on all the queries in this module.
     * 
     * @return
     *      the dependencies of this module
     */
    default Set<? extends IModule> getDependencies() {
        return dependencies().getModuleDependencies();
    }
    
    default Set<String> getDependencyIds() {
        return dependencies().getModuleDependencyIds();
    }
    
    // --------------------------------------------------------------------------------------------
    // Other
    // --------------------------------------------------------------------------------------------
    
    /**
     * Creates a copy of this module, but not it's state.
     * 
     * Please note that the created copy is not added to the context.
     * 
     * @return
     *      the copy, not added to the state
     * 
     * @deprecated
     *      Since modules have no <b>important</b> stateful fields, there is no need to ever copy
     *      one. Instead, a copy should be made of the state.
     */
    @Deprecated
    IModule copy();
}
