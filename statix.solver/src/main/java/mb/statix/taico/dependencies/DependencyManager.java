package mb.statix.taico.dependencies;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import mb.statix.taico.dependencies.affect.IDataAdditionAffect;
import mb.statix.taico.dependencies.affect.IDataRemovalOrChangeAffect;
import mb.statix.taico.dependencies.affect.IEdgeAdditionAffect;
import mb.statix.taico.dependencies.affect.IEdgeRemovalAffect;
import mb.statix.taico.ndependencies.observer.IDependencyObserver;
import mb.statix.taico.util.TOverrides;

public class DependencyManager<D extends Dependencies> implements Serializable, Iterable<Entry<String, D>> {
    private static final long serialVersionUID = 1L;
    
    private final Function<String, D> creator;
    private final Map<String, D> map = TOverrides.hashMap();
    
    private List<IDependencyObserver> observers = new ArrayList<>();
    
    private IEdgeAdditionAffect edgeAdd;
    private IEdgeRemovalAffect edgeRemove;
    private IDataAdditionAffect dataAdd;
    private IDataRemovalOrChangeAffect dataRemoveOrChange;
    
    public DependencyManager(Function<String, D> creator) {
        if (!(creator instanceof Serializable)) throw new IllegalArgumentException("The creator function needs to be serializable!");
        this.creator = creator;
    }
    
    /**
     * @param moduleId
     *      the id of the module
     * 
     * @return
     *      true if dependencies have been requested for this module before, false otherwise
     */
    public boolean hasDependencies(String moduleId) {
        return map.containsKey(moduleId);
    }
    
    /**
     * @param moduleId
     *      the id of the module to get dependencies of
     * 
     * @return
     *      the dependencies of the given module
     */
    public D getDependencies(String moduleId) {
        //TODO We might want to switch to not automatically creating dependencies
        return map.computeIfAbsent(moduleId, k -> creator.apply(moduleId));
    }
    
    /**
     * @param moduleId
     *      the id of the module
     * @param dependencies
     *      the dependencies to set
     * 
     * @return
     *      the dependencies that were replaced, null if this is the first time dependencies are
     *      set for this module
     */
    public D setDependencies(String moduleId, D dependencies) {
        D old;
        if ((old = map.putIfAbsent(moduleId, dependencies)) != null && !dependencies.isCopyOf(old)) {
            System.err.println("Replacing dependencies of " + moduleId + " with new dependencies!");
            for (IDependencyObserver observer : observers) {
                observer.removeDependencies(old.getDependencies().values());
                for (Dependency dependency : dependencies.getDependencies().values()) {
                    observer.onDependencyAdded(dependency);
                }
            }
            
            return old;
        }
        return null;
    }
    
    /**
     * Resets the dependencies of the given module.
     * 
     * @param moduleId
     *      the id of the module
     * 
     * @return
     *      the new dependencies
     */
    public D resetDependencies(String moduleId) {
        D deps = getDependencies(moduleId);
        for (IDependencyObserver observer : observers) {
            observer.removeDependencies(deps.getDependencies().values());
        }
        deps.clear();
        return deps;
    }
    
    @Override
    public Iterator<Entry<String, D>> iterator() {
        return map.entrySet().iterator();
    }
    
    /**
     * NOTE: Changes to the returned set write through to the manager.
     * 
     * @return
     *      a set with the ids of all the modules in the manager
     */
    public Set<String> modules() {
        return map.keySet();
    }
    
    /**
     * Changes to the returned collection write through to the manager.
     * 
     * @return
     *      a collection with all the dependencies in the manager
     */
    public Collection<D> dependencies() {
        return map.values();
    }
    
    // -------------------------------------------------------------------------------------------
    // Observers
    // -------------------------------------------------------------------------------------------
    
    public List<IDependencyObserver> getObservers() {
        return observers;
    }
    
    public void registerObservers(Iterable<IDependencyObserver> observers) {
        for (IDependencyObserver observer : observers) {
            registerObserver(observer);
        }
    }
    
    public void registerObserver(IDependencyObserver observer) {
        observers.add(observer);
        
        //Check affects
        updateAffect(observer);
    }
    
    public void clearObservers() {
        observers.clear();
        edgeAdd = null;
        edgeRemove = null;
        dataAdd = null;
        dataRemoveOrChange = null;
    }

    public void onDependencyAdded(Dependency dependency) {
        for (IDependencyObserver observer : observers) {
            observer.onDependencyAdded(dependency);
        }
    }
    
    /**
     * Resends all the dependencies to the observers.
     */
    public void refreshObservers() {
        for (D dependencies : map.values()) {
            for (Dependency dependency : dependencies.getDependencies().values()) {
                for (IDependencyObserver observer : observers) {
                    observer.onDependencyAdded(dependency);
                }
            }
        }
    }
    
    // -------------------------------------------------------------------------------------------
    // Affect
    // -------------------------------------------------------------------------------------------
    
    private void updateAffect(IDependencyObserver observer) {
        if (observer instanceof IEdgeAdditionAffect) {
            IEdgeAdditionAffect newEdgeAdd = (IEdgeAdditionAffect) observer;
            if (edgeAdd == null || newEdgeAdd.edgeAdditionAffectScore() > edgeAdd.edgeAdditionAffectScore()) {
                edgeAdd = newEdgeAdd;
            }
        }
        
        if (observer instanceof IEdgeRemovalAffect) {
            IEdgeRemovalAffect newEdgeRemove = (IEdgeRemovalAffect) observer;
            if (edgeRemove == null || newEdgeRemove.edgeRemovalAffectScore() > edgeRemove.edgeRemovalAffectScore()) {
                edgeRemove = newEdgeRemove;
            }
        }
        
        if (observer instanceof IDataAdditionAffect) {
            IDataAdditionAffect newDataAdd = (IDataAdditionAffect) observer;
            if (dataAdd == null || newDataAdd.dataAdditionAffectScore() > dataAdd.dataAdditionAffectScore()) {
                dataAdd = newDataAdd;
            }
        }
        
        if (observer instanceof IDataRemovalOrChangeAffect) {
            IDataRemovalOrChangeAffect newDataRemoveOrChange = (IDataRemovalOrChangeAffect) observer;
            if (dataRemoveOrChange == null || newDataRemoveOrChange.dataRemovalOrChangeAffectScore() > dataRemoveOrChange.dataRemovalOrChangeAffectScore()) {
                dataRemoveOrChange = newDataRemoveOrChange;
            }
        }
    }
    
    public IEdgeAdditionAffect edgeAddition() {
        return edgeAdd;
    }
    
    public IEdgeRemovalAffect edgeRemoval() {
        return edgeRemove;
    }
    
    public IDataAdditionAffect dataAddition() {
        return dataAdd;
    }
    
    public IDataRemovalOrChangeAffect dataRemoveOrChange() {
        return dataRemoveOrChange;
    }
}
