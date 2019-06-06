package mb.statix.taico.solver.query;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.Multimap;

import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.LabelWF;

/**
 * Interface for storing query details.
 * 
 * @param <S>
 *      the type of scopes
 * @param <L>
 *      the type of labels
 * @param <D>
 *      the type of data
 */
public interface IQueryDetails<S extends D, L, D> extends Serializable {
    Multimap<S, LabelWF<L>> getRelevantEdges();
    
    Multimap<S, LabelWF<L>> getRelevantData();
    
    DataWF<D> getDataWellFormedness();
    
    Collection<String> getReachedModules();
    
    List<ITerm> getQueryResult();
}
