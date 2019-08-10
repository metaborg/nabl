package mb.statix.modular.dependencies.affect;

import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.terms.Scope;

public interface IAffected {

    /**
     * Determines if this dependency can be affected by the removal of the given scope.
     * 
     * @param scope
     *      the scope to check
     * 
     * @return
     *      true if this dependency can be affected by the removal, false otherwise
     */
    public boolean canBeAffectedByScopeRemoval(Scope scope);
    
    /**
     * Determines if this dependency is affected by the removal of the given scope.
     * 
     * @param scope
     *      the scope to check
     * 
     * @return
     *      true if this dependency is affected by the removal, false otherwise
     */
    public boolean isAffectedByScopeRemoval(Scope scope);
    
    /**
     * @return
     *      if true, this dependency is ONLY affected by scope removal if the
     *      {@link #isAffectedByScopeRemoval(Scope)} method returns true. Otherwise, this
     *      dependency can only determine if it COULD be affected with
     *      {@link #canBeAffectedByScopeRemoval(Scope)}
     */
    public boolean scopeRemovalPrecise();
    
    /**
     * Determines if this dependency can be affected by the addition of the given edge.
     * 
     * @param scope
     *      the scope of the edge (source)
     * @param label
     *      the label of the edge
     * 
     * @return
     *      true if this dependency can be affected by the addition of the edge, false otherwise
     */
    public boolean canBeAffectedByEdgeAddition(Scope scope, ITerm label);
    
    /**
     * Determines if this query can be affected by the removal of the given edge.
     * 
     * @param scope
     *      the (source) scope of the edge
     * @param label
     *      the label of the edge
     * 
     * @return
     *      true if this dependency can be affected by the removal of the edge, false otherwise
     */
    public boolean canBeAffectedByEdgeRemoval(Scope scope, ITerm label);
    
    /**
     * Determines if this dependency can be affected by the addition of the given data edge.
     * 
     * @param scope
     *      the scope of the edge (source)
     * @param label
     *      the label of the edge
     * 
     * @return
     *      true if this dependency can be affected by the addition of the edge, false otherwise
     */
    public boolean canBeAffectedByDataAddition(Scope scope, ITerm relation);
    
    /**
     * Determines if this query can be affected by the removal of the given data edge.
     * 
     * @param scope
     *      the (source) scope of the data edge
     * @param relation
     *      the relation of the data
     * 
     * @return
     *      true if this dependency can be affected by the removal of the data edge, false
     *      otherwise
     */
    public boolean canBeAffectedByDataRemoval(Scope scope, ITerm relation);
}
