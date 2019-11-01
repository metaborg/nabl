package mb.statix.modular.ndependencies.name;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;

import mb.statix.modular.dependencies.Dependency;
import mb.statix.modular.name.NameAndRelation;
import mb.statix.scopegraph.terms.Scope;

public class SimpleNameDependencyManager implements INameDependencyManager, Serializable {
    private static final long serialVersionUID = 1L;
    
    private SetMultimap<NameAndRelation, Dependency> nameDependencies = MultimapBuilder.hashKeys().hashSetValues().build();
    
    public synchronized Set<Dependency> getDependencies(NameAndRelation nameRel) {
        return nameDependencies.get(nameRel);
    }
    
    public synchronized boolean addDependency(NameAndRelation nameRel, Dependency dependency) {
        return nameDependencies.put(nameRel, dependency);
    }
    
    @Override
    public Set<Dependency> getDependencies(NameAndRelation nameRel, Scope scope) {
        return getDependencies(nameRel);
    }
    
    @Override
    public boolean addDependency(NameAndRelation nameRel, Scope scope, Dependency dependency) {
        return addDependency(nameRel, dependency);
    }
    
    @Override
    public synchronized void removeDependencies(Collection<Dependency> dependencies) {
        for (Dependency dependency : dependencies) {
            NameAndRelation nar = INameDependencyManager.getNameFromDependency(dependency);
            if (nar == null) continue;
            nameDependencies.remove(nar, dependency);
        }
    }
    
    @Override
    public void onDependencyAdded(Dependency dependency) {
        NameAndRelation nar = INameDependencyManager.getNameFromDependency(dependency);
        if (nar == null) return;
        addDependency(nar, dependency);
    }
    
    // --------------------------------------------------------------------------------------------
    // Affect
    // --------------------------------------------------------------------------------------------
    
    @Override
    public int dataNameAdditionAffectScore() {
        return 1; //O(1) lookup, SOMETIMES reports affected when this is not the case
    }
    
    @Override
    public int dataNameRemovalOrChangeAffectScore() {
        return 3; //O(1) lookup, OFTEN reports affected when this is not the case
    }
}
