package mb.statix.taico.ndependencies.name;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;

import mb.statix.scopegraph.terms.Scope;
import mb.statix.taico.dependencies.Dependency;
import mb.statix.taico.dependencies.details.NameDependencyDetail;
import mb.statix.taico.name.NameAndRelation;

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
    public void removeDependencies(Collection<Dependency> dependencies) {
        for (Dependency dependency : dependencies) {
            nameDependencies.remove(getNameFromDependency(dependency), dependency);
        }
    }
    
    private NameAndRelation getNameFromDependency(Dependency dependency) {
        NameDependencyDetail detail = dependency.getDetails(NameDependencyDetail.class);
        return detail.toNameAndRelation();
    }
}
