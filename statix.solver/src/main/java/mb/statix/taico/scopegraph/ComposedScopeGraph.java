package mb.statix.taico.scopegraph;

import java.util.List;

import io.usethesource.capsule.Set.Immutable;
import mb.statix.taico.util.IOwnable;

public class ComposedScopeGraph<V extends IOwnable<V, L, R>, L, R> implements IMScopeGraph<V, L, R> {

    //TODO owner is the global module
    @Override
    public Immutable<L> getLabels() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Immutable<R> getRelations() {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public Iterable<V> getScopes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<IEdge<V, L, V>> getEdges(V scope, L label) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<IEdge<V, R, List<V>>> getData(V scope, R label) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean addEdge(V sourceScope, L label, V targetScope) {
        return sourceScope.getOwner().getScopeGraph().addEdge(sourceScope, label, targetScope);
    }

    @Override
    public boolean addDatum(V scope, R relation, Iterable<V> datum) {
        
        // TODO Auto-generated method stub
        return false;
    }
    
}
