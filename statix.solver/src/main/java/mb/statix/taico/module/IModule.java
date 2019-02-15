package mb.statix.taico.module;

import java.util.Set;

import mb.statix.taico.paths.IQuery;
import mb.statix.taico.scopegraph.IMScopeGraph;
import mb.statix.taico.util.IOwnable;

/**
 * Interface to represent a module.
 */
public interface IModule<V extends IOwnable<V, L, R>, L, R> {
    String getId();
    
    Set<IQuery<V, L, R>> queries();
    
    IModule<V, L, R> getParent();
    
    Set<IModule<V, L, R>> getChildren();
    
    IMScopeGraph<V, L, R> getScopeGraph();
}
