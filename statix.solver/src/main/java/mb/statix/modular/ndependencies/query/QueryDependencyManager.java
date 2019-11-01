package mb.statix.modular.ndependencies.query;

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
import mb.statix.modular.dependencies.details.QueryResultDependencyDetail;
import mb.statix.modular.util.TOverrides;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.path.IStep;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.util.collection.LightWeightHashTrieRelation3;
import mb.statix.util.collection.MapMultimap;

public class QueryDependencyManager implements IQueryDependencyManager, Serializable {
    private static final long serialVersionUID = 1L;
    
    //The sparser the map is populated, the more sense it makes to go with the tuple approach instead.
//    private transient LightWeightHashTrieRelation3.Transient<Scope, ITerm, Dependency> queryDependencies = LightWeightHashTrieRelation3.Transient.of();
    private MapMultimap<Scope, ITerm, Dependency> queryDependencies = TOverrides.mapSetMultimap();
    
    @Override
    public Iterable<Dependency> getDependencies(Scope scope) {
        return queryDependencies.get2(scope).values();
//        return queryDependencies.get(scope).stream().map(Entry::getValue)::iterator;
    }
    
    @Override
    public Set<Dependency> getDependencies(Scope scope, ITerm label) {
        return (Set<Dependency>) queryDependencies.get(scope, label);
    }
    
    @Override
    public boolean addDependency(Scope scope, ITerm label, Dependency dependency) {
        return queryDependencies.put(scope, label, dependency);
    }
    
    @Override
    public void removeDependencies(Collection<Dependency> dependencies) {
        for (Dependency dependency : dependencies) {
            QueryResultDependencyDetail detail = dependency.getDetails(QueryResultDependencyDetail.class);
            for (IResolutionPath<Scope, ITerm, ITerm> path : detail.getPaths()) {
                queryDependencies.remove(path.getPath().getTarget(), path.getLabel(), dependency);
                for (IStep<Scope, ITerm> step : path.getPath()) {
                    queryDependencies.remove(step.getSource(), step.getLabel(), dependency);
                }
            }
        }
    }
    
    // --------------------------------------------------------------------------------------------
    // Affect
    // --------------------------------------------------------------------------------------------
    
    @Override
    public int edgeRemovalAffectScore() {
        return 0; //O(n) (small n) lookup, EXACT
    }
    
    @Override
    public int dataNameRemovalOrChangeAffectScore() {
        return 0; //O(n) (small n) lookup, EXACT
    }
    
    // --------------------------------------------------------------------------------------------
    // Serialization
    // --------------------------------------------------------------------------------------------
    
//    private void writeObject(ObjectOutputStream out) throws IOException {
//        out.defaultWriteObject();
//        LightWeightHashTrieRelation3.Immutable<Scope, ITerm, Dependency> frozen = queryDependencies.freeze();
//        out.writeObject(frozen);
//        this.queryDependencies = frozen.melt();
//    }
//    
//    @SuppressWarnings("unchecked")
//    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
//        in.defaultReadObject();
//        LightWeightHashTrieRelation3.Immutable<Scope, ITerm, Dependency> frozen =
//                (LightWeightHashTrieRelation3.Immutable<Scope, ITerm, Dependency>) in.readObject();
//        this.queryDependencies = frozen.melt();
//    }
}
