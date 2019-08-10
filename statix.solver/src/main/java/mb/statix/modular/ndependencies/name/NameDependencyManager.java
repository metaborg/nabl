package mb.statix.modular.ndependencies.name;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

import mb.statix.modular.dependencies.Dependency;
import mb.statix.modular.dependencies.details.QueryDependencyDetail;
import mb.statix.modular.name.NameAndRelation;
import mb.statix.modular.util.LightWeightHashTrieRelation3;
import mb.statix.scopegraph.terms.Scope;

public class NameDependencyManager implements INameDependencyManager, Serializable {
    private static final long serialVersionUID = 1L;
    
    //TODO OPTIMIZATION Might benefit from being a MapMultimap instead
    private transient LightWeightHashTrieRelation3.Transient<NameAndRelation, Scope, Dependency> nameDependencies = LightWeightHashTrieRelation3.Transient.of();
    
    @Override
    public synchronized Set<Dependency> getDependencies(NameAndRelation nameRel, Scope scope) {
        return nameDependencies.get(nameRel, scope);
    }
    
    @Override
    public synchronized boolean addDependency(NameAndRelation nameRel, Scope scope, Dependency dependency) {
        return nameDependencies.put(nameRel, scope, dependency);
    }
    
    @Override
    public void removeDependencies(Collection<Dependency> dependencies) {
        for (Dependency dependency : dependencies) {
            NameAndRelation nar = INameDependencyManager.getNameFromDependency(dependency);
            Set<Scope> scopes = getScopesFromDependency(dependency);
            synchronized (this) {
                for (Scope scope : scopes) {
                    nameDependencies.remove(nar, scope, dependency);
                }
            }
        }
    }
    
    @Override
    public void onDependencyAdded(Dependency dependency) {
        NameAndRelation nar = INameDependencyManager.getNameFromDependency(dependency);
        Set<Scope> scopes = getScopesFromDependency(dependency);
        synchronized (this) {
            for (Scope scope : scopes) {
                nameDependencies.put(nar, scope, dependency);
            }
        }
    }
    
    // --------------------------------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------------------------------
    
    private static Set<Scope> getScopesFromDependency(Dependency dependency) {
        QueryDependencyDetail detail = dependency.getDetails(QueryDependencyDetail.class);
        return detail.getDataScopes();
    }
    
    // --------------------------------------------------------------------------------------------
    // Affect
    // --------------------------------------------------------------------------------------------
    
    @Override
    public int dataAdditionAffectScore() {
        return 0; //O(1) lookup, exact
    }
    
    @Override
    public int dataRemovalOrChangeAffectScore() {
        return 2; //O(1) lookup, but sometimes reports affected when this is not the case
    }
    
    // --------------------------------------------------------------------------------------------
    // Serialization
    // --------------------------------------------------------------------------------------------
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        LightWeightHashTrieRelation3.Immutable<NameAndRelation, Scope, Dependency> frozen = nameDependencies.freeze();
        out.writeObject(frozen);
        this.nameDependencies = frozen.melt();
    }
    
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        LightWeightHashTrieRelation3.Immutable<NameAndRelation, Scope, Dependency> frozen =
                (LightWeightHashTrieRelation3.Immutable<NameAndRelation, Scope, Dependency>) in.readObject();
        this.nameDependencies = frozen.melt();
    }
}
