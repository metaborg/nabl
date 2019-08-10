package mb.statix.taico.ndependencies.name;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

import mb.statix.scopegraph.terms.Scope;
import mb.statix.taico.dependencies.Dependency;
import mb.statix.taico.dependencies.details.NameDependencyDetail;
import mb.statix.taico.dependencies.details.QueryDependencyDetail;
import mb.statix.taico.name.NameAndRelation;
import mb.statix.taico.util.LightWeightHashTrieRelation3;

public class NameDependencyManager implements INameDependencyManager, Serializable {
    private static final long serialVersionUID = 1L;
    
    //TODO OPTIMIZATION Might benefit from being a MapMultimap instead
    private LightWeightHashTrieRelation3.Transient<NameAndRelation, Scope, Dependency> nameDependencies = LightWeightHashTrieRelation3.Transient.of();
    
    @Override
    public synchronized Set<Dependency> getDependencies(NameAndRelation nameRel, Scope scope) {
        return nameDependencies.get(nameRel, scope);
    }
    
    @Override
    public synchronized boolean addDependency(NameAndRelation nameRel, Scope scope, Dependency dependency) {
        return nameDependencies.put(nameRel, scope, dependency);
    }
    
    @Override
    public synchronized void removeDependencies(Collection<Dependency> dependencies) {
        for (Dependency dependency : dependencies) {
            NameAndRelation nar = getNameFromDependency(dependency);
            for (Scope scope : getScopesFromDependency(dependency)) {
                nameDependencies.remove(nar, scope, dependency);
            }
        }
    }
    
    // --------------------------------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------------------------------
    
    private NameAndRelation getNameFromDependency(Dependency dependency) {
        NameDependencyDetail detail = dependency.getDetails(NameDependencyDetail.class);
        return detail.toNameAndRelation();
    }
    
    private Set<Scope> getScopesFromDependency(Dependency dependency) {
        QueryDependencyDetail detail = dependency.getDetails(QueryDependencyDetail.class);
        return detail.getDataScopes();
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
