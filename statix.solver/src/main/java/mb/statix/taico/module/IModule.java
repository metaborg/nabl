package mb.statix.taico.module;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

import mb.nabl2.terms.ITerm;
import mb.statix.solver.constraint.CResolveQuery;
import mb.statix.taico.paths.IQuery;
import mb.statix.taico.scopegraph.IMInternalScopeGraph;
import mb.statix.taico.scopegraph.IOwnableScope;
import mb.statix.taico.scopegraph.IOwnableTerm;
import mb.statix.taico.solver.MState;

/**
 * Interface to represent a module.
 */
public interface IModule {
    String getId();
    
    IModule getParent();
    
    Set<IModule> getChildren();
    
    IMInternalScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> getScopeGraph();
    
    /**
     * @return
     *      all the modules that are descendent from this module
     */
    default Iterable<IModule> getDescendants() {
        return getChildren().stream()
                .flatMap(m -> StreamSupport.stream(m.getDescendants().spliterator(), false))
                ::iterator;
    }
    
    /**
     * @param canExtend
     *      the scopes from this module and parents that the child can extend
     * @return
     *      the child
     */
    IModule createChild(Iterable<IOwnableScope> canExtend);
    
    /**
     * @return
     *      the state of this module
     */
    MState getCurrentState();
    
    /**
     * Method used once when a module state has been constructed.
     * 
     * @param state
     *      the state of this module
     *      
     * @throws IllegalStateException
     *      If the state of this module has already been set.
     */
    void setCurrentState(MState state);
    
    //Dependencies
    Map<CResolveQuery, Collection<IModule>> getDependencies();
    
    void addQuery(CResolveQuery query, Collection<IModule> modules);
    
    Set<IQuery<IOwnableTerm, ITerm, ITerm, ITerm>> queries();
    
}
