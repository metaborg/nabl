package mb.statix.taico.scopegraph;

import java.util.Collection;
import java.util.Map;

import mb.statix.taico.module.IModule;
import mb.statix.taico.util.IOwnable;

public interface ITrackingScopeGraph<S extends IOwnable, V, L, R> extends IMInternalScopeGraph<S, V, L, R> {
    Map<S, L> getTrackedEdges();
    
    Map<S, R> getTrackedData();
    
    Map<IModule, Map<S, L>> aggregateTrackedEdges();
    
    Map<IModule, Map<S, R>> aggregateTrackedData();
    
    /**
     * The directly reached modules, including their children.
     * 
     * @return
     *      all modules that were reached by this tracking graph
     */
    Collection<? extends IModule> getReachedModules();
    
    @Override
    Collection<? extends ITrackingScopeGraph<S, V, L, R>> getChildren();
}
