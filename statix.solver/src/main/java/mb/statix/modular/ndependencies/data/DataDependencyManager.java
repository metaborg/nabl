package mb.statix.modular.ndependencies.data;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;

import mb.nabl2.terms.ITerm;
import mb.nabl2.util.Tuple2;
import mb.statix.modular.dependencies.Dependency;
import mb.statix.modular.dependencies.details.NameDependencyDetail;
import mb.statix.modular.dependencies.details.SimpleQueryDependencyDetail;
import mb.statix.modular.util.TOverrides;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.util.collection.LightWeightHashTrieRelation3;
import mb.statix.util.collection.MapMultimap;

public class DataDependencyManager implements IDataDependencyManager<ITerm>, Serializable {
    private static final long serialVersionUID = 1L;
    
    //The sparser the map is populated, the more sense it makes to go with the tuple approach instead.
//    private transient LightWeightHashTrieRelation3.Transient<Scope, ITerm, Dependency> dataDependencies = LightWeightHashTrieRelation3.Transient.of();
    private MapMultimap<Scope, ITerm, Dependency> dataDependencies = TOverrides.mapSetMultimap();
    private final boolean rejectNames;
    
    public DataDependencyManager(boolean rejectNames) {
        this.rejectNames = rejectNames;
    }
    
    @Override
    public Iterable<Dependency> getDependencies(Scope scope) {
        return dataDependencies.get2(scope).values();
//        return dataDependencies.get(scope).stream().map(Entry::getValue)::iterator;
    }
    
    @Override
    public Set<Dependency> getDependencies(Scope scope, ITerm label) {
        return (Set<Dependency>) dataDependencies.get(scope, label);
    }
    
    @Override
    public synchronized boolean addDependency(Scope scope, ITerm label, Dependency dependency) {
        return dataDependencies.put(scope, label, dependency);
    }
    
    @Override
    public void removeDependencies(Collection<Dependency> dependencies) {
        for (Dependency dependency : dependencies) {
            for (Entry<Scope, ITerm> entry : getData(dependency)) {
                dataDependencies.remove(entry.getKey(), entry.getValue(), dependency);
            }
        }
    }
    
    @Override
    public void onDependencyAdded(Dependency dependency) {
        if (rejectNames && dependency.hasDetails(NameDependencyDetail.class)) return;
        for (Entry<Scope, ITerm> entry : getData(dependency)) {
            dataDependencies.put(entry.getKey(), entry.getValue(), dependency);
        }
    }
    
    // --------------------------------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------------------------------
    
    private static Collection<Entry<Scope, ITerm>> getData(Dependency dependency) {
        SimpleQueryDependencyDetail detail = dependency.getDetails(SimpleQueryDependencyDetail.class);
        return detail.getRelevantData().entries();
    }
    
    // --------------------------------------------------------------------------------------------
    // Affect
    // --------------------------------------------------------------------------------------------
    
    @Override
    public int dataNameAdditionAffectScore() {
        return 0; //O(1) lookup, exact
    }
    
    @Override
    public int dataNameRemovalOrChangeAffectScore() {
        return 2; //O(1) lookup, but SOMETIMES reports affected when this is not the case
    }
    
    @Override
    public int dataAdditionAffectScore() {
        return 0; //O(1) lookup, exact
    }
    
    @Override
    public int dataRemovalAffectScore() {
        return 2; //O(1) lookup, but SOMETIMES reports affected when this is not the case
    }
    
    // --------------------------------------------------------------------------------------------
    // Serialization
    // --------------------------------------------------------------------------------------------
    
//    private void writeObject(ObjectOutputStream out) throws IOException {
//        out.defaultWriteObject();
//        LightWeightHashTrieRelation3.Immutable<Scope, ITerm, Dependency> frozen = dataDependencies.freeze();
//        out.writeObject(frozen);
//        this.dataDependencies = frozen.melt();
//    }
//    
//    @SuppressWarnings("unchecked")
//    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
//        in.defaultReadObject();
//        LightWeightHashTrieRelation3.Immutable<Scope, ITerm, Dependency> frozen =
//                (LightWeightHashTrieRelation3.Immutable<Scope, ITerm, Dependency>) in.readObject();
//        this.dataDependencies = frozen.melt();
//    }
}
