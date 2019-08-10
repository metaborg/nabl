package mb.statix.modular.solver.completeness;

import static mb.statix.modular.util.TDebug.COMPLETENESS;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import mb.nabl2.terms.ITerm;
import mb.statix.modular.module.IModule;
import mb.statix.modular.solver.ModuleSolver;
import mb.statix.modular.util.Scopes;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.scopegraph.terms.Scope;

public class CompletenessCache {
    private ModuleSolver solver;
    private ConcurrentHashMap<CriticalEdge, Set<String>> incomplete = new ConcurrentHashMap<>();
//    private SetMultimap<CriticalEdge, ModuleConstraintStore> edgeObservers = MultimapBuilder.hashKeys().hashSetValues().build();
    private ConcurrentHashMap<CriticalEdge, Object> cachedEdges = new ConcurrentHashMap<>();
    
    /**
     * @param edge
     * @return
     *      the set of modules that is incomplete
     */
    private Set<String> getOrAddIncomplete(CriticalEdge edge) {
        return incomplete.computeIfAbsent(edge, a -> Collections.synchronizedSet(new HashSet<>()));
    }
    
    /**
     * Called by a module to notify that the its critical edge has been resolved.
     * 
     * @param module
     *      the module for which the edge has been resolved
     * @param edge
     *      the edge that has been resolved
     * 
     * @throws IllegalStateException
     *      If the reported edge is not actually complete in the given module.
     * @throws IllegalStateException
     *      If the reported edge has already been reported as complete by the given module.
     */
    public void notifyComplete(IModule module, CriticalEdge edge) {
        //TODO Remove assertion at some point
        if (!module.getCurrentState().solver().isCompleteSelf(Scopes.getScope(edge.scope()), edge.label())) {
            throw new IllegalStateException("Notification<" + module.getId() + ", " + edge + ">, The reporting module is not completed itself in this edge!");
        }
        
        //Remove the module from incompleteness, explore its children to see if they are incomplete
        replaceWithIncompleteChildren(getOrAddIncomplete(edge), module, edge);
    }
    
    /**
     * Called whenever the given module becomes complete (excluding its children). At this point
     * we are sure that all children that might affect the critical edge have been created.
     * <p>
     * This method will first add all the child incompletenesses (if any) and will then remove
     * the original module from the complete modules. This results in an atomic update of
     * completeness.
     * <p>
     * If this method causes the incomplete set to become empty, then it will trigger all the
     * observers via the store.
     * 
     * @param incomplete
     *      the set of incomplete modules
     * @param module
     *      the module that became complete
     * @param edge
     *      the edge
     */
    private void replaceWithIncompleteChildren(Set<String> incomplete, IModule module, CriticalEdge edge) {
        String ownerId = module.getId();
        if (!incomplete.contains(ownerId)) throw new IllegalStateException("Cannot swap with children if we are already complete!");
        
        module.getCurrentState().solver().computeCompleteness(incomplete, Scopes.getScope(edge.scope()), edge.label());
        
        if (!remove(incomplete, ownerId, edge)) {
            String msg = "When removing " + edge + " for module " + module + ", we found that this edge + module combination was already resolved from the incompleteness!";
            System.err.println(msg);
            throw new IllegalStateException(msg);
        }
    }
    
    /**
     * Removes the given module from the incompleteness, triggering observers if the set becomes
     * empty.
     * 
     * @param incompleteModules
     *      the incomplete modules
     * @param module
     *      the module to remove
     * @param edge
     *      the edge to remove
     * 
     * @return
     *      false if the given module was already not in the set of incomplete modules, true
     *      otherwise
     */
    private boolean remove(Set<String> incompleteModules, String module, CriticalEdge edge) {
        if (!incompleteModules.remove(module)) return false;
        
        //If this edge has been resolved, send the notification to the observers
        if (incompleteModules.isEmpty()) {
            sendNotificationToObservers(edge);
        }
        return true;
    }
    
    private void sendNotificationToObservers(CriticalEdge edge) {
        //TODO IMPORTANT
//        solver.getStore().activateFromEdge(edge, TDebug.DEV_OUT, true);
        
//        //If we were to take over the notification process
//        Set<ModuleConstraintStore> observers;
//        synchronized (edgeObservers) {
//            observers = edgeObservers.removeAll(edge);
//        }
//        
//        //Activate all observers
//        for (ModuleConstraintStore store : observers) {
//            //We first need to active and then, if it is likely that the module is currently not solving, we send a notification
//            if (STORE_DEBUG) System.err.println(getOwner() + ": Delegating activation of edge " + edge + " to " + store);
//
//            store.activateFromEdge(edge, TDebug.DEV_OUT, false); //Activate but don't propagate
//            
//            //Only notify if it is currently not doing anything (probably)
//            final IObserver<ModuleConstraintStore> observer = store.getStoreObserver();
//            if (observer != null && store.activeSize() == 1) {
//                observer.notify(store);
//            }
//        }
    }
    
    /**
     * @param requester
     *      the requester of the completeness request
     * @param scopeTerm
     *      the scope of the edge
     * @param label
     *      the label of the edge
     * 
     * @return
     *      true if the given edge is complete, false otherwise
     */
    public boolean isComplete(IModule requester, ITerm scopeTerm, ITerm label) {
        Scope scope = Scopes.getScope(scopeTerm);
        if (COMPLETENESS) System.err.println("CompletenessCache of " + getOwner() + " got query for " + scope + "-" + label + ".");
        
        CriticalEdge edge = CriticalEdge.of(scope, label);
        
        //If we have not seen this edge before, compute and then cache its result
        ensureComputed(requester, label, scope, edge);
        
        //Determine if the edge is complete or not
        return incomplete.get(edge).isEmpty(); //Should be null pointer safe, since we have just added it in the cacheEdgeResult method
    }

    /**
     * @param requester
     *      the requester
     * @param label
     *      the label
     * @param scope
     *      the scope
     * @param edge
     *      the edge
     */
    private void ensureComputed(IModule requester, ITerm label, Scope scope, CriticalEdge edge) {
        if (cachedEdges.containsKey(edge)) return;
        
        Object mutex = new Object();
        synchronized (mutex) {
            Object oldMutex = cachedEdges.computeIfAbsent(edge, k -> mutex);
            if (oldMutex == mutex) {
                computeEdgeResult(requester, edge, scope, label);
            } else {
                synchronized (oldMutex) {}
            }
        }
    }
    
    /**
     * Computes and caches the result for the given critical edge.
     * 
     * @param requester
     *      the requester of the edge
     * @param scope
     *      the scope of the edge
     * @param label
     *      the label of the edge
     * 
     * @throws IllegalStateException
     *      If the module owning the given scope cannot be retrieved.
     * @throws IllegalStateException
     *      If the owner of the scope is not the owner of this cache.
     */
    private void computeEdgeResult(IModule requester, CriticalEdge edge, Scope scope, ITerm label) {
        IModule scopeOwner = Scopes.getOwner(scope, requester);
        if (scopeOwner == null) throw new IllegalStateException("Encountered scope without owning module: " + scope);
        if (scopeOwner != getOwner()) throw new IllegalStateException("The isComplete request was not redirected to the correct cache (targeted to " + getOwner() + ", scope " + scope + ")");

        //Note that this method call can cause the set stored in #incomplete to change
        solver.computeCompleteness(getOrAddIncomplete(edge), scope, label);
    }
    
    private IModule getOwner() {
        return solver.getOwner();
    }
}
