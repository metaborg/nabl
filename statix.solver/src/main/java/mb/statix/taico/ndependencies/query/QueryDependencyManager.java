package mb.statix.taico.ndependencies.query;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

import mb.nabl2.terms.ITerm;
import mb.nabl2.util.Tuple2;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.path.IStep;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.taico.dependencies.Dependency;
import mb.statix.taico.dependencies.details.QueryResultDependencyDetail;
import mb.statix.taico.util.LightWeightHashTrieRelation3;

public class QueryDependencyManager implements IQueryDependencyManager, Serializable {
    private static final long serialVersionUID = 1L;
    
    //The sparser the map is populated, the more sense it makes to go with the tuple approach instead.
    private LightWeightHashTrieRelation3.Transient<Scope, ITerm, Dependency> queryDependencies = LightWeightHashTrieRelation3.Transient.of();
    
    @Override
    public synchronized Iterable<Dependency> getDependencies(Scope scope) {
        return queryDependencies.get(scope).stream().map(Tuple2::_2)::iterator;
    }
    
    @Override
    public synchronized Set<Dependency> getDependencies(Scope scope, ITerm label) {
        return queryDependencies.get(scope, label);
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
    public int dataRemovalOrChangeAffectScore() {
        return 0; //O(n) (small n) lookup, EXACT
    }
    
    // --------------------------------------------------------------------------------------------
    // Serialization
    // --------------------------------------------------------------------------------------------
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        LightWeightHashTrieRelation3.Immutable<Scope, ITerm, Dependency> frozen = queryDependencies.freeze();
        out.writeObject(frozen);
        this.queryDependencies = frozen.melt();
    }
    
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        LightWeightHashTrieRelation3.Immutable<Scope, ITerm, Dependency> frozen =
                (LightWeightHashTrieRelation3.Immutable<Scope, ITerm, Dependency>) in.readObject();
        this.queryDependencies = frozen.melt();
    }
}
