package mb.statix.taico.scopegraph.diff;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import mb.nabl2.util.collections.IRelation3;
import mb.statix.taico.name.Name;
import mb.statix.taico.name.NameAndRelation;

public interface IScopeGraphDiff<S extends D, L, D> {
    /**
     * @return
     *      a set of all scopes that have been added
     */
    Set<S> getAddedScopes();
    
    /**
     * @return
     *      a set of all scopes that have been removed
     */
    Set<S> getRemovedScopes();
    
    //---------------------------------------------------------------------------------------------
    
    Multimap<S, NameAndRelation> getChangedNamesPS();
    Multimap<S, NameAndRelation> getRemovedNamesPS();
    Multimap<S, NameAndRelation> getAddedNamesPS();
    
    //---------------------------------------------------------------------------------------------
    
    /**
     * @return
     *      all the edges that were added
     */
    IRelation3<S, L, S> getAddedEdges();
    /**
     * @return
     *      all the edges that were removed
     */
    IRelation3<S, L, S> getRemovedEdges();
    
    //---------------------------------------------------------------------------------------------
    
    /**
     * @return
     *      all the names that have been added (new sort with relation)
     */
    IRelation3<S, L, Name> getAddedData();
    
    /**
     * @return
     *      all the names that have been removed (all relations for sort removed)
     */
    IRelation3<S, L, Name> getRemovedData();
    
    /**
     * @return
     *      all the names for which an associated relation has changed (!decl)
     */
    IRelation3<S, L, Name> getChangedData();
    
    //---------------------------------------------------------------------------------------------
    
    /**
     * @return
     *      a set of all names that are affected by changes
     */
    default Set<Name> getAffectedNames() {
        return Sets.union(getAddedData().valueSet(), Sets.union(getRemovedData().valueSet(), getChangedData().valueSet()));
    }
    
    //---------------------------------------------------------------------------------------------
    /**
     * @return
     *      the diffs of each of the children
     */
    Map<String, IScopeGraphDiff<S, L, D>> childDiffs();
}
