package mb.statix.taico.solver.query;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import mb.nabl2.terms.ITerm;

/**
 * Interface for storing query details.
 * 
 * @param <S>
 *      the type of scopes
 * @param <L>
 *      the type of labels
 */
public interface IQueryDetails<S, L> {
    Map<String, Map<S, L>> getRelevantEdges();
    
    Map<String, Map<S, L>> getRelevantData();
    
    Collection<String> getReachedModules();
    
    List<ITerm> getQueryResult();
}
