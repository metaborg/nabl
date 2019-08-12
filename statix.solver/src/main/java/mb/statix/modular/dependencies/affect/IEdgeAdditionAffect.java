package mb.statix.modular.dependencies.affect;

import java.util.Comparator;

import com.google.common.collect.Iterables;

import mb.nabl2.terms.ITerm;
import mb.statix.modular.dependencies.Dependency;
import mb.statix.scopegraph.terms.Scope;

public interface IEdgeAdditionAffect {
    public static final Comparator<IEdgeAdditionAffect> COMPARATOR = (a, b) -> -Integer.compare(a.edgeAdditionAffectScore(), b.edgeAdditionAffectScore());
    
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
    
    /**
     * @param other
     *      the other edge addition affect to be composed with
     * 
     * @return
     *      a new {@link IEdgeAdditionAffect} composed of this affect and the given one 
     */
    default IEdgeAdditionAffect compose(IEdgeAdditionAffect other) {
        return new IEdgeAdditionAffect() {
            
            @Override
            public int edgeAdditionAffectScore() {
                return IEdgeAdditionAffect.this.edgeAdditionAffectScore();
            }
            
            @Override
            public Iterable<Dependency> affectedByEdgeAddition(Scope scope, ITerm label) {
                return Iterables.concat(
                        IEdgeAdditionAffect.this.affectedByEdgeAddition(scope, label),
                        other.affectedByEdgeAddition(scope, label));
            }
        };
    }
}
