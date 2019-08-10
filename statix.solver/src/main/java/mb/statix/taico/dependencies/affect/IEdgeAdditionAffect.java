package mb.statix.taico.dependencies.affect;

import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.taico.dependencies.Dependency;

public interface IEdgeAdditionAffect {
    /**
     * @param scope
     *      the (source) scope of the edge
     * @param label
     *      the label of the edge
     * 
     * @return
     *      the dependencies that can be affected by the addition of the given edge
     */
    public Iterable<Dependency> affectedByEdgeAddition(Scope scope, ITerm label);
    
    /**
     * @return
     *      the score (lower is better) for how well this predicts the impact of edge addition
     */
    public int edgeAdditionAffectScore();
}
