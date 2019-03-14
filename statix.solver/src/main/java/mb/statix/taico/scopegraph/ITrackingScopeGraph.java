package mb.statix.taico.scopegraph;

import java.util.Collection;
import java.util.Map;

import mb.statix.taico.module.IModule;
import mb.statix.taico.util.IOwnable;

public interface ITrackingScopeGraph<S extends IOwnable, V, L, R> extends IMInternalScopeGraph<S, V, L, R> {
    /**
     * A map with the scopes and the labels that were requested as data on the scope graph
     * of this tracker alone. This does not include any child or parent trackers.
     * 
     * @return
     *      the map of tracked edges
     */
    Map<S, L> getTrackedEdges();
    
    /**
     * A map with the scopes and the relations that were requested as data on the scope graph
     * of this tracker alone. This does not include any child or parent trackers.
     * 
     * @return
     *      the map of tracked data
     */
    Map<S, R> getTrackedData();
    
    /**
     * A map with, per module, the scopes and the labels that were requested as edges on this
     * tracker and all relevant child and parent trackers.
     * 
     * @return
     *      the map of all requested edges
     */
    Map<IModule, Map<S, L>> aggregateTrackedEdges();
    
    /**
     * A map with, per module, the scopes and the relations that were requested as data on this
     * tracker and all relevant child and parent trackers.
     * 
     * @return
     *      the map of all requested data
     */
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
