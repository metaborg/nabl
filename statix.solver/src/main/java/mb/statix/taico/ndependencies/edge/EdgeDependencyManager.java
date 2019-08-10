package mb.statix.taico.ndependencies.edge;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;

import mb.nabl2.terms.ITerm;
import mb.nabl2.util.Tuple2;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.taico.dependencies.Dependency;
import mb.statix.taico.dependencies.details.SimpleQueryDependencyDetail;
import mb.statix.taico.util.LightWeightHashTrieRelation3;

public class EdgeDependencyManager implements IEdgeDependencyManager<ITerm>, Serializable {
    private static final long serialVersionUID = 1L;
    
    //The sparser the map is populated, the more sense it makes to go with the tuple approach instead.
    private LightWeightHashTrieRelation3.Transient<Scope, ITerm, Dependency> edgeDependencies = LightWeightHashTrieRelation3.Transient.of();
    
    @Override
    public synchronized Iterable<Dependency> getDependencies(Scope scope) {
        return edgeDependencies.get(scope).stream().map(Tuple2::_2)::iterator;
    }
    
    @Override
    public synchronized Set<Dependency> getDependencies(Scope scope, ITerm label) {
        return edgeDependencies.get(scope, label);
    }
    
    @Override
    public boolean addDependency(Scope scope, ITerm label, Dependency dependency) {
        return edgeDependencies.put(scope, label, dependency);
    }
    
    @Override
    public void removeDependencies(Collection<Dependency> dependencies) {
        for (Dependency dependency : dependencies) {
            SimpleQueryDependencyDetail detail = dependency.getDetails(SimpleQueryDependencyDetail.class);
            for (Entry<Scope, ITerm> entry : detail.getRelevantEdges().entries()) {
                edgeDependencies.remove(entry.getKey(), entry.getValue(), dependency);
            }
        }
    }
    
    // --------------------------------------------------------------------------------------------
    // Serialization
    // --------------------------------------------------------------------------------------------
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        LightWeightHashTrieRelation3.Immutable<Scope, ITerm, Dependency> frozen = edgeDependencies.freeze();
        out.writeObject(frozen);
        this.edgeDependencies = frozen.melt();
    }
    
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        LightWeightHashTrieRelation3.Immutable<Scope, ITerm, Dependency> frozen =
                (LightWeightHashTrieRelation3.Immutable<Scope, ITerm, Dependency>) in.readObject();
        this.edgeDependencies = frozen.melt();
    }
}
