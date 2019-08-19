package mb.statix.modular.dependencies;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import mb.nabl2.terms.ITerm;
import mb.statix.modular.dependencies.affect.IDataAdditionAffect;
import mb.statix.modular.dependencies.affect.IDataNameAdditionAffect;
import mb.statix.modular.dependencies.affect.IDataNameRemovalOrChangeAffect;
import mb.statix.modular.dependencies.affect.IDataRemovalAffect;
import mb.statix.modular.dependencies.affect.IEdgeAdditionAffect;
import mb.statix.modular.dependencies.affect.IEdgeRemovalAffect;
import mb.statix.modular.name.NameAndRelation;
import mb.statix.modular.ndependencies.observer.IDependencyObserver;
import mb.statix.modular.util.TOverrides;
import mb.statix.scopegraph.terms.Scope;

public class DependencyManager<D extends Dependencies> implements Serializable, Iterable<Entry<String, D>>,
    IEdgeAdditionAffect, IEdgeRemovalAffect,
    IDataAdditionAffect, IDataRemovalAffect,
    IDataNameAdditionAffect, IDataNameRemovalOrChangeAffect {
    private static final long serialVersionUID = 1L;
    
    private final Function<String, D> creator;
    private final Map<String, D> map = TOverrides.hashMap();
    
    private List<IDependencyObserver> observers = new ArrayList<>();
    
    private IEdgeAdditionAffect edgeAdd;
    private IEdgeRemovalAffect edgeRemove;
    private IDataAdditionAffect dataAdd;
    private IDataRemovalAffect dataRemove;
    private IDataNameAdditionAffect dataNameAdd;
    private IDataNameRemovalOrChangeAffect dataNameRemoveOrChange;
    
    public DependencyManager(Function<String, D> creator) {
        if (!(creator instanceof Serializable)) throw new IllegalArgumentException("The creator function needs to be serializable!");
        this.creator = creator;
    }
    
    public Function<String, D> getCreator() {
        return creator;
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
        dataNameAdd = null;
        dataNameRemoveOrChange = null;
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
        
        if (observer instanceof IDataRemovalAffect) {
            IDataRemovalAffect newDataRemove = (IDataRemovalAffect) observer;
            if (dataRemove == null || newDataRemove.dataRemovalAffectScore() > dataRemove.dataRemovalAffectScore()) {
                dataRemove = newDataRemove;
            }
        }
        
        if (observer instanceof IDataNameAdditionAffect) {
            IDataNameAdditionAffect newDataAdd = (IDataNameAdditionAffect) observer;
            if (dataNameAdd == null || newDataAdd.dataNameAdditionAffectScore() > dataNameAdd.dataNameAdditionAffectScore()) {
                dataNameAdd = newDataAdd;
            }
        }
        
        if (observer instanceof IDataNameRemovalOrChangeAffect) {
            IDataNameRemovalOrChangeAffect newDataRemoveOrChange = (IDataNameRemovalOrChangeAffect) observer;
            if (dataNameRemoveOrChange == null || newDataRemoveOrChange.dataNameRemovalOrChangeAffectScore() > dataNameRemoveOrChange.dataNameRemovalOrChangeAffectScore()) {
                dataNameRemoveOrChange = newDataRemoveOrChange;
            }
        }
    }
    
    @Override
    public Iterable<Dependency> affectedByEdgeAddition(Scope scope, ITerm label) {
        return edgeAdd.affectedByEdgeAddition(scope, label);
    }
    
    @Override
    public Iterable<Dependency> affectedByEdgeRemoval(Scope scope, ITerm label) {
        return edgeRemove.affectedByEdgeRemoval(scope, label);
    }
    
    @Override
    public Iterable<Dependency> affectedByDataAddition(Scope scope, ITerm relation) {
        return dataAdd.affectedByDataAddition(scope, relation);
    }
    
    @Override
    public Iterable<Dependency> affectedByDataRemoval(Scope scope, ITerm relation) {
        return dataRemove.affectedByDataRemoval(scope, relation);
    }
    
    @Override
    public Iterable<Dependency> affectedByDataNameAddition(NameAndRelation nameAndRelation, Scope scope) {
        return dataNameAdd.affectedByDataNameAddition(nameAndRelation, scope);
    }
    
    @Override
    public Iterable<Dependency> affectedByDataNameRemovalOrChange(NameAndRelation nameAndRelation, Scope scope) {
        return dataNameRemoveOrChange.affectedByDataNameRemovalOrChange(nameAndRelation, scope);
    }
    
    @Override
    public int edgeAdditionAffectScore() {
        return edgeAdd == null ? -1 : edgeAdd.edgeAdditionAffectScore();
    }
    
    @Override
    public int edgeRemovalAffectScore() {
        return edgeRemove == null ? -1 : edgeRemove.edgeRemovalAffectScore();
    }
    
    @Override
    public int dataAdditionAffectScore() {
        return dataAdd == null ? -1 : dataAdd.dataAdditionAffectScore();
    }
    
    @Override
    public int dataRemovalAffectScore() {
        return dataRemove == null ? -1 : dataRemove.dataRemovalAffectScore();
    }
    
    @Override
    public int dataNameAdditionAffectScore() {
        return dataNameAdd == null ? -1 : dataNameAdd.dataNameAdditionAffectScore();
    }
    
    @Override
    public int dataNameRemovalOrChangeAffectScore() {
        return dataNameRemoveOrChange == null ? -1 : dataNameRemoveOrChange.dataNameRemovalOrChangeAffectScore();
    }

    public void wipe() {
        this.dataAdd = null;
        this.dataNameAdd = null;
        this.dataNameRemoveOrChange = null;
        this.dataRemove = null;
        this.edgeAdd = null;
        this.edgeRemove = null;
        this.map.clear();
        this.observers.clear();
        this.observers = null;
    }
}
