package mb.statix.taico.module;

import java.util.Set;
import java.util.stream.StreamSupport;

import mb.nabl2.terms.ITerm;
import mb.statix.solver.constraint.CResolveQuery;
import mb.statix.taico.scopegraph.IMInternalScopeGraph;
import mb.statix.taico.scopegraph.IOwnableScope;
import mb.statix.taico.scopegraph.IOwnableTerm;
import mb.statix.taico.solver.MState;
import mb.statix.taico.solver.query.QueryDetails;

/**
 * Interface to represent a module.
 */
public interface IModule {
    /**
     * @return
     *      the unique identifier for this module
     */
    String getId();
    
    /**
     * @return
     *      the parent of this module, or null if this is the top level module
     */
    IModule getParent();
    
    /**
     * @return
     *      the children of this module
     */
    Set<IModule> getChildren();
    
    /**
     * Returns the mutable scope graph belonging to this module. Additions can be made to the
     * returned scope graph, so consecutive calls can yield different results.
     * 
     * @return
     *      the scope graph of this module
     */
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
     * 
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
    
    /**
     * Adds a query with its resolution details to determine the dependencies.
     * 
     * @param query
     *      the constraint representing the query
     * @param details
     *      the details relevant for dependencies related to this query
     */
    void addQuery(CResolveQuery query, QueryDetails<IOwnableTerm, ITerm, ITerm> details);
    
    
    /**
     * The aggregated set of all dependencies based on all the queries in this module.
     * 
     * @return
     *      the dependencies of this module
     */
    Set<? extends IModule> getDependencies();
    
    //Set<IQuery<IOwnableTerm, ITerm, ITerm, ITerm>> queries();
    
}
