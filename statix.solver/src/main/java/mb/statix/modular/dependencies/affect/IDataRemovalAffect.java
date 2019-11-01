package mb.statix.modular.dependencies.affect;

import java.util.Comparator;

import com.google.common.collect.Iterables;

import mb.nabl2.terms.ITerm;
import mb.statix.modular.dependencies.Dependency;
import mb.statix.scopegraph.terms.Scope;

public interface IDataRemovalAffect {
    public static final Comparator<IDataRemovalAffect> COMPARATOR = (a, b) -> -Integer.compare(a.dataRemovalAffectScore(), b.dataRemovalAffectScore());
    
    /**
     * @param scope
     *      the (source) scope of the data
     * @param relation
     *      the relation of the data
     * 
     * @return
     *      the dependencies that can be affected by the addition of the given data
     */
    public Iterable<Dependency> affectedByDataRemoval(Scope scope, ITerm relation);
    
    /**
     * @return
     *      the score (lower is better) for how well this predicts the impact of data removal
     */
    public int dataRemovalAffectScore();
    
    /**
     * @param other
     *      the other {@link IDataRemovalAffect} to be composed with
     * 
     * @return
     *      a new {@link IDataRemovalAffect} composed of this affect and the given one 
     */
    default IDataRemovalAffect compose(IDataRemovalAffect other) {
        return new IDataRemovalAffect() {
            
            @Override
            public int dataRemovalAffectScore() {
                return IDataRemovalAffect.this.dataRemovalAffectScore();
            }
            
            @Override
            public Iterable<Dependency> affectedByDataRemoval(Scope scope, ITerm relation) {
                return Iterables.concat(
                        IDataRemovalAffect.this.affectedByDataRemoval(scope, relation),
                        other.affectedByDataRemoval(scope, relation));
            }
        };
    }
}
